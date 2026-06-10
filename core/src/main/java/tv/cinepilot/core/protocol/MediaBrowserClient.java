package tv.cinepilot.core.protocol;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import tv.cinepilot.core.AndroidCollections;

public final class MediaBrowserClient {
    private final HttpTransport transport;
    private final SessionRepository sessions;
    private final ClientIdentity client;

    public MediaBrowserClient(HttpTransport transport, SessionRepository sessions, ClientIdentity client) {
        if (transport == null) {
            throw new IllegalArgumentException("transport is required");
        }
        if (sessions == null) {
            throw new IllegalArgumentException("sessions is required");
        }
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        this.transport = transport;
        this.sessions = sessions;
        this.client = client;
    }

    public ServerIdentity discover(MediaServerAddress address) {
        ProtocolResponse response = send(address, MediaBrowserRequests.publicSystemInfo());
        return MediaBrowserResponseMapper.serverIdentity(address, response.body());
    }

    public AuthenticatedServer authenticate(ServerIdentity server, String username, String password) {
        ProtocolRequest request = MediaBrowserRequests.authenticateByName(
                client,
                server.flavor(),
                username,
                password
        );
        ProtocolResponse response = send(server.address(), request);
        AuthSession session = MediaBrowserResponseMapper.authSession(client, response.body());
        SavedSession saved = new SavedSession(SessionScope.from(server, session), session.accessToken());
        sessions.save(saved);
        return new AuthenticatedServer(server, session);
    }

    public boolean quickConnectEnabled(ServerIdentity server) {
        ProtocolResponse response = send(server.address(), MediaBrowserRequests.quickConnectEnabled());
        return Boolean.parseBoolean(response.body());
    }

    public QuickConnectSession initiateQuickConnect(ServerIdentity server) {
        ProtocolResponse response = send(server.address(), MediaBrowserRequests.initiateQuickConnect());
        return MediaBrowserResponseMapper.quickConnectSession(response.body());
    }

    public QuickConnectSession quickConnectState(ServerIdentity server, String secret) {
        ProtocolResponse response = send(server.address(), MediaBrowserRequests.quickConnectState(secret));
        return MediaBrowserResponseMapper.quickConnectSession(response.body());
    }

    public AuthenticatedServer authenticateWithQuickConnect(ServerIdentity server, String secret) {
        ProtocolRequest request = MediaBrowserRequests.authenticateWithQuickConnect(
                client,
                server.flavor(),
                secret
        );
        ProtocolResponse response = send(server.address(), request);
        AuthSession session = MediaBrowserResponseMapper.authSession(client, response.body());
        SavedSession saved = new SavedSession(SessionScope.from(server, session), session.accessToken());
        sessions.save(saved);
        return new AuthenticatedServer(server, session);
    }

    public List<PublicUserSummary> publicUsers(ServerIdentity server) {
        ProtocolResponse response = send(
                server.address(),
                MediaBrowserRequests.publicUsers(client, server.flavor())
        );
        return MediaBrowserResponseMapper.publicUsers(response.body());
    }

    public Optional<AuthSession> restore(ServerIdentity server, String userId) {
        SessionScope scope = new SessionScope(
                server.serverId(),
                server.address().value(),
                userId,
                client.clientName(),
                client.deviceId(),
                client.version()
        );
        return sessions.find(scope).map(saved -> saved.restore(client));
    }

    public PlaybackInfo playbackInfo(
            AuthenticatedServer authenticated,
            String itemId,
            PlaybackInfoOptions options
    ) {
        ProtocolResponse response = send(
                authenticated.server().address(),
                MediaBrowserRequests.playbackInfo(
                        authenticated.session(),
                        authenticated.server().flavor(),
                        itemId,
                        options
                )
        );
        return MediaBrowserResponseMapper.playbackInfo(itemId, response.body());
    }

    public Optional<PlayableMedia> playableMedia(
            AuthenticatedServer authenticated,
            PlaybackInfo playbackInfo,
            PlaybackSelectionPreferences preferences
    ) {
        return PlaybackSourceSelector.select(
                authenticated.server().address(),
                authenticated.session(),
                authenticated.server().flavor(),
                playbackInfo,
                preferences
        );
    }

