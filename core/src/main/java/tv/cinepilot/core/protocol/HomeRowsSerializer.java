package tv.cinepilot.core.protocol;

import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.tv.HomeRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON serializer/deserializer for the list of HomeRows shown on the
 * cold-start home screen.
 *
 * The serialized payload deliberately keeps only the fields needed to render
 * the media wall and preserve focus identity across app restarts:
 *
 *   HomeRow.id / HomeRow.title
 *   MediaItemSummary.id                  (persistent media identity)
 *   MediaItemSummary.name                (cell title overlay)
 *   MediaItemSummary.type                (row cell visual style)
 *   MediaItemSummary.folder / playable   (browse vs. open detail decision)
 *   MediaItemSummary.runTimeTicks        (cell duration overlay)
 *   MediaItemSummary.indexNumber / parentIndexNumber / seriesName / seriesId
 *                                        (S/E badge and next-up context)
 *   MediaItemSummary.imageTags / backdropImageTags
 *                                        (poster, landscape, backdrop URLs)
 *   UserItemData.played / playbackPositionTicks / playCount / favorite
 *                                        (watched badge, resume progress)
 *
 * Full detail payloads are always fetched fresh from the server when a cell
 * is opened; this cache exists purely to let the TV UI paint instantly on
 * launch and to provide a non-blank fallback if the server is unavailable.
 */
public final class HomeRowsSerializer {

    private HomeRowsSerializer() {
    }

