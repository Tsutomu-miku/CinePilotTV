package tv.cinepilot.core.protocol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Queue + quota model for offline playback. Lives in :core so the workflow
 * controller can reason about download state without importing Android APIs.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Enqueue new downloads (server id + item id + optional quality preference).</li>
 *   <li>Track download state ({@link State#QUEUED QUEUED} →
 *       {@link State#DOWNLOADING DOWNLOADING} → {@link State#READY READY} /
 *       {@link State#FAILED FAILED}).</li>
 *   <li>Report total bytes reserved vs. per-server quota so the UI can render
 *       quota-warning UI before enqueueing more downloads.</li>
 *   <li>Provide a list of ready items so the home screen can paint an
 *       "离线下载" rail when the network is unavailable.</li>
 * </ul>
 *
 * <p>The repository is <strong>thread-safe</strong>. It deliberately does not
 * touch disk; file system state is pushed into it by the Android download
 * service that actually owns the Media3 DownloadManager.
 */
public final class OfflineRepository {

    public enum State { QUEUED, DOWNLOADING, READY, FAILED, PAUSED }

    public static final class Entry {
        private final String serverId;
        private final String itemId;
        private final String itemName;
        private final int quality;   // 0=auto, 1=480p, 2=720p, 3=1080p, 4=4k
        private final long mediaSourceIdHash;
        private volatile State state;
        private volatile long bytesDownloaded;
        private volatile long bytesTotal;
        private volatile long localContentId;   // opaque key used by the downloader
        private volatile String errorMessage;

        public Entry(String serverId, String itemId, String itemName, int quality, long mediaSourceIdHash) {
            if (serverId == null || serverId.isBlank()) throw new IllegalArgumentException("serverId");
            if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId");
            this.serverId = serverId;
            this.itemId = itemId;
            this.itemName = itemName == null ? "" : itemName;
            this.quality = quality;
            this.mediaSourceIdHash = mediaSourceIdHash;
            this.state = State.QUEUED;
        }

        public String serverId() { return serverId; }
        public String itemId() { return itemId; }
        public String itemName() { return itemName; }
        public int quality() { return quality; }
        public long mediaSourceIdHash() { return mediaSourceIdHash; }
        public State state() { return state; }
        public long bytesDownloaded() { return bytesDownloaded; }
        public long bytesTotal() { return bytesTotal; }
        public long localContentId() { return localContentId; }
        public String errorMessage() { return errorMessage == null ? "" : errorMessage; }
        public float progressPercent() {
            if (bytesTotal <= 0L) return 0f;
            return Math.min(100f, 100f * bytesDownloaded / bytesTotal);
        }

        public void setState(State state) { this.state = state; }
        public void setBytes(long downloaded, long total) {
            this.bytesDownloaded = Math.max(0L, downloaded);
            this.bytesTotal = Math.max(0L, total);
        }
        public void setLocalContentId(long id) { this.localContentId = id; }
        public void setErrorMessage(String message) { this.errorMessage = message; }
    }

    /** Per-server quota rule. */
    public static final class Quota {
        private final long bytesPerServer;
        private final int itemsPerServer;

        public Quota(long bytesPerServer, int itemsPerServer) {
            this.bytesPerServer = Math.max(0L, bytesPerServer);
            this.itemsPerServer = Math.max(0, itemsPerServer);
        }

        public long bytesPerServer() { return bytesPerServer; }
        public int itemsPerServer() { return itemsPerServer; }
    }

    private final Object lock = new Object();
    private final Map<String, Entry> entries = new LinkedHashMap<>();   // key = serverId:itemId:quality
    private final Deque<String> orderedQueue = new ArrayDeque<>();
    private final Map<String, Quota> quotas = new LinkedHashMap<>();
    private final Quota defaultQuota = new Quota(16L * 1024 * 1024 * 1024, 25);  // 16GB / 25 items

    public void setQuota(String serverId, Quota quota) {
        if (serverId == null || quota == null) return;
        synchronized (lock) {
            quotas.put(serverId, quota);
        }
    }

    public Quota quotaFor(String serverId) {
        synchronized (lock) {
            return quotas.getOrDefault(serverId, defaultQuota);
        }
    }

    /**
     * Attempt to enqueue a download. Returns {@code null} on success, or a
     * human-readable reason (in Chinese, matching the rest of the app UI
     * copy) when the item cannot be enqueued (quota exceeded, duplicate, etc.).
     */
    public String enqueue(Entry entry) {
        if (entry == null) return "下载项无效";
        synchronized (lock) {
            String key = keyOf(entry.serverId(), entry.itemId(), entry.quality());
            Entry existing = entries.get(key);
            if (existing != null && existing.state() != State.FAILED) {
                if (existing.state() == State.READY) return "该项目已在离线库中";
                return "该项目已在下载队列中";
            }
            Quota quota = quotaFor(entry.serverId());
            int count = 0;
            long bytes = 0L;
            for (Entry e : entries.values()) {
                if (!e.serverId().equals(entry.serverId())) continue;
                if (e.state() == State.FAILED) continue;
                count++;
                bytes += Math.max(e.bytesTotal(), estimateBytes(e));
            }
            if (quota.itemsPerServer() > 0 && count >= quota.itemsPerServer()) {
                return "离线数量已达上限 (最多 " + quota.itemsPerServer() + " 项)";
            }
            long est = estimateBytes(entry);
            if (quota.bytesPerServer() > 0 && bytes + est > quota.bytesPerServer()) {
                return "离线容量将超过 " + humanBytes(quota.bytesPerServer()) + " 上限";
            }
            entries.put(key, entry);
            orderedQueue.addLast(key);
            return null;
        }
    }

    public Entry nextForWorker(String serverId) {
        synchronized (lock) {
            Iterator<String> iter = orderedQueue.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                Entry e = entries.get(key);
                if (e == null) { iter.remove(); continue; }
                if (!e.serverId().equals(serverId)) continue;
                if (e.state() == State.QUEUED) {
                    e.setState(State.DOWNLOADING);
                    return e;
                }
            }
            return null;
        }
    }

    public void markReady(String serverId, String itemId, int quality, long localContentId) {
        synchronized (lock) {
            Entry e = entries.get(keyOf(serverId, itemId, quality));
            if (e == null) return;
            e.setState(State.READY);
            e.setLocalContentId(localContentId);
            orderedQueue.remove(keyOf(serverId, itemId, quality));
        }
    }

    public void markFailed(String serverId, String itemId, int quality, String reason) {
        synchronized (lock) {
            Entry e = entries.get(keyOf(serverId, itemId, quality));
            if (e == null) return;
            e.setState(State.FAILED);
            e.setErrorMessage(reason == null ? "未知错误" : reason);
            orderedQueue.remove(keyOf(serverId, itemId, quality));
        }
    }

    public void updateProgress(String serverId, String itemId, int quality, long downloaded, long total) {
        synchronized (lock) {
            Entry e = entries.get(keyOf(serverId, itemId, quality));
            if (e == null) return;
            e.setBytes(downloaded, total);
            if (e.state() == State.QUEUED) e.setState(State.DOWNLOADING);
        }
    }

    public void pause(String serverId, String itemId, int quality) {
        synchronized (lock) {
            Entry e = entries.get(keyOf(serverId, itemId, quality));
            if (e == null) return;
            if (e.state() == State.DOWNLOADING || e.state() == State.QUEUED) e.setState(State.PAUSED);
        }
    }

    public void resume(String serverId, String itemId, int quality) {
        synchronized (lock) {
            Entry e = entries.get(keyOf(serverId, itemId, quality));
            if (e == null) return;
            if (e.state() == State.PAUSED) {
                e.setState(State.QUEUED);
                String key = keyOf(serverId, itemId, quality);
                if (!orderedQueue.contains(key)) orderedQueue.addLast(key);
            }
        }
    }

    public void delete(String serverId, String itemId, int quality) {
        synchronized (lock) {
            String key = keyOf(serverId, itemId, quality);
            entries.remove(key);
            orderedQueue.remove(key);
        }
    }

    public Entry get(String serverId, String itemId, int quality) {
        synchronized (lock) {
            return entries.get(keyOf(serverId, itemId, quality));
        }
    }

    /** Best effort: return the first ready entry that matches the server + item id pair. */
    public Entry readyFor(String serverId, String itemId) {
        synchronized (lock) {
            for (Entry e : entries.values()) {
                if (!e.serverId().equals(serverId)) continue;
                if (!e.itemId().equals(itemId)) continue;
                if (e.state() == State.READY) return e;
            }
            return null;
        }
    }

    public List<Entry> readyForServer(String serverId) {
        List<Entry> out = new ArrayList<>();
        synchronized (lock) {
            for (Entry e : entries.values()) {
                if (serverId != null && !e.serverId().equals(serverId)) continue;
                if (e.state() == State.READY) out.add(e);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public List<Entry> listForServer(String serverId) {
        List<Entry> out = new ArrayList<>();
        synchronized (lock) {
            for (Entry e : entries.values()) {
                if (!e.serverId().equals(serverId)) continue;
                out.add(e);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** Snapshot of every known entry across all servers. Used by the persistence layer. */
    public List<Entry> allEntries() {
        List<Entry> out = new ArrayList<>();
        synchronized (lock) {
            out.addAll(entries.values());
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Inserts an entry without running the duplicate/quota checks. Used only
     * by the persistence layer when reconstructing the repository from disk
     * (entries there are already accepted, and we must not reject them if a
     * server quota changed in between restarts).
     */
    public void enqueueExisting(Entry entry) {
        if (entry == null) return;
        synchronized (lock) {
            String key = keyOf(entry.serverId(), entry.itemId(), entry.quality());
            Entry existing = entries.get(key);
            if (existing != null) {
                entries.remove(key);
                orderedQueue.remove(key);
            }
            entries.put(key, entry);
            if (entry.state() == State.QUEUED || entry.state() == State.DOWNLOADING) {
                orderedQueue.addLast(key);
            }
        }
    }

    public long usedBytesForServer(String serverId) {
        long total = 0L;
        synchronized (lock) {
            for (Entry e : entries.values()) {
                if (!e.serverId().equals(serverId)) continue;
                if (e.state() == State.FAILED) continue;
                total += Math.max(e.bytesTotal(), estimateBytes(e));
            }
        }
        return total;
    }

    // --- internals -----------------------------------------------------------

    private static String keyOf(String serverId, String itemId, int quality) {
        return serverId + ':' + itemId + ':' + quality;
    }

    /** Very rough per-item size estimate used for quota checks before bytesTotal is known. */
    private static long estimateBytes(Entry e) {
        long perMinuteBytes = switch (e.quality()) {
            case 1 -> 150L * 1024 * 1024 / 60;    // 150 MB/hour (480p)
            case 2 -> 800L * 1024 * 1024 / 60;    // 800 MB/hour (720p)
            case 4 -> 4L * 1024 * 1024 * 1024 / 60; // 4GB/hour (4K)
            default -> 1800L * 1024 * 1024 / 60;    // 1.8GB/hour (1080p default)
        };
        return perMinuteBytes * 120;   // assume 2 hour movie worst-case
    }

    private static String humanBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024 * 1024) return String.format("%.1fTB", bytes / (1024e12));
        if (bytes >= 1024L * 1024 * 1024) return String.format("%.1fGB", bytes / (1024e9));
        if (bytes >= 1024L * 1024) return String.format("%.1fMB", bytes / (1024e6));
        if (bytes >= 1024L) return String.format("%.1fKB", bytes / (1024e3));
        return bytes + "B";
    }
}
