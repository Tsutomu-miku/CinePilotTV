package tv.cinepilot.plugins.chinese;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

/**
 * Extracts .zip / .7z archives into flat lists of subtitle files. RAR
 * support is attempted via reflection against
 * {@code ArchiveStreamFactory("rar")} when present; otherwise the raw
 * bytes are preserved and treated as a single "subtitle" candidate (in
 * case the uploader uploaded a plain SRT with a .rar-like magic by
 * accident).
 *
 * <p>Filenames that aren't valid UTF-8 (very common on Chinese-language
 * zip uploads from Windows) are decoded with GB18030 as fallback — the
 * same heuristic ChineseSubFinder applies.
 */
final class ArchiveExtractor {

    /** A single extracted subtitle file (name + raw bytes). */
    static final class Entry {
        final String fileName;
        final byte[] bytes;
        Entry(String fileName, byte[] bytes) {
            this.fileName = fileName;
            this.bytes = bytes;
        }
    }

    private static final int MAX_ENTRY_BYTES = 10 * 1024 * 1024;
    private static final int MAX_TOTAL_BYTES = 64 * 1024 * 1024;
    private static final int MAX_DEPTH = 3;

    static List<Entry> extractSubtitles(byte[] bytes) throws IOException {
        List<Entry> out = new ArrayList<>();
        extractRecursive(bytes, MAX_DEPTH, out, new int[] { 0 });
        return out;
    }

    // --- dispatcher -----------------------------------------------------

    private static void extractRecursive(byte[] bytes, int depthLeft, List<Entry> out, int[] totalBytes)
            throws IOException {
        if (depthLeft <= 0 || bytes.length < 4 || totalBytes[0] > MAX_TOTAL_BYTES) return;
        boolean handled;
        int head = head(bytes);
        switch (head) {
            case 0x504B0304:  // zip local file header
            case 0x504B0506:  // zip eocd
            case 0x504B0708:  // zip data descriptor
                handled = extractZip(bytes, depthLeft, out, totalBytes);
                break;
            case 0x377ABCAF:  // 7z '7z¼¼'
                handled = extract7z(bytes, depthLeft, out, totalBytes);
                break;
            default:
                // Rar v4: 0x52 0x61 0x72 0x21 0x1A 0x07 0x00
                if (bytes.length >= 7
                        && bytes[0] == 0x52
                        && bytes[1] == 0x61
                        && bytes[2] == 0x72
                        && bytes[3] == 0x21
                        && bytes[4] == 0x1A
                        && bytes[5] == 0x07) {
                    handled = extractRarReflective(bytes, depthLeft, out, totalBytes);
                } else {
                    handled = false;
                }
                break;
        }
        if (!handled) {
            // Not an archive at all — treat the blob as a single candidate.
            keepIfSubtitle(new Entry(guessStandaloneFileName(bytes), bytes), out, totalBytes);
        }
    }

    private static int head(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
    }

    // --- zip ------------------------------------------------------------
    //
    // Streaming ZipArchiveInputStream with charset auto-detection. Avoids
    // the random-access ZipFile class because its constructors differ
    // across commons-compress versions (SeekableByteChannel overloads
    // were added in 1.21 and the order of Charset / encoding arguments
    // has shifted historically). Streaming is slower but perfectly
    // acceptable for <20 MB subtitle archives.