    public MediaItemPage userViews(AuthenticatedServer authenticated) {
        ProtocolResponse response = send(
                authenticated.server().address(),
                MediaBrowserRequests.userViews(authenticated.session(), authenticated.server().flavor())
        );
        return MediaBrowserResponseMapper.itemPage(response.body());
    }

    public MediaItemPage items(AuthenticatedServer authenticated, ItemQuery query) {
        ProtocolResponse response = send(
                authenticated.server().address(),
                MediaBrowserRequests.items(authenticated.session(), authenticated.server().flavor(), query)
        );
        return MediaBrowserResponseMapper.itemPage(response.body());
    }

    public MediaItemPage resumeItems(AuthenticatedServer authenticated, int limit) {
        ProtocolResponse response = send(
                authenticated.server().address(),
                MediaBrowserRequests.resumeItems(authenticated.session(), authenticated.server().flavor(), limit)
        );
        return MediaBrowserResponseMapper.itemPage(response.body());
    }

    public MediaItemPage latestItems(AuthenticatedServer authenticated, String parentId, int limit) {
        ProtocolResponse response = send(
                authenticated.server().address(),
                MediaBrowserRequests.latestItems(authenticated.session(), authenticated.server().flavor(), parentId, limit)
        );
        return MediaBrowserResponseMapper.itemPage(response.body());
    }

    public MediaItemPage nextUpItems(AuthenticatedServer authenticated, int limit) {
        ProtocolResponse response = send(
                authenticated.server().address(),
                MediaBrowserRequests.nextUpItems(authenticated.session(), authenticated.server().flavor(), limit)
        );
        return MediaBrowserResponseMapper.itemPage(response.body());
    }

    public MediaItemPage nextUpItems(AuthenticatedServer authenticated, String seriesId, int limit) {
        ProtocolResponse response = send(
                authenticated.server().address(),
                MediaBrowserRequests.nextUpItems(authenticated.session(), authenticated.server().flavor(), limit, seriesId)
        );
        return MediaBrowserResponseMapper.itemPage(response.body());
    }

    public MediaItemSummary item(AuthenticatedServer authenticated, String itemId) {
        ProtocolResponse response = send(
                authenticated.server().address(),
                MediaBrowserRequests.item(authenticated.session(), authenticated.server().flavor(), itemId)
        );
        return MediaBrowserResponseMapper.item(response.body());
    }

    public String primaryImageUrl(AuthenticatedServer authenticated, MediaItemSummary item, int width, int height) {
        return itemImageUrl(authenticated, item, "Primary", width, height);
    }

    public String backdropImageUrl(AuthenticatedServer authenticated, MediaItemSummary item, int width, int height) {
        if (item == null) {
            throw new IllegalArgumentException("item is required");
        }
        if (!item.backdropImageTags().isEmpty()) {
            return itemImageUrl(authenticated, item, "Backdrop", 0, item.backdropImageTags().get(0), width, height);
        }
        if (item.imageTags().containsKey("Thumb")) {
            return itemImageUrl(authenticated, item, "Thumb", width, height);
        }
        return primaryImageUrl(authenticated, item, width, height);
    }

    public String itemImageUrl(
            AuthenticatedServer authenticated,
            MediaItemSummary item,
            String imageType,
            int width,
            int height
    ) {
        if (item == null) {
            throw new IllegalArgumentException("item is required");
        }
        return itemImageUrl(authenticated, item, imageType, -1, item.imageTags().get(imageType), width, height);
    }

    public String itemImageUrl(
            AuthenticatedServer authenticated,
            MediaItemSummary item,
            String imageType,
            int imageIndex,
            String tag,
            int width,
            int height
    ) {
        if (item == null) {
            throw new IllegalArgumentException("item is required");
        }
        if (imageType == null || imageType.isBlank()) {
            throw new IllegalArgumentException("imageType is required");
        }
        if (tag == null || tag.isBlank()) {
            return "";
        }
        String url = MediaImageRequests.item(
                authenticated.session(),
                authenticated.server().flavor(),
                item.id(),
                imageType,
                imageIndex,
                tag,
                width,
                height
        ).url(authenticated.server().address());
        return PlaybackUrlAuthorizer.withAccessToken(url, authenticated.session());
    }

