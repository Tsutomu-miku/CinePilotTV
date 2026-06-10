package tv.cinepilot.core.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tv.cinepilot.core.AndroidCollections;

public final class MediaBrowserResponseMapper {
    private MediaBrowserResponseMapper() {
    }

    public static ServerIdentity serverIdentity(MediaServerAddress address, String json) {
        Map<String, Object> root = JsonValue.object(json);
        String serverId = firstString(root, "Id", "ServerId");
        String serverName = firstString(root, "ServerName", "Name");
        String productName = firstString(root, "ProductName", "ServerName", "Name");
        return new ServerIdentity(address, serverId, ServerFlavor.fromServerName(productName), serverName);
    }

    public static AuthSession authSession(ClientIdentity client, String json) {
        Map<String, Object> root = JsonValue.object(json);
        Map<String, Object> user = JsonValue.childObject(root, "User");
        String accessToken = firstString(root, "AccessToken", "Token");
        String serverId = firstString(root, "ServerId");
        if (serverId == null) {
            serverId = JsonValue.string(JsonValue.childObject(root, "SessionInfo"), "ServerId");
        }
        String userId = firstString(user, "Id", "UserId");
        return new AuthSession(serverId, userId, accessToken, client);
    }

    public static List<PublicUserSummary> publicUsers(String json) {
        List<PublicUserSummary> users = new ArrayList<>();
        for (Object value : JsonValue.array(json)) {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> user = JsonValueMap.cast(map);
                users.add(new PublicUserSummary(
                        requiredString(user, "Id"),
                        valueOrEmpty(JsonValue.string(user, "Name")),
                        JsonValue.bool(user, "HasPassword")
                                || JsonValue.bool(user, "HasConfiguredPassword")
                                || JsonValue.bool(user, "HasConfiguredEasyPassword"),
                        valueOrEmpty(JsonValue.string(user, "PrimaryImageTag"))
                ));
            }
        }
        return AndroidCollections.listCopy(users);
    }

    public static QuickConnectSession quickConnectSession(String json) {
        Map<String, Object> root = JsonValue.object(json);
        return new QuickConnectSession(
                valueOrEmpty(JsonValue.string(root, "Code")),
                requiredString(root, "Secret"),
                JsonValue.bool(root, "Authenticated")
        );
    }

    public static PlaybackInfo playbackInfo(String itemId, String json) {
        Map<String, Object> root = JsonValue.object(json);
        String playSessionId = firstString(root, "PlaySessionId");
        List<MediaSourceInfo> mediaSources = new ArrayList<>();
        for (Object value : JsonValue.array(root, "MediaSources")) {
            if (value instanceof Map<?, ?> map) {
                mediaSources.add(mediaSource(JsonValueMap.cast(map)));
            }
        }
        return new PlaybackInfo(itemId, playSessionId, mediaSources);
    }

    public static List<ChapterInfo> chapters(String json) {
        List<ChapterInfo> out = new ArrayList<>();
        for (Object value : JsonValue.array(json)) {
            if (!(value instanceof Map<?, ?> map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> chapter = (Map<String, Object>) map;
            out.add(new ChapterInfo(
                    number(chapter, "StartPositionTicks").longValue(),
                    valueOrEmpty(JsonValue.string(chapter, "Name")),
                    valueOrEmpty(JsonValue.string(chapter, "ImageTag"))
            ));
        }
        return AndroidCollections.listCopy(out);
    }

    public static List<MediaSegmentInfo> mediaSegments(String json) {
        List<MediaSegmentInfo> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        List<Object> array;
        if (json.stripLeading().startsWith("[")) {
            array = JsonValue.array(json);
        } else {
            Map<String, Object> root = JsonValue.object(json);
            // Jellyfin 10.10 wraps as {"Items":[...]}; also accept {"Segments":[...]}
            List<Object> fromKey = JsonValue.array(root, "Items");
            if (fromKey.isEmpty()) fromKey = JsonValue.array(root, "Segments");
            array = fromKey;
        }
        for (Object value : array) {
            if (!(value instanceof Map<?, ?> map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> seg = (Map<String, Object>) map;
            MediaSegmentInfo.Type type = MediaSegmentInfo.Type.fromWireName(
                    firstString(seg, "Type", "SegmentType")
            );
            long start = number(seg, "StartPositionTicks").longValue();
            long end = number(seg, "EndPositionTicks").longValue();
            // Emby Premiere: IntroStartTicks / IntroEndTicks on the wrapping item.
            if (start == 0L && end == 0L) {
                long altStart = number(seg, "IntroStartTicks").longValue();
                long altEnd = number(seg, "IntroEndTicks").longValue();
                if (altStart > 0 || altEnd > 0) {
                    out.add(new MediaSegmentInfo(MediaSegmentInfo.Type.INTRO, altStart, altEnd));
                }
                long creditsStart = number(seg, "CreditsStartTicks").longValue();
                long creditsEnd = number(seg, "CreditsEndTicks").longValue();
                if (creditsStart > 0 || creditsEnd > 0) {
                    out.add(new MediaSegmentInfo(MediaSegmentInfo.Type.CREDITS, creditsStart, creditsEnd));
                }
                continue;
            }
            if (end > start) {
                out.add(new MediaSegmentInfo(type, start, end));
            }
        }
        return AndroidCollections.listCopy(out);
    }

    public static TrickplayInfo trickplayInfo(String json, String itemId, int tileWidth) {
        if (json == null || json.isBlank()) return TrickplayInfo.empty();
        Map<String, Object> root = JsonValue.object(json);
        Map<String, Object> selected = null;
        // Manifest groups resolutions by width.
        for (Object entry : JsonValue.array(root, "TileResolutions")) {
            if (!(entry instanceof Map<?, ?> map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> resolution = (Map<String, Object>) map;
            int width = number(resolution, "Width").intValue();
            if (selected == null) selected = resolution;
            if (width >= tileWidth && selected != null) {
                int selectedWidth = number(selected, "Width").intValue();
                // Prefer the smallest resolution that still meets the request, else the largest.
                if (selectedWidth < tileWidth || width < selectedWidth) selected = resolution;
            }
        }
        if (selected == null) return TrickplayInfo.empty();
        int w = number(selected, "Width").intValue();
        int h = number(selected, "Height").intValue();
        int tilesPerRow = number(selected, "TileWidth").intValue();
        int tilesPerCol = number(selected, "TileHeight").intValue();
        int tileCount = number(selected, "TileCount").intValue();
        long intervalTicks = number(selected, "IntervalTicks").longValue();
        String fileName = valueOrEmpty(JsonValue.string(selected, "FileName"));
        if (fileName.isBlank()) {
            // Jellyfin convention: {width}p.jpg
            fileName = w + "p.jpg";
        }
        String url = "/Videos/" + ProtocolRequest.encodePathSegment(itemId)
                + "/Trickplay/" + fileName;
        return new TrickplayInfo(w, h, tilesPerRow, tilesPerCol, tileCount, intervalTicks, url);
    }

    public static MediaItemPage itemPage(String json) {
        if (json != null && json.stripLeading().startsWith("[")) {
            List<MediaItemSummary> items = mediaItems(JsonValue.array(json));
            return new MediaItemPage(items, items.size(), 0);
        }
        Map<String, Object> root = JsonValue.object(json);
        List<MediaItemSummary> items = mediaItems(JsonValue.array(root, "Items"));
        return new MediaItemPage(
                items,
                number(root, "TotalRecordCount").intValue(),
                number(root, "StartIndex").intValue()
        );
    }

    private static List<MediaItemSummary> mediaItems(List<Object> values) {
        List<MediaItemSummary> items = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Map<?, ?> map) {
                items.add(mediaItem(JsonValueMap.cast(map)));
            }
        }
        return AndroidCollections.listCopy(items);
    }

    public static MediaItemSummary item(String json) {
        return mediaItem(JsonValue.object(json));
    }

    private static MediaSourceInfo mediaSource(Map<String, Object> source) {
        List<MediaStreamInfo> streams = new ArrayList<>();
        for (Object value : JsonValue.array(source, "MediaStreams")) {
            if (value instanceof Map<?, ?> map) {
                streams.add(mediaStream(JsonValueMap.cast(map)));
            }
        }
        return MediaSourceInfo.builder(requiredString(source, "Id"))
                .container(valueOrEmpty(JsonValue.string(source, "Container")))
                .directStreamUrl(JsonValue.string(source, "DirectStreamUrl"))
                .transcodingUrl(JsonValue.string(source, "TranscodingUrl"))
                .supportsDirectPlay(JsonValue.bool(source, "SupportsDirectPlay"))
                .supportsDirectStream(JsonValue.bool(source, "SupportsDirectStream"))
                .supportsTranscoding(JsonValue.bool(source, "SupportsTranscoding"))
                .name(valueOrEmpty(JsonValue.string(source, "Name")))
                .path(valueOrEmpty(JsonValue.string(source, "Path")))
                .sizeBytes(number(source, "Size").longValue())
                .bitRate(number(source, "Bitrate").longValue())
                .mediaStreams(streams)
                .build();
    }

    private static MediaStreamInfo mediaStream(Map<String, Object> stream) {
        return new MediaStreamInfo(
                number(stream, "Index").intValue(),
                MediaStreamType.fromWireName(JsonValue.string(stream, "Type")),
                JsonValue.string(stream, "Codec"),
                JsonValue.string(stream, "Language"),
                JsonValue.string(stream, "DisplayTitle"),
                optionalInt(stream, "Width"),
                optionalInt(stream, "Height"),
                optionalInt(stream, "Channels"),
                optionalInt(stream, "BitDepth"),
                number(stream, "BitRate").longValue(),
                JsonValue.string(stream, "Profile"),
                JsonValue.string(stream, "VideoRange"),
                JsonValue.string(stream, "VideoRangeType"),
                JsonValue.bool(stream, "IsDefault"),
                JsonValue.bool(stream, "IsForced"),
                JsonValue.bool(stream, "IsExternal"),
                valueOrEmpty(JsonValue.string(stream, "DeliveryMethod")),
                JsonValue.string(stream, "DeliveryUrl")
        );
    }

    private static MediaItemSummary mediaItem(Map<String, Object> item) {
        MediaItemType type = MediaItemType.fromWireName(JsonValue.string(item, "Type"));
        boolean folder = JsonValue.bool(item, "IsFolder");
        return new MediaItemSummary(
                requiredString(item, "Id"),
                valueOrEmpty(JsonValue.string(item, "ParentId")),
                valueOrEmpty(JsonValue.string(item, "Name")),
                type,
                folder,
                playable(item, type, folder),
                optionalLong(item, "RunTimeTicks"),
                optionalInt(item, "ProductionYear"),
                optionalInt(item, "IndexNumber"),
                optionalInt(item, "ParentIndexNumber"),
                valueOrEmpty(JsonValue.string(item, "SeriesName")),
                valueOrEmpty(JsonValue.string(item, "SeriesId")),
                valueOrEmpty(JsonValue.string(item, "Overview")),
                stringList(item, "Genres"),
                valueOrEmpty(JsonValue.string(item, "PremiereDate")),
                optionalDouble(item, "CommunityRating"),
                valueOrEmpty(JsonValue.string(item, "OfficialRating")),
                people(item),
                mediaStreams(item),
                userData(JsonValue.childObject(item, "UserData")),
                imageTags(JsonValue.childObject(item, "ImageTags")),
                stringList(item, "BackdropImageTags"),
                providerIds(JsonValue.childObject(item, "ProviderIds")),
                chapters(item)
        );
    }

    private static List<ChapterInfo> chapters(Map<String, Object> item) {
        List<ChapterInfo> chapters = new ArrayList<>();
        for (Object value : JsonValue.array(item, "Chapters")) {
            if (!(value instanceof Map<?, ?> map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> chapter = (Map<String, Object>) map;
            chapters.add(new ChapterInfo(
                    number(chapter, "StartPositionTicks").longValue(),
                    valueOrEmpty(JsonValue.string(chapter, "Name")),
                    valueOrEmpty(JsonValue.string(chapter, "ImageTag"))
            ));
        }
        return AndroidCollections.listCopy(chapters);
    }

    private static List<MediaPerson> people(Map<String, Object> item) {
        List<MediaPerson> people = new ArrayList<>();
        for (Object value : JsonValue.array(item, "People")) {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> person = JsonValueMap.cast(map);
                people.add(new MediaPerson(
                        valueOrEmpty(JsonValue.string(person, "Id")),
                        valueOrEmpty(JsonValue.string(person, "Name")),
                        valueOrEmpty(JsonValue.string(person, "Role")),
                        valueOrEmpty(JsonValue.string(person, "Type")),
                        valueOrEmpty(JsonValue.string(person, "PrimaryImageTag"))
                ));
            }
        }
        return AndroidCollections.listCopy(people);
    }

    private static List<MediaStreamInfo> mediaStreams(Map<String, Object> item) {
        List<MediaStreamInfo> streams = new ArrayList<>();
        for (Object value : JsonValue.array(item, "MediaStreams")) {
            if (value instanceof Map<?, ?> map) {
                streams.add(mediaStream(JsonValueMap.cast(map)));
            }
        }
        return AndroidCollections.listCopy(streams);
    }

    private static boolean playable(Map<String, Object> item, MediaItemType type, boolean folder) {
        if (item.containsKey("IsPlayable")) {
            return JsonValue.bool(item, "IsPlayable");
        }
        return !folder && (type == MediaItemType.MOVIE || type == MediaItemType.EPISODE || type == MediaItemType.VIDEO);
    }

    private static UserItemData userData(Map<String, Object> object) {
        if (object.isEmpty()) {
            return UserItemData.empty();
        }
        Boolean likes = object.containsKey("Likes")
                ? JsonValue.bool(object, "Likes")
                : null;
        Double rating = object.containsKey("Rating")
                ? optionalDouble(object, "Rating")
                : null;
        return new UserItemData(
                JsonValue.bool(object, "Played"),
                number(object, "PlaybackPositionTicks").longValue(),
                number(object, "PlayCount").intValue(),
                JsonValue.bool(object, "IsFavorite"),
                likes,
                rating
        );
    }

    private static Map<String, String> providerIds(Map<String, Object> object) {
        Map<String, String> ids = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            if (entry.getValue() instanceof String value) {
                ids.put(entry.getKey(), value);
            } else if (entry.getValue() instanceof Number number) {
                ids.put(entry.getKey(), number.toString());
            }
        }
        return ids;
    }

    private static Map<String, String> imageTags(Map<String, Object> object) {
        Map<String, String> tags = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            if (entry.getValue() instanceof String value) {
                tags.put(entry.getKey(), value);
            }
        }
        return tags;
    }

    private static List<String> stringList(Map<String, Object> object, String key) {
        List<String> values = new ArrayList<>();
        for (Object value : JsonValue.array(object, key)) {
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                values.add(stringValue);
            }
        }
        return values;
    }

    private static String firstString(Map<String, Object> object, String... keys) {
        for (String key : keys) {
            String value = JsonValue.string(object, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String requiredString(Map<String, Object> object, String key) {
        String value = JsonValue.string(object, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static Number number(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value instanceof Number number) {
            return number;
        }
        return 0;
    }

    private static Long optionalLong(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer optionalInt(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Double optionalDouble(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class JsonValueMap {
        @SuppressWarnings("unchecked")
        static Map<String, Object> cast(Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
    }
}
