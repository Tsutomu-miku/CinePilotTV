package tv.cinepilot.plugins.bangumi;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import tv.cinepilot.plugin.spi.CinePilotPlugin;
import tv.cinepilot.plugin.spi.MediaItemSnapshot;
import tv.cinepilot.plugin.spi.PlaybackSyncPlugin;
import tv.cinepilot.plugin.spi.PluginDescriptor;
import tv.cinepilot.plugin.spi.PluginSettingsStore;
import tv.cinepilot.plugin.spi.PluginStatus;
import tv.cinepilot.plugin.spi.UserDataSyncPlugin;

/**
 * Bangumi.tv user-data + scrobble plugin.
 *
 * <p>Setup flow: the user navigates to 访问
 * <a href="https://next.bangumi.tv/">next.bangumi.tv</a>, creates a personal
 * access token, pastes it into the CinePilot plugin settings screen, and
 * presses "验证". If the token is valid the plugin transitions to
 * {@link PluginStatus#READY}.
 *
 * <p>Sync strategy: every hook is dispatched on a single-threaded executor
 * so the bangumi rate limiter (60 req/min) is respected even when the
 * player fires progress events every few hundred milliseconds. Episode-level
 * progress callbacks are coalesced to the most recent position per item.
 */
public final class BangumiPlugin implements UserDataSyncPlugin, PlaybackSyncPlugin {
    private static final class Keys {
        static final String TOKEN = "token";
        static final String LAST_ERROR = "lastError";
        static final String SUBJECT_PREFIX = "s:";
        static final String EPISODE_PREFIX = "e:";
        static final String TIMESTAMP_SUFFIX = ":t";
    }