    public static String serialize(List<HomeRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(',');
            appendRow(sb, rows.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    public static List<HomeRow> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return AndroidCollections.emptyList();
        }
        List<Object> array = JsonValue.array(json);
        List<HomeRow> out = new ArrayList<>(array.size());
        for (Object entry : array) {
            if (!(entry instanceof Map<?, ?> raw)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> rowObj = (Map<String, Object>) raw;
            String id = JsonValue.string(rowObj, "id");
            String title = JsonValue.string(rowObj, "t");
            if (id == null || id.isBlank()) continue;
            List<MediaItemSummary> items = new ArrayList<>();
            for (Object itemEntry : JsonValue.array(rowObj, "items")) {
                if (!(itemEntry instanceof Map<?, ?> rawItem)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> itemObj = (Map<String, Object>) itemEntry;
                MediaItemSummary item = readItem(itemObj);
                if (item != null) items.add(item);
            }
            out.add(new HomeRow(id, title == null ? "" : title, items));
        }
        return AndroidCollections.listCopy(out);
    }

    // ----------------------------- writer --------------------------------

    private static void appendRow(StringBuilder sb, HomeRow row) {
        sb.append("{\"id\":");
        appendString(sb, row.id());
        sb.append(",\"t\":");
        appendString(sb, row.title());
        sb.append(",\"items\":[");
        List<MediaItemSummary> items = row.items();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            appendItem(sb, items.get(i));
        }
        sb.append("]}");
    }

    private static void appendItem(StringBuilder sb, MediaItemSummary item) {
        sb.append('{');
        boolean wrote = false;
        wrote = writeKey(sb, wrote, "id");
        appendString(sb, item.id());
        if (item.name() != null && !item.name().isEmpty()) {
            wrote = writeKey(sb, wrote, "n");
            appendString(sb, item.name());
        }
        wrote = writeKey(sb, wrote, "type");
        appendString(sb, item.type().name());
        if (item.folder()) {
            wrote = writeKey(sb, wrote, "f");
            sb.append('1');
        }
        if (item.playable()) {
            wrote = writeKey(sb, wrote, "p");
            sb.append('1');
        }
        if (item.runTimeTicks() != null) {
            wrote = writeKey(sb, wrote, "rt");
            sb.append(item.runTimeTicks());
        }
        if (item.productionYear() != null) {
            wrote = writeKey(sb, wrote, "py");
            sb.append(item.productionYear());
        }
        if (item.indexNumber() != null) {
            wrote = writeKey(sb, wrote, "s");
            sb.append(item.indexNumber());
        }
        if (item.parentIndexNumber() != null) {
            wrote = writeKey(sb, wrote, "ss");
            sb.append(item.parentIndexNumber());
        }
        if (item.seriesName() != null && !item.seriesName().isEmpty()) {
            wrote = writeKey(sb, wrote, "sn");
            appendString(sb, item.seriesName());
        }
        if (item.seriesId() != null && !item.seriesId().isEmpty()) {
            wrote = writeKey(sb, wrote, "sid");
            appendString(sb, item.seriesId());
        }
        if (item.premiereDate() != null && !item.premiereDate().isEmpty()) {
            wrote = writeKey(sb, wrote, "pd");
            appendString(sb, item.premiereDate());
        }
        if (item.communityRating() != null) {
            wrote = writeKey(sb, wrote, "cr");
            sb.append(item.communityRating());
        }
        if (item.officialRating() != null && !item.officialRating().isEmpty()) {
            wrote = writeKey(sb, wrote, "or");
            appendString(sb, item.officialRating());
        }
        if (item.overview() != null && !item.overview().isEmpty()) {
            wrote = writeKey(sb, wrote, "ov");
            appendString(sb, item.overview());
        }
        if (item.genres() != null && !item.genres().isEmpty()) {
            wrote = writeKey(sb, wrote, "g");
            sb.append('[');
            for (int i = 0; i < item.genres().size(); i++) {
                if (i > 0) sb.append(',');
                appendString(sb, item.genres().get(i));
            }
            sb.append(']');
        }
        if (item.imageTags() != null && !item.imageTags().isEmpty()) {
            wrote = writeKey(sb, wrote, "it");
            writeStringMap(sb, item.imageTags());
        }
        if (item.backdropImageTags() != null && !item.backdropImageTags().isEmpty()) {
            wrote = writeKey(sb, wrote, "bt");
            writeStringList(sb, item.backdropImageTags());
        }
        wrote = writeKey(sb, wrote, "u");
        writeUserData(sb, item.userData());
        if (item.providerIds() != null && !item.providerIds().isEmpty()) {
            wrote = writeKey(sb, true, "pid");
            writeStringMap(sb, item.providerIds());
        }
        sb.append('}');
    }

    /** Writes a leading comma (unless this is the very first key) followed by "\"key\":". */
    private static boolean writeKey(StringBuilder sb, boolean previousWrote, String key) {
        if (previousWrote) sb.append(',');
        sb.append('"').append(key).append("\":");
        return true;
    }

    private static void writeStringMap(StringBuilder sb, Map<String, String> tags) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
            if (!first) sb.append(',');
            first = false;
            appendString(sb, entry.getKey());
            sb.append(':');
            appendString(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeStringList(StringBuilder sb, List<String> values) {
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            appendString(sb, values.get(i));
        }
        sb.append(']');
    }

    private static void writeUserData(StringBuilder sb, UserItemData data) {
        sb.append('{');
        boolean wrote = false;
        if (data != null) {
            if (data.played()) {
                wrote = writeKey(sb, wrote, "p");
                sb.append('1');
            }
            if (data.playbackPositionTicks() > 0L) {
                wrote = writeKey(sb, wrote, "pos");
                sb.append(data.playbackPositionTicks());
            }
            if (data.playCount() > 0) {
                wrote = writeKey(sb, wrote, "c");
                sb.append(data.playCount());
            }
            if (data.favorite()) {
                wrote = writeKey(sb, wrote, "f");
                sb.append('1');
            }
            if (data.likes() != null) {
                wrote = writeKey(sb, wrote, "l");
                sb.append(data.likes() ? '1' : '0');
            }
            if (data.userRating() != null) {
                wrote = writeKey(sb, wrote, "r");
                sb.append(data.userRating());
            }
        }
        sb.append('}');
    }

    private static void appendString(StringBuilder sb, String s) {
        if (s == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ----------------------------- reader --------------------------------

    private static MediaItemSummary readItem(Map<String, Object> obj) {
        String id = JsonValue.string(obj, "id");
        if (id == null || id.isBlank()) return null;
        String name = sOrEmpty(obj, "n");
        MediaItemType type = typeFromString(JsonValue.string(obj, "type"));
        boolean folder = JsonValue.bool(obj, "f") || hasInt1(obj, "f");
        boolean playable = JsonValue.bool(obj, "p") || hasInt1(obj, "p");
        Long runTimeTicks = longOrNull(obj, "rt");
        Integer productionYear = intOrNull(obj, "py");
        Integer indexNumber = intOrNull(obj, "s");
        Integer parentIndexNumber = intOrNull(obj, "ss");
        String seriesName = sOrEmpty(obj, "sn");
        String seriesId = sOrEmpty(obj, "sid");
        String overview = sOrEmpty(obj, "ov");
        List<String> genres = readStringList(obj, "g");
        String premiereDate = sOrEmpty(obj, "pd");
        Double communityRating = doubleOrNull(obj, "cr");
        String officialRating = sOrEmpty(obj, "or");
        Map<String, String> imageTags = readStringMap(obj, "it");
        List<String> backdropTags = readStringList(obj, "bt");
        Map<String, String> providerIds = readStringMap(obj, "pid");
        UserItemData userData = readUserData(JsonValue.childObject(obj, "u"));
        return new MediaItemSummary(
                id, "", name, type, folder, playable,
                runTimeTicks, productionYear, indexNumber, parentIndexNumber,
                seriesName, seriesId, overview, genres,
                premiereDate, communityRating, officialRating,
                AndroidCollections.emptyList(), AndroidCollections.emptyList(),
                userData, imageTags, backdropTags, providerIds
        );
    }

    private static UserItemData readUserData(Map<String, Object> obj) {
        if (obj == null || obj.isEmpty()) return UserItemData.empty();
        boolean played = JsonValue.bool(obj, "p") || hasInt1(obj, "p");
        long pos = longOrZero(obj, "pos");
        int count = intOrZero(obj, "c");
        boolean fav = JsonValue.bool(obj, "f") || hasInt1(obj, "f");
        Boolean likes = obj.containsKey("l")
                ? (JsonValue.bool(obj, "l") || hasInt1(obj, "l"))
                : null;
        Double rating = doubleOrNull(obj, "r");
        return new UserItemData(played, pos, count, fav, likes, rating);
    }

    private static Map<String, String> readStringMap(Map<String, Object> parent, String key) {
        Map<String, Object> raw = JsonValue.childObject(parent, key);
        if (raw == null || raw.isEmpty()) return AndroidCollections.emptyMap();
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String s) out.put(e.getKey(), s);
        }
        return out.isEmpty() ? AndroidCollections.emptyMap() : AndroidCollections.mapCopy(out);
    }

    private static List<String> readStringList(Map<String, Object> parent, String key) {
        List<Object> raw = JsonValue.array(parent, key);
        if (raw == null || raw.isEmpty()) return AndroidCollections.emptyList();
        List<String> out = new ArrayList<>(raw.size());
        for (Object v : raw) {
            if (v instanceof String s) out.add(s);
        }
        return out.isEmpty() ? AndroidCollections.emptyList() : AndroidCollections.listCopy(out);
    }

    private static String sOrEmpty(Map<String, Object> obj, String k) {
        String s = JsonValue.string(obj, k);
        return s == null ? "" : s;
    }

    private static Integer intOrNull(Map<String, Object> obj, String k) {
        Object v = obj.get(k);
        if (v instanceof Number n) return n.intValue();
        return null;
    }

    private static int intOrZero(Map<String, Object> obj, String k) {
        Object v = obj.get(k);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private static Long longOrNull(Map<String, Object> obj, String k) {
        Object v = obj.get(k);
        if (v instanceof Number n) return n.longValue();
        return null;
    }

    private static Double doubleOrNull(Map<String, Object> obj, String k) {
        Object v = obj.get(k);
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    private static long longOrZero(Map<String, Object> obj, String k) {
        Object v = obj.get(k);
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    private static boolean hasInt1(Map<String, Object> obj, String k) {
        Object v = obj.get(k);
        return v instanceof Number n && n.intValue() == 1;
    }

    private static MediaItemType typeFromString(String s) {
        if (s == null || s.isEmpty()) return MediaItemType.UNKNOWN;
        try {
            return MediaItemType.valueOf(s);
        } catch (IllegalArgumentException ignored) {
            return MediaItemType.UNKNOWN;
        }
    }
}