    private static boolean extractZip(byte[] bytes, int depthLeft, List<Entry> out, int[] totalBytes) {
        Charset fallback = detectZipNameCharset(bytes);
        boolean extractedAny = false;
        // Streaming extraction (primary). We try UTF-8 first, fall back to
        // GB18030 if charset detection says so.
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new ByteArrayInputStream(bytes), fallback.name(), true, true)) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                byte[] data = readFully(zis, MAX_ENTRY_BYTES);
                if (looksLikeSubtitle(name)) {
                    keepIfSubtitle(new Entry(baseName(name), data), out, totalBytes);
                } else {
                    // Might be a nested archive.
                    extractRecursive(data, depthLeft - 1, out, totalBytes);
                }
                extractedAny = true;
            }
        } catch (IOException | RuntimeException ignored) {
            // Swallow — corrupt zip happens.
        }
        return extractedAny;
    }

    /**
     * Scan the end-of-central-directory record; if any entry has the
     * "language encoding flag" (0x800) cleared, the original filenames
     * are NOT UTF-8 and we should read them with GB18030.
     */
    private static Charset detectZipNameCharset(byte[] bytes) {
        int probe = Math.min(bytes.length, 65535 + 22);
        int start = Math.max(0, bytes.length - probe);
        for (int i = bytes.length - 22; i >= start; i--) {
            if (bytes[i] == 0x50 && bytes[i + 1] == 0x4B
                    && bytes[i + 2] == 0x05 && bytes[i + 3] == 0x06) {
                int entries = (bytes[i + 10] & 0xFF) | ((bytes[i + 11] & 0xFF) << 8);
                int centralOffset = (bytes[i + 16] & 0xFF)
                        | ((bytes[i + 17] & 0xFF) << 8)
                        | ((bytes[i + 18] & 0xFF) << 16)
                        | ((bytes[i + 19] & 0xFF) << 24);
                int pos = centralOffset;
                int scanned = 0;
                while (scanned < entries && pos + 46 <= bytes.length) {
                    if (bytes[pos] != 0x50 || bytes[pos + 1] != 0x4B
                            || bytes[pos + 2] != 0x01 || bytes[pos + 3] != 0x02) break;
                    int flags = (bytes[pos + 8] & 0xFF) | ((bytes[pos + 9] & 0xFF) << 8);
                    int nameLen = (bytes[pos + 28] & 0xFF) | ((bytes[pos + 29] & 0xFF) << 8);
                    int extraLen = (bytes[pos + 30] & 0xFF) | ((bytes[pos + 31] & 0xFF) << 8);
                    int commentLen = (bytes[pos + 32] & 0xFF) | ((bytes[pos + 33] & 0xFF) << 8);
                    if ((flags & 0x800) == 0) {
                        return Charset.forName("GB18030");
                    }
                    pos = pos + 46 + nameLen + extraLen + commentLen;
                    scanned++;
                }
                break;
            }
        }
        return StandardCharsets.UTF_8;
    }

    // --- 7z -------------------------------------------------------------

    private static boolean extract7z(byte[] bytes, int depthLeft, List<Entry> out, int[] totalBytes) {
        boolean extractedAny = false;
        try (SeekableInMemoryByteChannel ch = new SeekableInMemoryByteChannel(bytes);
             SevenZFile sz = new SevenZFile(ch)) {
            SevenZArchiveEntry entry;
            while ((entry = sz.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName() == null ? "subtitle" : entry.getName();
                // commons-compress >= 1.23 requires passing the entry to
                // getInputStream explicitly.
                byte[] data = readFully(sz.getInputStream(entry), MAX_ENTRY_BYTES);
                if (looksLikeSubtitle(name)) {
                    keepIfSubtitle(new Entry(baseName(name), data), out, totalBytes);
                } else {
                    extractRecursive(data, depthLeft - 1, out, totalBytes);
                }
                extractedAny = true;
            }
        } catch (IOException | RuntimeException ignored) { /* corrupted / unsupported */ }
        return extractedAny;
    }

    // --- rar (reflective attempt, else noop) ---------------------------

    private static boolean extractRarReflective(
            byte[] bytes, int depthLeft, List<Entry> out, int[] totalBytes) {
        try {
            Class<?> streamFactoryCls =
                    Class.forName("org.apache.commons.compress.archivers.ArchiveStreamFactory");
            Object factory = streamFactoryCls
                    .getConstructor(boolean.class, String.class)
                    .newInstance(true, "UTF-8");
            java.lang.reflect.Method create = streamFactoryCls.getMethod(
                    "createArchiveInputStream", String.class, InputStream.class);
            Object ais = create.invoke(factory, "rar", new ByteArrayInputStream(bytes));
            if (ais == null) return false;
            try {
                java.lang.reflect.Method getNext = findMethodByName(ais.getClass(), "getNextEntry");
                if (getNext == null) return false;
                Class<?> entryCls = Class.forName(
                        "org.apache.commons.compress.archivers.ArchiveEntry");
                getNext.setAccessible(true);
                Object entry;
                while ((entry = getNext.invoke(ais)) != null) {
                    java.lang.reflect.Method getName = findMethodByName(entry.getClass(), "getName");
                    java.lang.reflect.Method isDir = findMethodByName(entry.getClass(), "isDirectory");
                    if (isDir != null && Boolean.TRUE.equals(isDir.invoke(entry))) continue;
                    String name = "rar_entry";
                    if (getName != null) {
                        Object n = getName.invoke(entry);
                        if (n instanceof String) name = (String) n;
                    }
                    byte[] data = readFully((InputStream) ais, MAX_ENTRY_BYTES);
                    if (looksLikeSubtitle(name)) {
                        keepIfSubtitle(new Entry(baseName(name), data), out, totalBytes);
                    } else {
                        extractRecursive(data, depthLeft - 1, out, totalBytes);
                    }
                }
            } finally {
                try { ((java.io.Closeable) ais).close(); } catch (RuntimeException ignored) {}
            }
            return true;
        } catch (Throwable ignored) {
            // JunRar not available or archive unreadable. Fall through so
            // the dispatcher treats the raw bytes as a standalone file.
            return false;
        }
    }

    private static java.lang.reflect.Method findMethodByName(Class<?> c, String name) {
        for (java.lang.reflect.Method m : c.getMethods()) {
            if (m.getName().equals(name)) return m;
        }
        for (Class<?> i : c.getInterfaces()) {
            java.lang.reflect.Method m = findMethodByName(i, name);
            if (m != null) return m;
        }
        Class<?> s = c.getSuperclass();
        if (s != null) return findMethodByName(s, name);
        return null;
    }

    // --- helpers --------------------------------------------------------

    private static void keepIfSubtitle(Entry e, List<Entry> out, int[] totalBytes) {
        if (!looksLikeSubtitle(e.fileName)) return;
        if (e.bytes.length < 16) return;
        if (totalBytes[0] + e.bytes.length > MAX_TOTAL_BYTES) return;
        totalBytes[0] += e.bytes.length;
        out.add(e);
    }

    private static boolean looksLikeSubtitle(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = lower.substring(dot + 1);
        switch (ext) {
            case "srt": case "ass": case "ssa": case "vtt":
            case "smi": case "sub": case "idx": case "sup": case "scc":
                return true;
            default:
                return false;
        }
    }

    private static String baseName(String path) {
        if (path == null) return "";
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash < 0 ? path : path.substring(slash + 1);
    }

    /**
     * When a downloaded payload is not a known archive container, we need a
     * filename with a subtitle extension for Media3 to pick the right
     * decoder. Infer the format from a short magic sniff of the content
     * (SRT counters, ASS [Script Info] header, etc.). Falls back to ".srt"
     * (the most common Chinese subtitle format).
     */
    private static String guessStandaloneFileName(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "subtitle.srt";
        // Probe first ~256 bytes (skip leading BOM / whitespace).
        int probe = Math.min(bytes.length, 256);
        String head = new String(bytes, 0, probe, StandardCharsets.UTF_8)
                .replace("\uFEFF", "")
                .replaceAll("^\\s+", "");
        String lowerHead = head.toLowerCase(Locale.ROOT);
        if (lowerHead.startsWith("[script info]")) return "subtitle.ass";
        if (lowerHead.startsWith("webtv\t")) return "subtitle.vtt";
        if (lowerHead.startsWith("webvtt")) return "subtitle.vtt";
        if (head.length() >= 20 && head.substring(0, 20).contains("-->")) return "subtitle.srt";
        if (head.startsWith("1") && head.contains("-->")) return "subtitle.srt";
        // PGS .sup magic: first two bytes 'P' 'G' followed by 0x53 (S) is rare.
        // Most common fallback: SRT.
        return "subtitle.srt";
    }

    private static byte[] readFully(InputStream in, int max) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > max) throw new IOException("archive entry > " + max + " bytes");
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private ArchiveExtractor() {}
}
