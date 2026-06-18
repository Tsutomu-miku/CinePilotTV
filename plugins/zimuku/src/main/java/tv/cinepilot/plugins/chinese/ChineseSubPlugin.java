package tv.cinepilot.plugins.chinese;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import tv.cinepilot.plugin.spi.CinePilotPlugin;
import tv.cinepilot.plugin.spi.MediaItemSnapshot;
import tv.cinepilot.plugin.spi.PluginDescriptor;
import tv.cinepilot.plugin.spi.PluginSettingsStore;
import tv.cinepilot.plugin.spi.PluginStatus;
import tv.cinepilot.plugin.spi.SubtitleSearchPlugin;
import tv.cinepilot.plugin.spi.SubtitleSearchResult;

/**
 * Aggregating subtitle search plugin for Chinese-speaking users.
 *
 * <p>Bundles three sources into a single plugin (to keep the plugin list
 * tidy and share infrastructure):
 * <ol>
 *   <li><b>Shooter (射手网)</b> — keyword search via official public API.
 *       Highest hit rate among free Chinese providers.</li>
 *   <li><b>Zimuku (字幕库)</b> — HTML scraping of zimuku.org. Includes the
 *       most active uploader community for recent Chinese releases.</li>
 *   <li><b>Xunlei (迅雷字幕)</b> — sandai.net content-id API. Hit rate is
 *       low because CinePilot cannot compute the required file-hash from
 *       a streaming URL, but the call is essentially free and sometimes
 *       returns unique matches.</li>
 * </ol>
 *
 * <p>Search results from all sources are merged, deduplicated by language
 * + name similarity, then ranked by a composite score (Chinese-bilingual
 * preference, download count, source priority).
 *
 * <p>Most downloads from these providers are .zip or .rar archives that
 * contain multiple subtitle files (简体 + 繁体 + 英文, SRT + ASS, etc.).
 * The plugin extracts the archive and picks the single "best" subtitle
 * file inside (preferring bilingual Chinese/English > ASS > SRT). This
 * keeps the contract with the rest of CinePilot, which expects
 * {@link SubtitleSearchPlugin#downloadSubtitle} to return raw subtitle
 * bytes (not a compressed archive).
 */
public final class ChineseSubPlugin implements CinePilotPlugin, SubtitleSearchPlugin {

