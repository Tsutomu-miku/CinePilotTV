package tv.cinepilot.plugins.bangumi;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import tv.cinepilot.plugin.spi.AuthVerificationResult;
import tv.cinepilot.plugin.spi.CinePilotPlugin;
import tv.cinepilot.plugin.spi.ItemSyncStatus;
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
 *
 * <p>Per-item state tracked in the plugin store (keyed by Jellyfin
 * serverId:itemId):
 *   - {@code s:<key>}, {@code s:<key>:t}       cached bangumi subject id + timestamp
 *   - {@code e:<key>}, {@code e:<key>:t}       cached bangumi episode id + timestamp
 *   - {@code w:<key>}, {@code w:<key>:t}       "successfully marked watched" flag
 *   - {@code f:<key>}                          last error text if this item last failed
 *   - {@code p:<key>}                          persistent provider id mapping for subject
 *   - {@code q:<key>}                          persistent provider id mapping for episode
 */
public final class BangumiPlugin implements UserDataSyncPlugin, PlaybackSyncPlugin {
    private static final class Keys {
        static final String TOKEN = "token";
        static final String LAST_ERROR = "lastError";
        static final String SUBJECT_PREFIX = "s:";
        static final String EPISODE_PREFIX = "e:";
        static final String TIMESTAMP_SUFFIX = ":t";
        static final String WATCHED_PREFIX = "w:";
        /**
         * Success stamp written after `setSubjectCollection(subjectId, DONE)
         * succeeds for a non-episode item (series, movie). Mirrors
         * WATCHED_PREFIX for the episode path.
         */
        static final String USER_DONE_PREFIX = "u:";
        static final String FAILED_PREFIX = "f:";
        static final String PROVIDER_SUBJECT_PREFIX = "p:";
        static final String PROVIDER_EPISODE_PREFIX = "q:";
    }

    private static final long CACHE_DURATION_MS = TimeUnit.DAYS.toMillis(30);
    /** Scrobble a progress report only every 60s wall-clock seconds. */
    private static final long MIN_SCRUB_INTERVAL_MS = 60_000L;
    /** Consider an episode "watched" when playback crosses 90% of its run time. */
    private static final float WATCHED_THRESHOLD = 0.9f;
    /** Minimum run time (ms) for a progress event to be worth recording. */
    private static final long MIN_RUN_TIME_MS = 90_000L;
    private static final int SEARCH_SUBJECT_LIMIT = 5;
    private static final int LIST_EPISODES_LIMIT = 200;

