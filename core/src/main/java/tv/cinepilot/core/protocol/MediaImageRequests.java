package tv.cinepilot.core.protocol;

public final class MediaImageRequests {
    private MediaImageRequests() {
    }

    public static ProtocolRequest item(
            AuthSession session,
            ServerFlavor flavor,
            String itemId,
            String imageType,
            String tag,
            int width,
            int height
    ) {
        return item(session, flavor, itemId, imageType, -1, tag, width, height);
    }

    public static ProtocolRequest item(
            AuthSession session,
            ServerFlavor flavor,
            String itemId,
            String imageType,
            int imageIndex,
            String tag,
            int width,
            int height
    ) {
        require(itemId, "itemId");
        require(imageType, "imageType");
        String encodedItemId = ProtocolRequest.encodePathSegment(itemId);
        String imagePath = "/Items/" + encodedItemId + "/Images/" + ProtocolRequest.encodePathSegment(imageType);
        if (imageIndex >= 0) {
            imagePath += "/" + imageIndex;
        }
        ProtocolRequest.Builder builder = MediaBrowserRequests.authenticated(
                ProtocolRequest.get(imagePath),
                session,
                flavor
        );
        return imageQuery(builder, tag, width, height).build();
    }

    public static ProtocolRequest publicUser(
            ClientIdentity client,
            ServerFlavor flavor,
            String userId,
            String imageType,
            String tag,
            int width,
            int height
    ) {
        require(userId, "userId");
        require(imageType, "imageType");
        String encodedUserId = ProtocolRequest.encodePathSegment(userId);
        ProtocolRequest.Builder builder = ProtocolRequest
                .get("/Users/" + encodedUserId + "/Images/" + ProtocolRequest.encodePathSegment(imageType))
                .header("X-Emby-Authorization", MediaBrowserRequests.clientAuthorization(client, flavor));
        return imageQuery(builder, tag, width, height).build();
    }

    private static ProtocolRequest.Builder imageQuery(
            ProtocolRequest.Builder builder,
            String tag,
            int width,
            int height
    ) {
        if (tag != null && !tag.isBlank()) {
            builder.query("tag", tag);
        }
        if (width > 0) {
            builder.query("fillWidth", Integer.toString(width));
        }
        if (height > 0) {
            builder.query("fillHeight", Integer.toString(height));
        }
        return builder.query("quality", "90");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
