package tv.cinepilot.core.protocol;

import java.util.Optional;

public final class PlaybackSourceSelector {
    private PlaybackSourceSelector() {
    }

    public static Optional<PlayableMedia> select(
            MediaServerAddress serverAddress,
            AuthSession session,
            ServerFlavor flavor,
            PlaybackInfo playbackInfo,
            PlaybackSelectionPreferences preferences
    ) {
        PlaybackSelectionPreferences safePreferences = preferences == null
                ? PlaybackSelectionPreferences.defaults()
                : preferences;

        var sources = candidateSources(playbackInfo, safePreferences);

        Optional<PlayableMedia> directPlay = sources.stream()
                .filter(MediaSourceInfo::supportsDirectPlay)
                .filter(source -> hasValue(source.directStreamUrl()))
                .findFirst()
                .map(source -> playableFromUrl(
                        serverAddress,
                        playbackInfo,
                        source,
                        PlayMethod.DIRECT_PLAY,
                        source.directStreamUrl(),
                        safePreferences
                ));
        if (directPlay.isPresent()) {
            return directPlay;
        }

        Optional<PlayableMedia> staticDirectPlay = sources.stream()
                .filter(MediaSourceInfo::supportsDirectPlay)
                .findFirst()
                .map(source -> playableFromStaticStreamRequest(
                        serverAddress,
                        session,
                        flavor,
                        playbackInfo,
                        source,
                        safePreferences
                ));
        if (staticDirectPlay.isPresent()) {
            return staticDirectPlay;
        }

        Optional<PlayableMedia> directStream = sources.stream()
                .filter(MediaSourceInfo::supportsDirectStream)
                .filter(source -> hasValue(source.directStreamUrl()))
                .findFirst()
                .map(source -> playableFromUrl(
                        serverAddress,
                        playbackInfo,
                        source,
                        PlayMethod.DIRECT_STREAM,
                        source.directStreamUrl(),
                        safePreferences
                ));
        if (directStream.isPresent()) {
            return directStream;
        }

        Optional<PlayableMedia> transcodeUrl = sources.stream()
                .filter(MediaSourceInfo::supportsTranscoding)
                .filter(source -> hasValue(source.transcodingUrl()))
                .findFirst()
                .map(source -> playableFromUrl(
                        serverAddress,
                        playbackInfo,
                        source,
                        PlayMethod.TRANSCODE,
                        source.transcodingUrl(),
                        safePreferences
                ));
        if (transcodeUrl.isPresent()) {
            return transcodeUrl;
        }

        return sources.stream()
                .filter(MediaSourceInfo::supportsTranscoding)
                .findFirst()
                .map(source -> playableFromHlsRequest(session, flavor, playbackInfo, source, safePreferences));
    }

    private static java.util.List<MediaSourceInfo> candidateSources(
            PlaybackInfo playbackInfo,
            PlaybackSelectionPreferences preferences
    ) {
        if (!hasValue(preferences.mediaSourceId())) {
            return playbackInfo.mediaSources();
        }
        return playbackInfo.mediaSources().stream()
                .filter(source -> preferences.mediaSourceId().equals(source.id()))
                .toList();
    }

    private static PlayableMedia playableFromUrl(
            MediaServerAddress serverAddress,
            PlaybackInfo playbackInfo,
            MediaSourceInfo source,
            PlayMethod method,
            String url,
            PlaybackSelectionPreferences preferences
    ) {
        return new PlayableMedia(
                playbackInfo.itemId(),
                source.id(),
                playbackInfo.playSessionId(),
                method,
                resolveUrl(serverAddress, url),
                null,
                selectedAudioStreamIndex(source, preferences),
                selectedSubtitleStreamIndex(source, preferences),
                selectedSubtitleDeliveryUrl(serverAddress, source, preferences),
                selectedSubtitleCodec(source, preferences),
                selectedSubtitleLanguage(source, preferences),
                selectedSubtitleDisplayTitle(source, preferences),
                preferences.playbackRate()
        );
    }

    private static PlayableMedia playableFromStaticStreamRequest(
            MediaServerAddress serverAddress,
            AuthSession session,
            ServerFlavor flavor,
            PlaybackInfo playbackInfo,
            MediaSourceInfo source,
            PlaybackSelectionPreferences preferences
    ) {
        return new PlayableMedia(
                playbackInfo.itemId(),
                source.id(),
                playbackInfo.playSessionId(),
                PlayMethod.DIRECT_PLAY,
                null,
                MediaBrowserRequests.staticVideoStream(
                        session,
                        flavor,
                        playbackInfo.itemId(),
                        source.id(),
                        playbackInfo.playSessionId()
                ),
                selectedAudioStreamIndex(source, preferences),
                selectedSubtitleStreamIndex(source, preferences),
                selectedSubtitleDeliveryUrl(serverAddress, source, preferences),
                selectedSubtitleCodec(source, preferences),
                selectedSubtitleLanguage(source, preferences),
                selectedSubtitleDisplayTitle(source, preferences),
                preferences.playbackRate()
        );
    }