    public String publicUserImageUrl(ServerIdentity server, PublicUserSummary user, int width, int height) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        if (user == null) {
            throw new IllegalArgumentException("user is required");
        }
        if (user.primaryImageTag().isBlank()) {
            return "";
        }
        return MediaImageRequests.publicUser(
                client,
                server.flavor(),
                user.id(),
                "Primary",
                user.primaryImageTag(),
                width,
                height
        ).url(server.address());
    }

    public String personImageUrl(AuthenticatedServer authenticated, MediaPerson person, int width, int height) {
        if (authenticated == null) {
            throw new IllegalArgumentException("authenticated is required");
        }
        if (person == null) {
            throw new IllegalArgumentException("person is required");
        }
        if (person.id().isBlank() || person.primaryImageTag().isBlank()) {
            return "";
        }
        String url = MediaImageRequests.item(
                authenticated.session(),
                authenticated.server().flavor(),
                person.id(),
                "Primary",
                person.primaryImageTag(),
                width,
                height
        ).url(authenticated.server().address());
        return PlaybackUrlAuthorizer.withAccessToken(url, authenticated.session());
    }

    public void sendPlaybackCheckIn(AuthenticatedServer authenticated, PlaybackCheckIn checkIn) {
        if (checkIn == null) {
            throw new IllegalArgumentException("checkIn is required");
        }
        ProtocolRequest request = switch (checkIn.endpoint()) {
            case STARTED -> MediaBrowserRequests.playbackStarted(
                    authenticated.session(),
                    authenticated.server().flavor(),
                    checkIn.report()
            );
            case PROGRESS -> MediaBrowserRequests.playbackProgress(
                    authenticated.session(),
                    authenticated.server().flavor(),
                    checkIn.report(),
                    checkIn.event() == null ? PlaybackEvent.TIME_UPDATE : checkIn.event()
            );
            case STOPPED -> MediaBrowserRequests.playbackStopped(
                    authenticated.session(),
                    authenticated.server().flavor(),
                    checkIn.report()
            );
        };
        send(authenticated.server().address(), request);
    }

    public void logout(AuthenticatedServer authenticated) {
        try {
            send(
                    authenticated.server().address(),
                    MediaBrowserRequests.logout(authenticated.session(), authenticated.server().flavor())
            );
        } finally {
            forget(authenticated);
        }
    }

    // ---- User state (P1-10 favorite / played / rating) ----

    public void toggleFavorite(AuthenticatedServer authenticated, String itemId, boolean shouldBeFavorite) {
        require(itemId, "itemId");
        ProtocolRequest request = shouldBeFavorite
                ? MediaBrowserRequests.addFavorite(
                        authenticated.session(), authenticated.server().flavor(), itemId)
                : MediaBrowserRequests.removeFavorite(
                        authenticated.session(), authenticated.server().flavor(), itemId);
        send(authenticated.server().address(), request);
    }

    public void markWatched(AuthenticatedServer authenticated, String itemId, boolean shouldBeWatched) {
        require(itemId, "itemId");
        ProtocolRequest request = shouldBeWatched
                ? MediaBrowserRequests.markPlayed(
                        authenticated.session(), authenticated.server().flavor(), itemId)
                : MediaBrowserRequests.markUnplayed(
                        authenticated.session(), authenticated.server().flavor(), itemId);
        send(authenticated.server().address(), request);
    }

    public void setRating(AuthenticatedServer authenticated, String itemId, Double ratingZeroToTen) {
        require(itemId, "itemId");
        ProtocolRequest request = MediaBrowserRequests.setRating(
                authenticated.session(), authenticated.server().flavor(), itemId, ratingZeroToTen);
        send(authenticated.server().address(), request);
    }

    public MediaItemPage personItems(AuthenticatedServer authenticated, String personId, int limit) {
        require(personId, "personId");
        ItemQuery query = ItemQuery.browse()
                .recursive(true)
                .personIds(personId)
                .includeItemTypes("Movie,Series,Episode,Video")
                .mediaTypes("Video")
                .limit(Math.max(0, limit))
                .sortBy("ProductionYear,SortName")
                .sortOrder("Descending,Ascending")
                .build();
        return items(authenticated, query);
    }

    public MediaItemPage favoriteItems(AuthenticatedServer authenticated, int limit) {
        ItemQuery query = ItemQuery.browse()
                .recursive(true)
                .filters("IsFavorite")
                .mediaTypes("Video")
                .includeItemTypes("Movie,Series,Episode,Video")
                .limit(Math.max(0, limit))
                .sortBy("DatePlayed,CommunityRating,SortName")
                .sortOrder("Descending,Descending,Ascending")
                .build();
        return items(authenticated, query);
    }

    public MediaItemPage collections(AuthenticatedServer authenticated, int limit) {
        ProtocolRequest request = MediaBrowserRequests.collections(
                authenticated.session(), authenticated.server().flavor(), limit);
        ProtocolResponse response = send(authenticated.server().address(), request);
        return MediaBrowserResponseMapper.itemPage(response.body());
    }

    // ---- Chapters, MediaSegments, Trickplay (P1 batch 6) ----

    public List<ChapterInfo> chapters(AuthenticatedServer authenticated, String itemId) {
        require(itemId, "itemId");
        try {
            ProtocolResponse response = send(authenticated.server().address(),
                    MediaBrowserRequests.chapters(authenticated.session(),
                            authenticated.server().flavor(), itemId));
            return MediaBrowserResponseMapper.chapters(response.body());
        } catch (MediaBrowserException exception) {
            return AndroidCollections.emptyList();
        }
    }

    public List<MediaSegmentInfo> mediaSegments(AuthenticatedServer authenticated, String itemId) {
        require(itemId, "itemId");
        try {
            ProtocolResponse response = send(authenticated.server().address(),
                    MediaBrowserRequests.mediaSegments(authenticated.session(),
                            authenticated.server().flavor(), itemId));
            return MediaBrowserResponseMapper.mediaSegments(response.body());
        } catch (MediaBrowserException exception) {
            return AndroidCollections.emptyList();
        }
    }

    public TrickplayInfo trickplayInfo(AuthenticatedServer authenticated, String itemId, int tileWidth) {
        require(itemId, "itemId");
        try {
            ProtocolResponse response = send(authenticated.server().address(),
                    MediaBrowserRequests.trickplayInfo(authenticated.session(),
                            authenticated.server().flavor(), itemId));
            TrickplayInfo info = MediaBrowserResponseMapper.trickplayInfo(
                    response.body(), itemId, tileWidth);
            String base = authenticated.server().address().value();
            String authorized = PlaybackUrlAuthorizer.withAccessToken(
                    base + info.imageUrl(), authenticated.session());
            return new TrickplayInfo(
                    info.tileWidth(), info.tileHeight(), info.tilesPerRow(),
                    info.tilesPerColumn(), info.tileCount(), info.tileIntervalTicks(),
                    authorized);
        } catch (MediaBrowserException exception) {
            return TrickplayInfo.empty();
        }
    }

    public void forget(AuthenticatedServer authenticated) {
        if (authenticated == null) {
            return;
        }
        sessions.revoke(SessionScope.from(authenticated.server(), authenticated.session()));
    }

    private ProtocolResponse send(MediaServerAddress address, ProtocolRequest request) {
        try {
            ProtocolResponse response = transport.send(address, request);
            if (response.successful()) {
                return response;
            }
            if (response.unauthorized()) {
                throw new MediaBrowserException("Authentication expired or token is invalid", response.statusCode());
            }
            throw new MediaBrowserException("Media server request failed with HTTP " + response.statusCode(), response.statusCode());
        } catch (IOException exception) {
            throw new MediaBrowserException("Media server request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MediaBrowserException("Media server request was interrupted", exception);
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