    /** Key under which the current auth generation is stored in the plugin settings store. */
    private static final String AUTH_GEN_KEY = "auth_generation";

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
        // Per-item errors live under Keys.FAILED_PREFIX + cacheKey and are
        // surfaced by the details-screen sync chip. They must not flip the
        // whole plugin into TEMPORARILY_UNAVAILABLE.
        return PluginStatus.READY;
    }

    // --- Auth verification ------------------------------------------------

    @Override public AuthVerificationResult verifyAuth(PluginSettingsStore store, String credential) {
        if (credential == null || credential.isBlank()) {
            return AuthVerificationResult.failure("请输入个人访问令牌（PAT）");
        }
        try {
            String raw = new BangumiApi(credential).me();
            String nickname = BangumiJson.parseUserNickname(raw);
            if (nickname.isBlank()) nickname = "已验证用户";
            return AuthVerificationResult.success(nickname);
        } catch (IOException | RuntimeException exception) {
            String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return AuthVerificationResult.failure(message);
        }
    }

    @Override public void saveAuth(PluginSettingsStore store, String credential) {
        store.putString(Keys.TOKEN, credential);
        store.putString(Keys.LAST_ERROR, "");
        // Bump the auth generation so any sync tasks queued under a prior PAT
        // check their generation at execution time and skip the write.
        bumpAuthGeneration(store);
    }

    @Override public void clearAuth(PluginSettingsStore store) {
        // Bump generation first so queued tasks abort their work even before we
        // wipe Keys.TOKEN; this prevents a race where a running task reads
        // TOKEN before the remove() below lands but after the outer cleanup.
        bumpAuthGeneration(store);
        store.remove(Keys.TOKEN);
        store.remove(Keys.LAST_ERROR);
        // Wipe all per-item sync state when the user rotates credentials:
        // all success stamps, failure markers, subject/episode caches, and
        // provider id mappings are account-scoped on Bangumi's side. Keeping
        // them would leak stale "已同步 / 同步失败" badges after login change.
        store.removeByPrefix(Keys.SUBJECT_PREFIX);
        store.removeByPrefix(Keys.EPISODE_PREFIX);
        store.removeByPrefix(Keys.WATCHED_PREFIX);
        store.removeByPrefix(Keys.USER_DONE_PREFIX);
        store.removeByPrefix(Keys.FAILED_PREFIX);
        store.removeByPrefix(Keys.PROVIDER_SUBJECT_PREFIX);
        store.removeByPrefix(Keys.PROVIDER_EPISODE_PREFIX);
        // The timestamp keys share the same prefix root as their payloads
        // ($prefix$key + ":t"), so removing-by-payload-prefix handles both.
    }

    @Override public void onUnloaded() {
        worker.shutdownNow();
    }

    // --- UserDataSyncPlugin -------------------------------------------

    @Override public void onRatingChanged(
            PluginSettingsStore store, MediaItemSnapshot item, double ratingZeroToTen) {
        if (item.kind() == MediaItemSnapshot.Kind.EPISODE) return;
        final long gen = currentAuthGeneration(store);
        final int rating = (int) Math.round(ratingZeroToTen);
        submit(store, () -> {
            if (!authGenerationMatches(store, gen)) return;
            long subjectId = resolveSubjectId(store, item, /*persistProviderId=*/ true);
            if (subjectId <= 0) return;
            api(store).setSubjectRating(subjectId, rating);
        });
    }

    @Override public void onFavoriteChanged(
            PluginSettingsStore store, MediaItemSnapshot item, boolean isFavorite) {
        if (item.kind() == MediaItemSnapshot.Kind.EPISODE) return;
        if (!isFavorite) {
            // Jellyfin "favorite" ≠ Bangumi wish-list 1:1. Skip the inverse
            // because Bangumi has no atomic "remove from collection" API and
            // setting DONE here would corrupt upstream state for anime the
            // user has not actually watched. A full two-way sync lives in P1.
            return;
        }
        final long gen = currentAuthGeneration(store);
        submit(store, () -> {
            if (!authGenerationMatches(store, gen)) return;
            long subjectId = resolveSubjectId(store, item, /*persistProviderId=*/ true);
            if (subjectId <= 0) return;
            api(store).setSubjectCollection(subjectId, BangumiApi.SubjectCollectionType.WISH);
        });
    }

    @Override public void onWatchedChanged(
            PluginSettingsStore store, MediaItemSnapshot item, boolean isWatched) {
        if (!isWatched) {
            // Bangumi does not expose an atomic "un-mark as watched" verb, and
            // deleting a collection entry can clear user ratings as a side
            // effect. Skip the un-mark path until the plugin supports a safe
            // inverse operation (P1 roadmap).
            return;
        }
        String key = BangumiSubjectMatcher.cacheKey(item);
        final long gen = currentAuthGeneration(store);
        submit(store, () -> {
            if (!authGenerationMatches(store, gen)) return;
            if (item.kind() == MediaItemSnapshot.Kind.EPISODE) {
                if (watchedMarked.add(key)) markEpisodeWatched(store, item);
            } else {
                try {
                    long subjectId = resolveSubjectId(store, item, /*persistProviderId=*/ true);
                    if (subjectId <= 0) {
                        store.putString(Keys.FAILED_PREFIX + key,
                                "未找到匹配的 Bangumi 条目");
                        return;
                    }
                    api(store).setSubjectCollection(subjectId,
                            BangumiApi.SubjectCollectionType.DONE);
                    store.putLong(Keys.USER_DONE_PREFIX + key, System.currentTimeMillis());
                    store.remove(Keys.FAILED_PREFIX + key);
                } catch (IOException | RuntimeException ex) {
                    String message = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    store.putString(Keys.FAILED_PREFIX + key, message);
                    throw ex;
                }
            }
        });
    }

    // --- Per-item status (details badge) ---------------------------------

    @Override public ItemSyncStatus itemSyncStatus(PluginSettingsStore store, MediaItemSnapshot item) {
        // Only anime-ish kinds (EPISODE, SERIES, SEASON, MOVIE) are interesting.
        switch (item.kind()) {
            case EPISODE:
            case SERIES:
            case SEASON:
            case MOVIE:
                break;
            default:
                return ItemSyncStatus.UNSUPPORTED;
        }
        String token = store.getString(Keys.TOKEN, "");
        if (token.isBlank()) return ItemSyncStatus.AUTH_REQUIRED;
        String key = BangumiSubjectMatcher.cacheKey(item);
        String failed = store.getString(Keys.FAILED_PREFIX + key, "");
        if (!failed.isBlank()) return ItemSyncStatus.FAILED;
        // Only SYNCED on an explicit post-write success stamp.
        // WATCHED_PREFIX = episode mark-watched completed upstream.
        // USER_DONE_PREFIX = non-episode (series/season/movie) collection=DONE completed upstream.
        long episodeStamp = store.getLong(Keys.WATCHED_PREFIX + key, 0L);
        if (episodeStamp > 0L) return ItemSyncStatus.SYNCED;
        long doneStamp = store.getLong(Keys.USER_DONE_PREFIX + key, 0L);
        if (doneStamp > 0L) return ItemSyncStatus.SYNCED;
        return ItemSyncStatus.UNMATCHED;
    }

    @Override public void retrySyncItem(PluginSettingsStore store, MediaItemSnapshot item) {
        String key = BangumiSubjectMatcher.cacheKey(item);
        // Clear the failed marker so the UI briefly shows pending state and
        // the new attempt's result can overwrite it cleanly.
        store.remove(Keys.FAILED_PREFIX + key);
        if (item.kind() == MediaItemSnapshot.Kind.EPISODE) {
            runCatchingIOException(() -> markEpisodeWatched(store, item));
        } else {
            runCatchingIOException(() -> {
                long subjectId = resolveSubjectId(store, item, /*persistProviderId=*/ true);
                if (subjectId > 0) {
                    api(store).setSubjectCollection(
                            subjectId, BangumiApi.SubjectCollectionType.DONE);
                    store.putLong(Keys.USER_DONE_PREFIX + key, System.currentTimeMillis());
                }
            });
        }
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
        final long gen = currentAuthGeneration(store);
        submit(store, () -> {
            if (!authGenerationMatches(store, gen)) return;
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
        final long gen = currentAuthGeneration(store);
        submit(store, () -> {
            if (!authGenerationMatches(store, gen)) return;
            if (watchedMarked.add(key)) markEpisodeWatched(store, item);
        });
    }

    // --- Internal -----------------------------------------------------

    private long resolveSubjectId(
            PluginSettingsStore store,
            MediaItemSnapshot item,
            boolean persistProviderId) throws IOException {
        long explicit = BangumiSubjectMatcher.explicitSubjectId(item);
        if (explicit > 0) return explicit;
        // Persistent provider-id mapping written by a prior successful match.
        // Consult this BEFORE the 30-day volatile (s:) cache so the user's
        // manual override or a past fuzzy match survives cache expiry without
        // ever re-running search (and potentially mis-matching a re-titled item).
        String providerKey = Keys.PROVIDER_SUBJECT_PREFIX + BangumiSubjectMatcher.cacheKey(item);
        String providerIdStr = store.getString(providerKey, "");
        if (!providerIdStr.isBlank()) {
            try {
                long id = Long.parseLong(providerIdStr);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) {
                // fall through to the cache/search path and, on success, the
                // caller will overwrite the malformed value via persistProviderId.
            }
        }
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
                keyword, BangumiSubjectMatcher.subjectType(item), SEARCH_SUBJECT_LIMIT);
        List<BangumiJson.SubjectMatch> matches = BangumiJson.parseSearch(raw);
        if (matches.isEmpty()) return 0L;
        long id = matches.get(0).id;
        store.putLong(key, id);
        store.putLong(timeKey, System.currentTimeMillis());
        if (persistProviderId) {
            store.putString(providerKey, Long.toString(id));
        }
        return id;
    }

    private long resolveEpisodeId(
            PluginSettingsStore store, MediaItemSnapshot episode, long subjectId) throws IOException {
        // Persistent per-item provider-id mapping for episodes. Survives the
        // 30-day volatile (e:) cache the same way PROVIDER_SUBJECT_PREFIX does
        // for subjects.
        String providerKey = Keys.PROVIDER_EPISODE_PREFIX + BangumiSubjectMatcher.cacheKey(episode);
        String providerIdStr = store.getString(providerKey, "");
        if (!providerIdStr.isBlank()) {
            try {
                long id = Long.parseLong(providerIdStr);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) {
                // fall through to list-and-match, which will overwrite the key.
            }
        }
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
                api(store).listEpisodes(subjectId, LIST_EPISODES_LIMIT));
        BangumiJson.EpisodeMatch match = null;
        for (BangumiJson.EpisodeMatch ep : episodes) {
            if (Math.round(ep.sort) == episodeNo) { match = ep; break; }
        }
        if (match == null) return 0L;
        store.putLong(eKey, match.id);
        store.putLong(tKey, System.currentTimeMillis());
        store.putString(providerKey, Long.toString(match.id));
        return match.id;
    }

    private void markEpisodeWatched(PluginSettingsStore store, MediaItemSnapshot item) throws IOException {
        String key = BangumiSubjectMatcher.cacheKey(item);
        try {
            long subjectId = resolveSubjectId(store, item, /*persistProviderId=*/ true);
            if (subjectId <= 0) {
                store.putString(Keys.FAILED_PREFIX + key, "未找到匹配的 Bangumi 条目");
                return;
            }
            long episodeId = resolveEpisodeId(store, item, subjectId);
            if (episodeId <= 0) {
                store.putString(Keys.FAILED_PREFIX + key, "未找到匹配的 Bangumi 分集");
                return;
            }
            api(store).setEpisodeCollection(episodeId, BangumiApi.EpisodeCollectionType.WATCHED);
            store.putLong(Keys.WATCHED_PREFIX + key, System.currentTimeMillis());
            store.remove(Keys.FAILED_PREFIX + key);
        } catch (IOException | RuntimeException ex) {
            String message = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            store.putString(Keys.FAILED_PREFIX + key, message);
            throw ex;
        }
    }

    private BangumiApi api(PluginSettingsStore store) {
        String token = store.getString(Keys.TOKEN, "");
        if (token.isBlank()) throw new IllegalStateException("bangumi access token not set");
        return new BangumiApi(token);
    }

    /**
     * Read the current auth generation. The generation is bumped on any
     * credential rotation (saveAuth / clearAuth) so queued tasks that
     * were submitted under a prior account can detect the shift.
     */
    private static long currentAuthGeneration(PluginSettingsStore store) {
        return store.getLong(AUTH_GEN_KEY, 0L);
    }

    /**
     * Bump the auth generation by one. Invoked from any credential-change
     * path; writes are serialized via the SharedPreferences editor inside a
     * single store.putLong call.
     */
    private static void bumpAuthGeneration(PluginSettingsStore store) {
        store.putLong(AUTH_GEN_KEY, currentAuthGeneration(store) + 1L);
    }

    /**
     * Generation check at the start of every queued sync task. Returns true
     * only if the current stored generation still matches the value snapped
     * when the task was submitted. A mismatch means the user rotated or
     * removed the PAT while this task was pending; abort to avoid cross-
     * account writes.
     */
    private static boolean authGenerationMatches(PluginSettingsStore store, long capturedAtSubmit) {
        return currentAuthGeneration(store) == capturedAtSubmit;
    }

    private void submit(PluginSettingsStore store, ThrowingRunnable work) {
        worker.submit(() -> {
            try {
                work.run();
            } catch (IOException | RuntimeException ignored) {
                // Per-item failure diagnostics are written into the item-scoped
                // Keys.FAILED_PREFIX entry by the specific work block
                // (markEpisodeWatched etc.), and surfaced through itemSyncStatus.
            }
        });
    }

    /**
     * Variant of {@link #submit} that runs work inline (never wraps in a
     * worker task) and swallows exceptions. Used by {@link #retrySyncItem}
     * which is already invoked on the plugin host's worker executor.
     *
     * <p>Note: {@link #markEpisodeWatched} already writes per-item failures
     * into the scoped {@code Keys.FAILED_PREFIX + cacheKey} entry, so even
     * silent failures remain visible to the user via the details sync chip.
     */
    private void runCatchingIOException(ThrowingRunnable work) {
        try {
            work.run();
        } catch (IOException ignored) {
            // retrySyncItem callers are already wrapped in try/catch by the
            // plugin host; any persistent error shows up via itemSyncStatus.
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws IOException;
    }
}
