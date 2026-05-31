package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MediaBrowserRequests {
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
                .query("Fields", "PrimaryImageAspectRatio,MediaSources,MediaStreams,Overview,ParentId,Genres,ProductionYear")
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
                .query("Fields", "PrimaryImageAspectRatio,MediaSources,MediaStreams,Overview,ParentId,Genres,ProductionYear")
                .build();
    }

    public static ProtocolRequest nextUpItems(AuthSession session, ServerFlavor flavor, int limit) {
        return authenticated(ProtocolRequest.get("/Shows/NextUp"), session, flavor)
                .query("UserId", session.userId())
                .query("Limit", Integer.toString(limit))
                .query("EnableImages", "true")
                .query("EnableUserData", "true")
                .query("ImageTypeLimit", "1")
                .query("Fields", "PrimaryImageAspectRatio,MediaSources,MediaStreams,Overview,ParentId,Genres,ProductionYear")
                .build();
    }

    public static ProtocolRequest item(AuthSession session, ServerFlavor flavor, String itemId) {
        require(itemId, "itemId");
        String userId = ProtocolRequest.encodePathSegment(session.userId());
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        return authenticated(ProtocolRequest.get("/Users/" + userId + "/Items/" + encodedItemId), session, flavor)
                .query("Fields", "PrimaryImageAspectRatio,MediaSources,MediaStreams,Overview,ParentId,Genres,ProductionYear,Chapters")
                .build();
    }

    public static ProtocolRequest itemImage(
            AuthSession session,
            ServerFlavor flavor,
            String itemId,
            String imageType,
            String tag,
            int width,
            int height
    ) {
        require(itemId, "itemId");
        require(imageType, "imageType");
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        ProtocolRequest.Builder builder = authenticated(
                ProtocolRequest.get("/Items/" + encodedItemId + "/Images/" + ProtocolRequest.encodePathSegment(imageType)),
                session,
                flavor
        );
        if (tag != null && !tag.isBlank()) {
            builder.query("tag", tag);
        }
        if (width > 0) {
            builder.query("fillWidth", Integer.toString(width));
        }
        if (height > 0) {
            builder.query("fillHeight", Integer.toString(height));
        }
        builder.query("quality", "90");
        return builder.build();
    }

    public static ProtocolRequest playbackInfo(
            AuthSession session,
            ServerFlavor flavor,
            String itemId,
            PlaybackInfoOptions options
    ) {
        require(itemId, "itemId");
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        ProtocolRequest.Builder builder = authenticated(
                ProtocolRequest.get("/Items/" + encodedItemId + "/PlaybackInfo"),
                session,
                flavor
        ).query("UserId", session.userId());
        (options == null ? PlaybackInfoOptions.defaults() : options).applyTo(builder);
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

    private static ProtocolRequest.Builder authenticated(
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
}