    private static PlayableMedia playableFromHlsRequest(
            AuthSession session,
            ServerFlavor flavor,
            PlaybackInfo playbackInfo,
            MediaSourceInfo source,
            PlaybackSelectionPreferences preferences
    ) {
        HlsStreamOptions.Builder builder = HlsStreamOptions.builder(playbackInfo.itemId(), source.id())
                .playSessionId(playbackInfo.playSessionId())
                .startTimeTicks(preferences.startTimeTicks());
        if (preferences.audioStreamIndex() != null) {
            builder.audioStreamIndex(preferences.audioStreamIndex());
        }
        if (preferences.subtitleStreamIndex() != null) {
            builder.subtitleStreamIndex(preferences.subtitleStreamIndex());
            if (preferences.subtitleStreamIndex() >= 0) {
                builder.subtitleMethod("Hls");
            }
        }
        if (preferences.maxAudioChannels() != null) {
            builder.maxAudioChannels(preferences.maxAudioChannels());
        }
        if (preferences.maxWidth() > 0) {
            builder.maxWidth(preferences.maxWidth());
        }
        if (preferences.maxHeight() > 0) {
            builder.maxHeight(preferences.maxHeight());
        }
        if (preferences.maxBitRate() > 0) {
            builder.videoBitRate(preferences.maxBitRate());
        }

        return new PlayableMedia(
                playbackInfo.itemId(),
                source.id(),
                playbackInfo.playSessionId(),
                PlayMethod.TRANSCODE,
                null,
                MediaBrowserRequests.hlsStream(session, flavor, builder.build()),
                selectedAudioStreamIndex(source, preferences),
                selectedSubtitleStreamIndex(source, preferences),
                "",
                "",
                "",
                "",
                preferences.playbackRate()
        );
    }

    private static Integer selectedAudioStreamIndex(MediaSourceInfo source, PlaybackSelectionPreferences preferences) {
        if (preferences.audioStreamIndex() != null) {
            return preferences.audioStreamIndex();
        }
        return defaultStreamIndex(source, MediaStreamType.AUDIO, true);
    }

    private static Integer selectedSubtitleStreamIndex(MediaSourceInfo source, PlaybackSelectionPreferences preferences) {
        if (preferences.subtitleStreamIndex() != null) {
            return preferences.subtitleStreamIndex();
        }
        return defaultStreamIndex(source, MediaStreamType.SUBTITLE, false);
    }

    private static String selectedSubtitleDeliveryUrl(
            MediaServerAddress serverAddress,
            MediaSourceInfo source,
            PlaybackSelectionPreferences preferences
    ) {
        return selectedSubtitleStream(source, preferences)
                .map(MediaStreamInfo::deliveryUrl)
                .filter(PlaybackSourceSelector::hasValue)
                .map(url -> resolveUrl(serverAddress, url))
                .orElse("");
    }

    private static String selectedSubtitleCodec(MediaSourceInfo source, PlaybackSelectionPreferences preferences) {
        return selectedSubtitleStream(source, preferences)
                .map(MediaStreamInfo::codec)
                .orElse("");
    }

    private static String selectedSubtitleLanguage(MediaSourceInfo source, PlaybackSelectionPreferences preferences) {
        return selectedSubtitleStream(source, preferences)
                .map(MediaStreamInfo::language)
                .orElse("");
    }

    private static String selectedSubtitleDisplayTitle(MediaSourceInfo source, PlaybackSelectionPreferences preferences) {
        return selectedSubtitleStream(source, preferences)
                .map(MediaStreamInfo::displayTitle)
                .orElse("");
    }

    private static Optional<MediaStreamInfo> selectedSubtitleStream(
            MediaSourceInfo source,
            PlaybackSelectionPreferences preferences
    ) {
        Integer selectedIndex = selectedSubtitleStreamIndex(source, preferences);
        if (selectedIndex == null || selectedIndex < 0) {
            return Optional.empty();
        }
        return source.mediaStreams().stream()
                .filter(stream -> stream.type() == MediaStreamType.SUBTITLE)
                .filter(stream -> stream.index() == selectedIndex)
                .findFirst();
    }

    private static Integer defaultStreamIndex(MediaSourceInfo source, MediaStreamType type, boolean fallbackToFirst) {
        Optional<MediaStreamInfo> defaultStream = source.mediaStreams().stream()
                .filter(stream -> stream.type() == type)
                .filter(MediaStreamInfo::defaultStream)
                .findFirst();
        if (defaultStream.isPresent()) {
            return defaultStream.get().index();
        }
        if (!fallbackToFirst) {
            return null;
        }
        return source.mediaStreams().stream()
                .filter(stream -> stream.type() == type)
                .findFirst()
                .map(MediaStreamInfo::index)
                .orElse(null);
    }

    private static String resolveUrl(MediaServerAddress address, String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return address.resolvePath(url);
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