    private static final class Keys {
        static final String LAST_ERROR = "lastError";
        static final String ZIMUKU_BLOCKED = "zimuku:blocked";
    }

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            "chinese-subs",
            "中文字幕源（射手 / 字幕库 / 迅雷）",
            "0.1.0",
            "从射手网、字幕库 (zimuku.org)、迅雷字幕聚合搜索中文字幕。"
                    + "无需任何授权或 API 密钥即可使用。\n"
                    + "• 射手：官方公开 API，无需配置\n"
                    + "• 字幕库：HTML 抓取，若被反爬屏蔽会自动降级\n"
                    + "• 迅雷：CDN 公开接口，命中率较低",
            "CinePilot Community"
    );

    private final ShooterApi shooter = new ShooterApi();
    private final ZimukuScraper zimuku = new ZimukuScraper();
    private final XunleiApi xunlei = new XunleiApi();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void onLoaded(PluginSettingsStore store) {
        // No eager init needed; clients are stateless.
    }

    @Override
    public void onUnloaded() {
        // No background threads to shut down.
    }

    @Override
    public PluginStatus status(PluginSettingsStore store) {
        String err = store.getString(Keys.LAST_ERROR, "");
        if (err.isBlank()) return PluginStatus.READY;
        // "Last error" only indicates *transient* last-op failure; the
        // provider set itself stays usable.
        return PluginStatus.READY;
    }

    // --- SubtitleSearchPlugin ----------------------------------------------

    @Override
    public List<SubtitleSearchResult> searchSubtitles(
            PluginSettingsStore store,
            MediaItemSnapshot item,
            String languageFilter
    ) {
        String keyword = buildKeyword(item);
        if (keyword.isBlank()) return List.of();

        List<RankedResult> ranked = new ArrayList<>();

        // Source 1: Shooter
        try {
            for (ShooterApi.Match m : shooter.searchByKeyword(keyword)) {
                // Shooter.downloads is a synthetic position-rank, not a real count.
                // Feed the rank into scoring but expose 0 to the UI so misleading
                // "N 次下载" lines are not rendered for shooter rows.
                int score = scoreFor(m.language, m.downloads, 5000);
                ranked.add(new RankedResult(
                        "sh:" + m.id,
                        m.name,
                        m.language,
                        detectFormat(m.name),
                        m.rating,
                        0,
                        m.author,
                        score,
                        Payload.shooter(m.downloadLink)
                ));
            }
        } catch (IOException | RuntimeException ignored) {
            store.putString(Keys.LAST_ERROR, "shooter: " + ignored.toString());
        }

        // Source 2: Zimuku (skipped if previous runs flagged blocking)
        boolean zimukuBlocked = store.getBoolean(Keys.ZIMUKU_BLOCKED, false);
        if (!zimukuBlocked) {
            try {
                for (ZimukuScraper.Hit h : zimuku.search(keyword)) {
                    int score = scoreFor(h.language, h.downloads, 10_000);
                    ranked.add(new RankedResult(
                            "zk:" + h.detailId,
                            h.name,
                            h.language,
                            detectFormat(h.name),
                            h.rating,
                            h.downloads,
                            h.author,
                            score,
                            Payload.zimuku(h.detailId)
                    ));
                }
            } catch (IOException | RuntimeException err) {
                store.putString(Keys.LAST_ERROR, "zimuku: " + err);
                if (looksLikeBlocking(err)) {
                    store.putBoolean(Keys.ZIMUKU_BLOCKED, true);
                }
            }
        }

        // Source 3: Xunlei
        try {
            // Xunlei uses content-id (SHA1 of 3 chunks). Without the file
            // bytes we can't compute it, but any non-empty string still
            // returns keyword-adjacent results occasionally. Use a stable
            // deterministic placeholder derived from the item name.
            String cid = "cinepilot_" + Math.abs(keyword.hashCode());
            for (XunleiApi.Match m : xunlei.query(cid)) {
                int score = scoreFor(m.language, m.downloads, 1000);
                ranked.add(new RankedResult(
                        "xl:" + m.scid,
                        m.name,
                        m.language,
                        detectFormat(m.name),
                        m.rating,
                        m.downloads,
                        m.author,
                        score,
                        Payload.shooter(m.url)   // re-use plain URL payload
                ));
            }
        } catch (IOException | RuntimeException ignored) {
            store.putString(Keys.LAST_ERROR, "xunlei: " + ignored.toString());
        }

        // Rank
        ranked.sort(Comparator
                .comparingInt((RankedResult r) -> r.score).reversed()
                .thenComparingInt(r -> r.downloads).reversed());

        // Language filter (SPI parameter, empty means "all").
        if (languageFilter != null && !languageFilter.isBlank()) {
            String lf = languageFilter.toLowerCase();
            ranked.removeIf(r -> !r.language.toLowerCase().contains(lf)
                    && !r.name.toLowerCase().contains(lf));
        }

        int limit = Math.min(ranked.size(), 30);
        List<SubtitleSearchResult> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            RankedResult r = ranked.get(i);
            // Persist the provider-specific download payload inside the id
            // field. downloadSubtitle() will decode it. The id format is:
            //   "<prefix>:<base64-payload>"
            String packedId = IdCodec.encode(r.id, r.payload);
            out.add(new SubtitleSearchResult(
                    DESCRIPTOR.id(),
                    packedId,
                    r.name,
                    r.language,
                    r.format,
                    r.rating,
                    r.downloads,
                    r.author
            ));
        }
        return out;
    }

    @Override
    public byte[] downloadSubtitle(PluginSettingsStore store, SubtitleSearchResult result) {
        IdCodec.Decoded decoded = IdCodec.decode(result.id());
        if (decoded == null) throw new IllegalStateException("bad subtitle id");
        final byte[] rawBytes;
        if (decoded.payload.zimukuId != null) {
            try {
                rawBytes = zimuku.download(decoded.payload.zimukuId);
            } catch (IOException e) {
                if (looksLikeBlocking(e)) {
                    store.putBoolean(Keys.ZIMUKU_BLOCKED, true);
                }
                throw new RuntimeException(e);
            }
        } else if (decoded.payload.url != null) {
            try {
                rawBytes = HttpOps.httpGet(decoded.payload.url, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException("unknown payload type");
        }
        return pickBestSubtitleBytes(rawBytes);
    }

    // --- Search keyword construction ---------------------------------------

    private static String buildKeyword(MediaItemSnapshot item) {
        StringBuilder sb = new StringBuilder(item.name());
        if (item.productionYear() > 0) {
            sb.append(' ').append(item.productionYear());
        }
        // If the item is an episode, append SxxEyy — very effective for TV.
        if (item.kind() == MediaItemSnapshot.Kind.EPISODE) {
            if (item.parentIndexNumber() > 0) {
                sb.append(String.format(" S%02d", item.parentIndexNumber()));
            }
            if (item.indexNumber() > 0) {
                sb.append(String.format("E%02d", item.indexNumber()));
            }
        }
        return sb.toString().trim();
    }

    // --- Ranking -----------------------------------------------------------

    /** Higher is better. */
    private static int scoreFor(String language, int downloads, int sourceBoost) {
        int base = LanguageGuess.weight(language) * 10_000;
        int dl = Math.min(downloads, 100_000);  // cap
        return base + dl + sourceBoost;
    }

    // --- Format detection --------------------------------------------------

    private static SubtitleSearchResult.Format detectFormat(String name) {
        String lower = name == null ? "" : name.toLowerCase();
        if (lower.contains(".ass")) return SubtitleSearchResult.Format.ASS;
        if (lower.contains(".ssa")) return SubtitleSearchResult.Format.SSA;
        if (lower.contains(".vtt") || lower.contains("webvtt")) return SubtitleSearchResult.Format.VTT;
        if (lower.contains(".sup") || lower.contains("pgs")) return SubtitleSearchResult.Format.PGS;
        if (lower.contains(".srt")) return SubtitleSearchResult.Format.SRT;
        return SubtitleSearchResult.Format.UNKNOWN;
    }

    // --- Archive extraction + best-file selection --------------------------

    /**
     * Take downloaded bytes (possibly an archive, possibly a raw subtitle
     * file) and return the bytes of a single subtitle file suitable for
     * Media3.
     *
     * <p>The selection policy prefers bilingual subtitles (简英/繁英), then
     * simplified Chinese, then traditional Chinese, then English. Within a
     * language tier, ASS > SRT > VTT (ASS preserves styling most closely).
     */
    private static byte[] pickBestSubtitleBytes(byte[] payload) {
        List<ArchiveExtractor.Entry> entries;
        try {
            entries = ArchiveExtractor.extractSubtitles(payload);
        } catch (IOException e) {
            throw new RuntimeException("archive extraction failed: " + e.getMessage(), e);
        }
        if (entries.isEmpty()) {
            throw new RuntimeException("download contained no valid subtitle files");
        }
        ArchiveExtractor.Entry best = null;
        int bestScore = Integer.MIN_VALUE;
        for (ArchiveExtractor.Entry e : entries) {
            int langScore = LanguageGuess.weight(e.fileName);
            int formatScore = formatScore(e.fileName);
            int score = langScore * 1000 + formatScore;
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        if (best == null) throw new RuntimeException("no subtitle candidate");
        return best.bytes;
    }

    private static int formatScore(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".ass")) return 6;
        if (lower.endsWith(".ssa")) return 5;
        if (lower.endsWith(".srt")) return 4;
        if (lower.endsWith(".vtt")) return 3;
        if (lower.endsWith(".idx")) return 2;
        if (lower.endsWith(".sub")) return 1;
        if (lower.endsWith(".sup")) return 0;
        return -1;
    }

    // --- Zimuku anti-blocking heuristic ------------------------------------

    private static boolean looksLikeBlocking(Throwable t) {
        String msg = t == null ? "" : (t.getMessage() == null ? "" : t.getMessage());
        String low = msg.toLowerCase();
        // Cloudflare / 503 / "challenge" pages.
        return low.contains("http 503")
                || low.contains("http 403")
                || low.contains("cloudflare")
                || low.contains("captcha")
                || low.contains("ddos")
                || low.contains("checking your browser");
    }

    // --- Internal data structures ------------------------------------------

    private static final class RankedResult {
        final String id;
        final String name;
        final String language;
        final SubtitleSearchResult.Format format;
        final String rating;
        final int downloads;
        final String author;
        final int score;
        final Payload payload;

        RankedResult(String id, String name, String language,
                     SubtitleSearchResult.Format format, String rating,
                     int downloads, String author, int score, Payload payload) {
            this.id = id;
            this.name = name;
            this.language = language;
            this.format = format;
            this.rating = rating;
            this.downloads = downloads;
            this.author = author;
            this.score = score;
            this.payload = payload;
        }
    }

    /**
     * Download payload. We need to carry source-specific download handles
     * (Zimuku detail id or a plain URL) from searchSubtitles() across to
     * downloadSubtitle(). Since SPI forces the communication channel to be
     * `SubtitleSearchResult.id()`, we serialize to a short string.
     */
    private static final class Payload {
        final String url;          // for shooter / xunlei direct links
        final String zimukuId;     // for zimuku detail ids (download via /down/<id>)

        private Payload(String url, String zimukuId) {
            this.url = url;
            this.zimukuId = zimukuId;
        }
        static Payload shooter(String u) { return new Payload(u, null); }
        static Payload zimuku(String id) { return new Payload(null, id); }

        String encode() {
            // "u:<url>" or "z:<id>"
            if (zimukuId != null) return "z:" + zimukuId;
            if (url != null) return "u:" + url;
            return "";
        }
        static Payload decode(String raw) {
            if (raw == null || raw.isEmpty()) return null;
            if (raw.startsWith("z:")) return zimuku(raw.substring(2));
            if (raw.startsWith("u:")) return shooter(raw.substring(2));
            return null;
        }
    }

    /**
     * Encodes/decodes a (search-id, payload) pair into a single string
     * suitable for use as {@code SubtitleSearchResult.id}. The SPI
     * contract for id is "provider-specific opaque identifier", so a
     * concatenated string is well within spec.
     */
    private static final class IdCodec {
        static final class Decoded {
            final String searchId;
            final Payload payload;
            Decoded(String searchId, Payload payload) {
                this.searchId = searchId;
                this.payload = payload;
            }
        }

        static String encode(String searchId, Payload payload) {
            // Use ASCII Unit Separator (0x1F) as delimiter — illegal in
            // URLs and HTML attributes, so no risk of collision with
            // either component.
            return searchId + "\u001f" + payload.encode();
        }

        static Decoded decode(String packed) {
            if (packed == null) return null;
            int sep = packed.indexOf('\u001f');
            if (sep < 0) return null;
            String sid = packed.substring(0, sep);
            String raw = packed.substring(sep + 1);
            Payload p = Payload.decode(raw);
            if (p == null) return null;
            return new Decoded(sid, p);
        }

        private IdCodec() {}
    }
}