    private static final long CACHE_DURATION_MS = TimeUnit.DAYS.toMillis(30);
    /** Scrobble a progress report only every 60s wall-clock seconds. */
    private static final long MIN_SCRUB_INTERVAL_MS = 60_000L;
    /** Consider an episode "watched" when playback crosses 90% of its run time. */
    private static final float WATCHED_THRESHOLD = 0.9f;
    /** Minimum run time (ms) for a progress event to be worth recording. */
    private static final long MIN_RUN_TIME_MS = 90_000L;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bangumi-plugin");
        t.setDaemon(true);
        return t;
    });

    private final java.util.Map<String, AtomicLong> lastProgressAt = new ConcurrentHashMap<>();
    private final Set<String> watchedMarked = ConcurrentHashMap.newKeySet();

    @Override public PluginDescriptor descriptor() {
        return new PluginDescriptor(
                "bangumi-tv",
                "Bangumi.tv 同步",
                "0.1.0",
                "将观看进度、评分、收藏状态同步到 bangumi.tv。"
                        + "用户需要手动粘贴 bangumi 个人访问令牌 (PAT)。",
                "CinePilot Community"
        );
    }

    @Override public void onLoaded(PluginSettingsStore store) {
        // Nothing to initialise eagerly; all resources are constructed on the worker thread.
    }

    @Override public PluginStatus status(PluginSettingsStore store) {
        String token = store.getString(Keys.TOKEN, "");
        if (token.isBlank()) return PluginStatus.AUTH_REQUIRED;
        String err = store.getString(Keys.LAST_ERROR, "");
        if (!err.isBlank()) return PluginStatus.TEMPORARILY_UNAVAILABLE;
        return PluginStatus.READY;
    }

    @Override public void onUnloaded() {
        worker.shutdownNow();
    }

    // --- UserDataSyncPlugin -------------------------------------------

    @Override public void onRatingChanged(
            PluginSettingsStore store, MediaItemSnapshot item, double ratingZeroToTen) {
        if (item.kind() == MediaItemSnapshot.Kind.EPISODE) return;
        int rating = (int) Math.round(ratingZeroToTen);
        submit(store, () -> {
            long subjectId = resolveSubjectId(store, item);
            if (subjectId <= 0) return;
            api(store).setSubjectRating(subjectId, rating);
        });
    }

    @Override public void onFavoriteChanged(
            PluginSettingsStore store, MediaItemSnapshot item, boolean isFavorite) {
        if (item.kind() == MediaItemSnapshot.Kind.EPISODE) return;
        submit(store, () -> {
            long subjectId = resolveSubjectId(store, item);
            if (subjectId <= 0) return;
            int collectionType = isFavorite
                    ? BangumiApi.SubjectCollectionType.WISH
                    : BangumiApi.SubjectCollectionType.DONE;
            api(store).setSubjectCollection(subjectId, collectionType);
        });
    }

    @Override public void onWatchedChanged(
            PluginSettingsStore store, MediaItemSnapshot item, boolean isWatched) {
        if (!isWatched) return;
        String key = BangumiSubjectMatcher.cacheKey(item);
        submit(store, () -> {
            if (item.kind() == MediaItemSnapshot.Kind.EPISODE) {
                if (watchedMarked.add(key)) markEpisodeWatched(store, item);
            } else {
                long subjectId = resolveSubjectId(store, item);
                if (subjectId > 0) {
                    api(store).setSubjectCollection(subjectId, BangumiApi.SubjectCollectionType.DONE);
                }
            }
        });
    }

    // --- PlaybackSyncPlugin -------------------------------------------

    @Override public void onPlaybackStarted(
            PluginSettingsStore store, MediaItemSnapshot item, long durationMs) {
        String key = BangumiSubjectMatcher.cacheKey(item);
        watchedMarked.remove(key);
        lastProgressAt.remove(key);
    }

    @Override public void onPlaybackProgress(
            PluginSettingsStore store,
            MediaItemSnapshot item,
            long positionMs,
            long durationMs) {
        if (durationMs < MIN_RUN_TIME_MS) return;
        if (item.kind() != MediaItemSnapshot.Kind.EPISODE) return;
        String key = BangumiSubjectMatcher.cacheKey(item);
        long now = System.currentTimeMillis();
        AtomicLong last = lastProgressAt.computeIfAbsent(key, k -> new AtomicLong(0L));
        long prev = last.get();
        if (now - prev < MIN_SCRUB_INTERVAL_MS) return;
        if (!last.compareAndSet(prev, now)) return;
        float ratio = durationMs <= 0 ? 0f : (float) positionMs / durationMs;
        if (ratio < WATCHED_THRESHOLD) return;
        submit(store, () -> {
            if (watchedMarked.add(key)) markEpisodeWatched(store, item);
        });
    }

    @Override public void onPlaybackStopped(
            PluginSettingsStore store,
            MediaItemSnapshot item,
            long finalPositionMs,
            long durationMs) {
        if (durationMs < MIN_RUN_TIME_MS) return;
        if (item.kind() != MediaItemSnapshot.Kind.EPISODE) return;
        String key = BangumiSubjectMatcher.cacheKey(item);
        lastProgressAt.remove(key);
        float ratio = durationMs <= 0 ? 0f : (float) finalPositionMs / durationMs;
        if (ratio < WATCHED_THRESHOLD) return;
        submit(store, () -> {
            if (watchedMarked.add(key)) markEpisodeWatched(store, item);
        });
    }

    // --- Internal -----------------------------------------------------

    private long resolveSubjectId(PluginSettingsStore store, MediaItemSnapshot item) throws IOException {
        long explicit = BangumiSubjectMatcher.explicitSubjectId(item);
        if (explicit > 0) return explicit;
        String key = Keys.SUBJECT_PREFIX + BangumiSubjectMatcher.cacheKey(item);
        String timeKey = key + Keys.TIMESTAMP_SUFFIX;
        long cached = store.getLong(key, 0L);
        if (cached > 0) {
            long stamp = store.getLong(timeKey, 0L);
            if (System.currentTimeMillis() - stamp < CACHE_DURATION_MS) return cached;
        }
        String keyword = BangumiSubjectMatcher.searchKeyword(item);
        if (keyword.isBlank()) return 0L;
        String raw = api(store).searchSubject(
                keyword, BangumiSubjectMatcher.subjectType(item), 5);
        List<BangumiJson.SubjectMatch> matches = BangumiJson.parseSearch(raw);
        if (matches.isEmpty()) return 0L;
        long id = matches.get(0).id;
        store.putLong(key, id);
        store.putLong(timeKey, System.currentTimeMillis());
        return id;
    }

    private long resolveEpisodeId(
            PluginSettingsStore store, MediaItemSnapshot episode, long subjectId) throws IOException {
        String eKey = Keys.EPISODE_PREFIX + BangumiSubjectMatcher.cacheKey(episode);
        String tKey = eKey + Keys.TIMESTAMP_SUFFIX;
        long cached = store.getLong(eKey, 0L);
        if (cached > 0) {
            long stamp = store.getLong(tKey, 0L);
            if (System.currentTimeMillis() - stamp < CACHE_DURATION_MS) return cached;
        }
        int episodeNo = BangumiSubjectMatcher.episodeNumber(episode);
        if (episodeNo <= 0 || subjectId <= 0) return 0L;
        List<BangumiJson.EpisodeMatch> episodes = BangumiJson.parseEpisodes(
                api(store).listEpisodes(subjectId, 200));
        BangumiJson.EpisodeMatch match = null;
        for (BangumiJson.EpisodeMatch ep : episodes) {
            if (Math.round(ep.sort) == episodeNo) { match = ep; break; }
        }
        if (match == null) return 0L;
        store.putLong(eKey, match.id);
        store.putLong(tKey, System.currentTimeMillis());
        return match.id;
    }

    private void markEpisodeWatched(PluginSettingsStore store, MediaItemSnapshot item) throws IOException {
        long subjectId = resolveSubjectId(store, item);
        if (subjectId <= 0) return;
        long episodeId = resolveEpisodeId(store, item, subjectId);
        if (episodeId <= 0) return;
        api(store).setEpisodeCollection(episodeId, BangumiApi.EpisodeCollectionType.WATCHED);
    }

    private BangumiApi api(PluginSettingsStore store) {
        String token = store.getString(Keys.TOKEN, "");
        if (token.isBlank()) throw new IllegalStateException("bangumi access token not set");
        return new BangumiApi(token);
    }

    private void submit(PluginSettingsStore store, ThrowingRunnable work) {
        worker.submit(() -> {
            try {
                work.run();
                store.putString(Keys.LAST_ERROR, "");
            } catch (IOException | RuntimeException exception) {
                String message = exception.getClass().getSimpleName()
                        + ": " + exception.getMessage();
                store.putString(Keys.LAST_ERROR, message);
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws IOException;
    }
}
