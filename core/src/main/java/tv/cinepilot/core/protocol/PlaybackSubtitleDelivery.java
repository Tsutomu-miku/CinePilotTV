package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class PlaybackSubtitleDelivery {
    private PlaybackSubtitleDelivery() {
    }

    static boolean requiresServerDelivery(
            MediaSourceInfo source,
            PlaybackSelectionPreferences preferences
    ) {
        Integer selectedIndex = preferences.subtitleStreamIndex();
        if (selectedIndex == null || selectedIndex < 0) {
            return false;
        }
        Optional<MediaStreamInfo> selectedStream = selectedStream(source, preferences);
        if (selectedStream.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(preferences.alwaysBurnInSubtitleWhenTranscoding()) ||
                serverHlsSubtitle(selectedStream.get()) ||
                isBlank(selectedStream.get().deliveryUrl());
    }

    static String selectedDeliveryMethod(
            MediaSourceInfo source,
            Integer selectedIndex
    ) {
        return selectedStream(source, selectedIndex)
                .map(MediaStreamInfo::deliveryMethod)
                .orElse("");
    }

    static String selectedDeliveryUrl(
            MediaServerAddress serverAddress,
            MediaSourceInfo source,
            Integer selectedIndex
    ) {
        return selectedStream(source, selectedIndex)
                .map(MediaStreamInfo::deliveryUrl)
                .filter(value -> !isBlank(value))
                .map(value -> resolveUrl(serverAddress, value))
                .orElse("");
    }

    static String selectedCodec(MediaSourceInfo source, Integer selectedIndex) {
        return selectedStream(source, selectedIndex)
                .map(MediaStreamInfo::codec)
                .orElse("");
    }

    static String selectedLanguage(MediaSourceInfo source, Integer selectedIndex) {
        return selectedStream(source, selectedIndex)
                .map(MediaStreamInfo::language)
                .orElse("");
    }

    static String selectedDisplayTitle(MediaSourceInfo source, Integer selectedIndex) {
        return selectedStream(source, selectedIndex)
                .map(MediaStreamInfo::displayTitle)
                .orElse("");
    }

    static String transcodingUrl(
            String url,
            MediaSourceInfo source,
            PlaybackSelectionPreferences preferences
    ) {
        Map<String, String> overrides = new LinkedHashMap<>();
        if (preferences.startTimeTicks() > 0) {
            overrides.put("StartTimeTicks", Long.toString(preferences.startTimeTicks()));
        }
        if (preferences.audioStreamIndex() != null) {
            overrides.put("AudioStreamIndex", preferences.audioStreamIndex().toString());
        }
        Integer subtitleStreamIndex = preferences.subtitleStreamIndex();
        if (subtitleStreamIndex != null) {
            overrides.put("SubtitleStreamIndex", subtitleStreamIndex.toString());
            if (subtitleStreamIndex >= 0) {
                overrides.put("SubtitleMethod", subtitleMethod(source, preferences));
            }
        }
        if (preferences.maxAudioChannels() != null) {
            overrides.put("MaxAudioChannels", preferences.maxAudioChannels().toString());
        }
        if (preferences.maxWidth() > 0) {
            overrides.put("MaxWidth", Integer.toString(preferences.maxWidth()));
        }
        if (preferences.maxHeight() > 0) {
            overrides.put("MaxHeight", Integer.toString(preferences.maxHeight()));
        }
        return PlaybackUrlQuery.withOverrides(url, overrides);
    }

    private static Optional<MediaStreamInfo> selectedStream(
            MediaSourceInfo source,
            PlaybackSelectionPreferences preferences
    ) {
        Integer selectedIndex = preferences.subtitleStreamIndex();
        if (selectedIndex == null || selectedIndex < 0) {
            return Optional.empty();
        }
        return source.mediaStreams().stream()
                .filter(stream -> stream.type() == MediaStreamType.SUBTITLE)
                .filter(stream -> stream.index() == selectedIndex)
                .findFirst();
    }

    private static String subtitleMethod(
            MediaSourceInfo source,
            PlaybackSelectionPreferences preferences
    ) {
        if (Boolean.TRUE.equals(preferences.alwaysBurnInSubtitleWhenTranscoding())) {
            return "Encode";
        }
        String deliveryMethod = selectedDeliveryMethod(source, preferences.subtitleStreamIndex());
        if (equalsIgnoreCase(deliveryMethod, "Encode")) {
            return "Encode";
        }
        return "Hls";
    }

    private static Optional<MediaStreamInfo> selectedStream(MediaSourceInfo source, Integer selectedIndex) {
        if (selectedIndex == null || selectedIndex < 0) {
            return Optional.empty();
        }
        return source.mediaStreams().stream()
                .filter(stream -> stream.type() == MediaStreamType.SUBTITLE)
                .filter(stream -> stream.index() == selectedIndex)
                .findFirst();
    }

    private static boolean serverHlsSubtitle(MediaStreamInfo stream) {
        return equalsIgnoreCase(stream.deliveryMethod(), "Hls") ||
                equalsIgnoreCase(stream.deliveryMethod(), "Encode");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String resolveUrl(MediaServerAddress address, String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return address.resolvePath(value);
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }
}
