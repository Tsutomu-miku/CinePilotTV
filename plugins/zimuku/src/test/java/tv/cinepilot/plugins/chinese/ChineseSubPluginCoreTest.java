package tv.cinepilot.plugins.chinese;

import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ChineseSubPluginCoreTest {

    // --- MinJson ----------------------------------------------------------

    @Test public void minJsonParsesObject() {
        Object r = MinJson.parse("{\"a\":1,\"b\":\"hello\",\"c\":true,\"d\":null,\"e\":[1,2,3]}");
        assertTrue(r instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) r;
        assertEquals(1L, m.get("a"));
        assertEquals("hello", m.get("b"));
        assertEquals(Boolean.TRUE, m.get("c"));
        assertNull(m.get("d"));
        assertTrue(m.get("e") instanceof List);
    }

    @Test public void minJsonParsesStringsWithEscapes() {
        Object r = MinJson.parse("{\"s\":\"line1\\nline2\\t\\\"quote\\\"\\u0041\"}");
        @SuppressWarnings("unchecked")
        String s = (String) ((Map<String, Object>) r).get("s");
        assertEquals("line1\nline2\t\"quote\"A", s);
    }

    @Test public void minJsonParsesNumbers() {
        Object r = MinJson.parse("[0,-123,3.14,1e2,-0.5E-2,9999999999]");
        assertTrue(r instanceof List);
        @SuppressWarnings("unchecked")
        List<Object> l = (List<Object>) r;
        assertEquals(0L, l.get(0));
        assertEquals(-123L, l.get(1));
        assertEquals(3.14, (Double) l.get(2), 1e-9);
        assertEquals(100.0, (Double) l.get(3), 1e-9);
        assertEquals(-0.005, (Double) l.get(4), 1e-9);
        assertEquals(9999999999L, l.get(5));
    }

    @Test public void minJsonShooterResponse() {
        String payload = "[{\"Desc\":\"简中\",\"Delay\":0,\"Files\":[{\"Ext\":\"srt\","
                + "\"Link\":\"https://example.com/Arrival.2016.chs.srt\"}]}]";
        Object r = MinJson.parse(payload);
        @SuppressWarnings("unchecked")
        List<Object> arr = (List<Object>) r;
        assertEquals(1, arr.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) arr.get(0);
        assertEquals("简中", row.get("Desc"));
        @SuppressWarnings("unchecked")
        Map<String, Object> f = (Map<String, Object>) ((List<Object>) row.get("Files")).get(0);
        assertEquals("srt", f.get("Ext"));
        assertEquals("https://example.com/Arrival.2016.chs.srt", f.get("Link"));
    }

    // --- LanguageGuess ----------------------------------------------------

    @Test public void languageGuessRecognisesCombinations() {
        assertEquals("简英双语", LanguageGuess.from("Arrival.2016.简英双语.srt", ""));
        assertEquals("繁英双语", LanguageGuess.from("繁英", ""));
        assertEquals("简繁英双语", LanguageGuess.from("", "Arrival.简繁英.ass"));
        assertEquals("简体中文", LanguageGuess.from("chs.gb.简体.srt", ""));
        assertEquals("繁体中文", LanguageGuess.from("", "cht.big5.繁体.srt"));
        assertEquals("英文", LanguageGuess.from("english.eng.英文", ""));
        assertEquals("", LanguageGuess.from("something-unrelated", ""));
        assertEquals("日文", LanguageGuess.from("日语", ""));
        assertEquals("韩文", LanguageGuess.from("", ".kor.韩语.srt"));
    }

    @Test public void languageWeightPrefersBilingualThenSimplified() {
        assertTrue(LanguageGuess.weight("简英双语") > LanguageGuess.weight("简体中文"));
        assertTrue(LanguageGuess.weight("简体中文") > LanguageGuess.weight("繁体中文"));
        assertTrue(LanguageGuess.weight("繁体中文") > LanguageGuess.weight("英文"));
    }

    // --- ZimukuScraper HTML parsing (offline) ---------------------------

    private static final String SAMPLE_SEARCH_ROW = ""
        + "<div id=\"sub_12345\" class=\"persub\" data-id=\"12345\">"
        + "  <div class=\"tt\"><a href=\"/detail/abc123.html\"><b>降临.Arrival.2016.1080p.BluRay.x264-SPARKS</b></a>"
        + "  <span>其他</span></div>"
        + "  <div class=\"subInfo\">"
        + "    <span class=\"lang\">简繁英双语</span>"
        + "    <span class=\"dl-num\">1.2万 次下载</span>"
        + "    <span class=\"rate\">★★★★☆</span>"
        + "    <span class=\"ur\">uploader007</span>"
        + "  </div>"
        + "</div>\n<div class=\"subtitlenav\">...</div>";

    @Test public void zimukuParsesSingleRow() {
        List<ZimukuScraper.Hit> hits = ZimukuScraper.parseSearch(SAMPLE_SEARCH_ROW);
        assertEquals(1, hits.size());
        ZimukuScraper.Hit h = hits.get(0);
        assertEquals("abc123", h.detailId);
        assertTrue(h.name.contains("降临"));
        assertTrue(h.name.contains("Arrival.2016"));
        assertEquals("简繁英双语", h.language);
        assertEquals("★★★★☆", h.rating);
        assertEquals("uploader007", h.author);
        assertEquals(12000, h.downloads);
    }

    @Test public void zimukuDownloadCounts() {
        List<ZimukuScraper.Hit> h1 = ZimukuScraper.parseSearch(withDl("12,345 次下载"));
        assertEquals(12345, h1.get(0).downloads);
        List<ZimukuScraper.Hit> h2 = ZimukuScraper.parseSearch(withDl("5万 次下载"));
        assertEquals(50000, h2.get(0).downloads);
        List<ZimukuScraper.Hit> h3 = ZimukuScraper.parseSearch(withDl("5,678"));
        assertEquals(5678, h3.get(0).downloads);
    }

    private static String withDl(String dl) {
        return "<div class=\"persub\">"
            + "<div class=\"tt\"><a href=\"/detail/9.html\"><b>sample</b></a></div>"
            + "<div class=\"subInfo\">"
            + "  <span class=\"dl-num\">" + dl + "</span>"
            + "</div>"
            + "</div>\n<div class=\"subtitlenav\">x</div>";
    }

    // --- ArchiveExtractor (only zip — no network) -----------------------

    @Test public void extractorKeepsPlainSrt() throws Exception {
        byte[] bytes = "1\n00:00:01,000 --> 00:00:02,000\nhello\n".getBytes();
        List<ArchiveExtractor.Entry> out = ArchiveExtractor.extractSubtitles(bytes);
        assertEquals(1, out.size());
        // Non-archive input gets a synthetic name with an extension so
        // Media3 and our own looksLikeSubtitle filter can recognise it.
        assertEquals("subtitle.srt", out.get(0).fileName);
    }

    @Test public void extractorUnzipsInnerSrt() throws Exception {
        // Build a tiny in-memory zip containing two files:
        //   readme.txt "not a sub"
        //   movie.chs.srt "1\n..."
        byte[] zipBytes = buildZip(new String[] {
            "readme.txt", "movie.chs.srt",
        }, new byte[][] {
            "not a subtitle".getBytes(),
            "1\n00:00:00,000 --> 00:00:05,000\n你好\n".getBytes(),
        });
        List<ArchiveExtractor.Entry> entries = ArchiveExtractor.extractSubtitles(zipBytes);
        assertEquals(1, entries.size());
        assertEquals("movie.chs.srt", entries.get(0).fileName);
    }

    // helper — write a simple stored (deflated = 0 stored) zip manually
    private static byte[] buildZip(String[] names, byte[][] contents) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);
        for (int i = 0; i < names.length; i++) {
            java.util.zip.ZipEntry e = new java.util.zip.ZipEntry(names[i]);
            zos.putNextEntry(e);
            zos.write(contents[i]);
            zos.closeEntry();
        }
        zos.close();
        return baos.toByteArray();
    }
}
