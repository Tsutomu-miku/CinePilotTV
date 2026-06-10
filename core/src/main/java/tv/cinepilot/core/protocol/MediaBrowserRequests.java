package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MediaBrowserRequests {
    private static final String ITEM_FIELDS = "PrimaryImageAspectRatio,MediaSources,MediaStreams,Overview,"
            + "ParentId,Genres,ProductionYear,SeriesId,PremiereDate,CommunityRating,OfficialRating,People,"
            + "ProviderIds,Chapters,ExtraType";

    private MediaBrowserRequests() {
    }

    public static ProtocolRequest publicSystemInfo() {
        return ProtocolRequest.get("/System/Info/Public")
                .header("Accept", "application/json")
                .build();
    }

    public static ProtocolRequest systemInfo(AuthSession session, ServerFlavor flavor) {
        return authenticated(ProtocolRequest.get("/System/Info"), session, flavor).build();
    }

    public static ProtocolRequest publicUsers(ClientIdentity client, ServerFlavor flavor) {
        return ProtocolRequest.get("/Users/Public")
                .header("Accept", "application/json")
                .header("X-Emby-Authorization", clientAuthorization(client, flavor))
                .build();
    }

    public static ProtocolRequest authenticateByName(
            ClientIdentity client,
            ServerFlavor flavor,
            String username,
            String password
    ) {
        require(username, "username");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Username", username);
        body.put("Pw", password == null ? "" : password);
        return ProtocolRequest.post("/Users/AuthenticateByName")
                .header("X-Emby-Authorization", clientAuthorization(client, flavor))
                .jsonBody(JsonPayload.object(body))
                .build();
    }

    public static ProtocolRequest authenticateWithQuickConnect(
            ClientIdentity client,
            ServerFlavor flavor,
            String secret
    ) {
        require(secret, "secret");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Secret", secret);
        return ProtocolRequest.post("/Users/AuthenticateWithQuickConnect")
                .header("X-Emby-Authorization", clientAuthorization(client, flavor))
                .jsonBody(JsonPayload.object(body))
                .build();
    }

    public static ProtocolRequest quickConnectEnabled() {
        return ProtocolRequest.get("/QuickConnect/Enabled")
                .header("Accept", "application/json")
                .build();
    }

    public static ProtocolRequest initiateQuickConnect() {
        return ProtocolRequest.post("/QuickConnect/Initiate")
                .header("Accept", "application/json")
                .build();
    }

    public static ProtocolRequest quickConnectState(String secret) {
        require(secret, "secret");
        return ProtocolRequest.get("/QuickConnect/Connect")
                .header("Accept", "application/json")
                .query("secret", secret)
                .build();
    }

    public static ProtocolRequest logout(AuthSession session, ServerFlavor flavor) {
        return authenticated(ProtocolRequest.post("/Sessions/Logout"), session, flavor).build();
    }

    public static ProtocolRequest userViews(AuthSession session, ServerFlavor flavor) {
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        return authenticated(ProtocolRequest.get("/Users/" + userId + "/Views"), session, flavor)
                .query("EnableImages", "true")
                .query("EnableUserData", "true")
                .build();
    }

    public static ProtocolRequest items(AuthSession session, ServerFlavor flavor, ItemQuery query) {
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        ProtocolRequest.Builder builder = authenticated(
                ProtocolRequest.get("/Users/" + userId + "/Items"),
                session,
                flavor
        );
        for (Map.Entry<String, String> entry : query.values().entrySet()) {
            builder.query(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    public static ProtocolRequest resumeItems(AuthSession session, ServerFlavor flavor, int limit) {
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        return authenticated(ProtocolRequest.get("/Users/" + userId + "/Items/Resume"), session, flavor)
                .query("MediaTypes", "Video")
                .query("Limit", Integer.toString(limit))
                .query("EnableImages", "true")
                .query("EnableUserData", "true")
                .query("ImageTypeLimit", "1")
                .query("EnableImageTypes", "Primary,Backdrop,Thumb")
                .query("Fields", ITEM_FIELDS)
                .build();
    }

    public static ProtocolRequest latestItems(AuthSession session, ServerFlavor flavor, String parentId, int limit) {
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        return authenticated(ProtocolRequest.get("/Users/" + userId + "/Items/Latest"), session, flavor)
                .query("ParentId", parentId)
                .query("Limit", Integer.toString(limit))
                .query("EnableImages", "true")
                .query("EnableUserData", "true")
                .query("ImageTypeLimit", "1")
                .query("EnableImageTypes", "Primary,Backdrop,Thumb")
                .query("Fields", ITEM_FIELDS)
                .build();
    }

    public static ProtocolRequest nextUpItems(AuthSession session, ServerFlavor flavor, int limit) {
        return nextUpItems(session, flavor, limit, "");
    }

    public static ProtocolRequest nextUpItems(AuthSession session, ServerFlavor flavor, int limit, String seriesId) {
        ProtocolRequest.Builder builder = authenticated(ProtocolRequest.get("/Shows/NextUp"), session, flavor)
                .query("UserId", session.userId())
                .query("Limit", Integer.toString(limit))
                .query("EnableImages", "true")
                .query("EnableUserData", "true")
                .query("ImageTypeLimit", "1")
                .query("EnableImageTypes", "Primary,Backdrop,Thumb")
                .query("Fields", ITEM_FIELDS);
        if (seriesId != null && !seriesId.isBlank()) {
            builder.query("SeriesId", seriesId);
        }
        return builder.build();
    }

    public static ProtocolRequest item(AuthSession session, ServerFlavor flavor, String itemId) {
        require(itemId, "itemId");
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        return authenticated(ProtocolRequest.get("/Users/" + userId + "/Items/" + encodedItemId), session, flavor)
                .query("EnableImages", "true")
                .query("ImageTypeLimit", "1")
                .query("EnableImageTypes", "Primary,Backdrop,Thumb")
                .query("Fields", ITEM_FIELDS + ",Chapters")
                .build();
    }

    public static ProtocolRequest playbackInfo(
            AuthSession session,
            ServerFlavor flavor,
            String itemId,
            PlaybackInfoOptions options
    ) {
        require(itemId, "itemId");
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        PlaybackInfoOptions safeOptions = options == null ? PlaybackInfoOptions.defaults() : options;
        ProtocolRequest.Builder builder;
        if (safeOptions.requiresPostBody()) {
            builder = authenticated(
                    ProtocolRequest.post("/Items/" + encodedItemId + "/PlaybackInfo"),
                    session,
                    flavor
            ).jsonBody(safeOptions.bodyJson(session.userId()));
        } else {
            builder = authenticated(
                    ProtocolRequest.get("/Items/" + encodedItemId + "/PlaybackInfo"),
                    session,
                    flavor
            ).query("UserId", session.userId());
            safeOptions.applyTo(builder);
        }
        return builder.build();
    }

    public static ProtocolRequest hlsStream(AuthSession session, ServerFlavor flavor, HlsStreamOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options is required");
        }
        String encodedItemId = ProtocolRequest.encodePathSegment(options.itemId());
        ProtocolRequest.Builder builder = authenticated(
                ProtocolRequest.get("/Videos/" + encodedItemId + "/master.m3u8"),
                session,
                flavor
        );
        options.applyTo(builder, session.client());
        return builder.build();
    }

    public static ProtocolRequest staticVideoStream(
            AuthSession session,
            ServerFlavor flavor,
            String itemId,
            String mediaSourceId,
            String playSessionId
    ) {
        require(itemId, "itemId");
        require(mediaSourceId, "mediaSourceId");
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        ProtocolRequest.Builder builder = authenticated(
                ProtocolRequest.get("/Videos/" + encodedItemId + "/stream"),
                session,
                flavor
        ).query("Static", "true")
                .query("MediaSourceId", mediaSourceId)
                .query("DeviceId", session.client().deviceId());
        if (playSessionId != null && !playSessionId.isBlank()) {
            builder.query("PlaySessionId", playSessionId);
        }
        return builder.build();
    }

    public static ProtocolRequest playbackStarted(AuthSession session, ServerFlavor flavor, PlaybackReport report) {
        return playbackReportRequest(session, flavor, PlaybackEndpoint.STARTED, report.toPayload());
    }

    public static ProtocolRequest playbackProgress(
            AuthSession session,
            ServerFlavor flavor,
            PlaybackReport report,
            PlaybackEvent event
    ) {
        return playbackReportRequest(session, flavor, PlaybackEndpoint.PROGRESS, report.toProgressPayload(event));
    }

    public static ProtocolRequest playbackStopped(AuthSession session, ServerFlavor flavor, PlaybackReport report) {
        return playbackReportRequest(session, flavor, PlaybackEndpoint.STOPPED, report.toPayload());
    }

    static String clientAuthorization(ClientIdentity client, ServerFlavor flavor) {
        String scheme = flavor == ServerFlavor.EMBY ? "Emby" : "MediaBrowser";
        return scheme + " " +
                "Client=\"" + escape(client.clientName()) + "\", " +
                "Device=\"" + escape(client.deviceName()) + "\", " +
                "DeviceId=\"" + escape(client.deviceId()) + "\", " +
                "Version=\"" + escape(client.version()) + "\"";
    }

    static ProtocolRequest.Builder authenticated(
            ProtocolRequest.Builder builder,
            AuthSession session,
            ServerFlavor flavor
    ) {
        return builder
                .header("Accept", "application/json")
                .header("X-Emby-Authorization", session.authorizationValue(flavor))
                .header(session.legacyTokenHeaderName(), session.accessToken());
    }

    private static ProtocolRequest playbackReportRequest(
            AuthSession session,
            ServerFlavor flavor,
            PlaybackEndpoint endpoint,
            Map<String, Object> payload
    ) {
        return authenticated(ProtocolRequest.post(endpoint.path()), session, flavor)
                .jsonBody(JsonPayload.object(payload))
                .build();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    // ---- User data state endpoints (P1-10 favorite / played / rating) ----

    public static ProtocolRequest addFavorite(AuthSession session, ServerFlavor flavor, String itemId) {
        require(itemId, "itemId");
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        return authenticated(
                ProtocolRequest.post("/Users/" + userId + "/FavoriteItems/" + encodedItemId),
                session,
                flavor
        ).build();
    }

    public static ProtocolRequest removeFavorite(AuthSession session, ServerFlavor flavor, String itemId) {
        require(itemId, "itemId");
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        return authenticated(
                ProtocolRequest.delete("/Users/" + userId + "/FavoriteItems/" + encodedItemId),
                session,
                flavor
        ).build();
    }

    public static ProtocolRequest markPlayed(AuthSession session, ServerFlavor flavor, String itemId) {
        require(itemId, "itemId");
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        return authenticated(
                ProtocolRequest.post("/Users/" + userId + "/PlayedItems/" + encodedItemId),
                session,
                flavor
        ).build();
    }

    public static ProtocolRequest markUnplayed(AuthSession session, ServerFlavor flavor, String itemId) {
        require(itemId, "itemId");
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        return authenticated(
                ProtocolRequest.delete("/Users/" + userId + "/PlayedItems/" + encodedItemId),
                session,
                flavor
        ).build();
    }

    public static ProtocolRequest setRating(
            AuthSession session,
            ServerFlavor flavor,
            String itemId,
            Double ratingZeroToTen
    ) {
        require(itemId, "itemId");
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        ProtocolRequest.Builder builder = authenticated(
                ProtocolRequest.post("/Users/" + session.userId() + "/Items/" + encodedItemId + "/Rating"),
                session,
                flavor
        );
        if (ratingZeroToTen == null) {
            builder.query("DeleteRating", "true");
        } else {
            double clamped = Math.max(0.0, Math.min(10.0, ratingZeroToTen));
            builder.query("Likes", "true");
            builder.query("Rating", Double.toString(clamped));
        }
        return builder.build();
    }

    // ---- Collections / BoxSets row (P1-16) ----

    public static ProtocolRequest collections(AuthSession session, ServerFlavor flavor, int limit) {
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        return authenticated(
                ProtocolRequest.get("/Users/" + userId + "/Items"),
                session,
                flavor
        ).query("IncludeItemTypes", "BoxSet")
                .query("Recursive", "true")
                .query("Limit", Integer.toString(Math.max(0, limit)))
                .query("EnableImages", "true")
                .query("EnableUserData", "true")
                .query("ImageTypeLimit", "1")
                .query("EnableImageTypes", "Primary,Backdrop,Thumb")
                .query("SortBy", "SortName")
                .query("SortOrder", "Ascending")
                .query("Fields", ITEM_FIELDS)
                .build();
    }
}
