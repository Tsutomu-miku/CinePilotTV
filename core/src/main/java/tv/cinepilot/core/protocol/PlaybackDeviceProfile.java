package tv.cinepilot.core.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tv.cinepilot.core.AndroidCollections;

public record PlaybackDeviceProfile(
        String name,
        List<String> videoCodecs,
        List<String> audioCodecs,
        List<String> subtitleFormats
) {
    public PlaybackDeviceProfile {
        name = (name == null || name.isBlank()) ? "CinePilot TV" : name;
        videoCodecs = AndroidCollections.listCopy(normalized(videoCodecs));
        audioCodecs = AndroidCollections.listCopy(normalized(audioCodecs));
        subtitleFormats = AndroidCollections.listCopy(normalized(subtitleFormats));
    }

    public boolean hasCodecHints() {
        return !videoCodecs.isEmpty() || !audioCodecs.isEmpty();
    }

    Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("Name", name);
        payload.put("SupportedMediaTypes", "Video,Audio");
        payload.put("DirectPlayProfiles", directPlayProfiles());
        payload.put("TranscodingProfiles", transcodingProfiles());
        payload.put("SubtitleProfiles", subtitleProfiles());
        return payload;
    }

    private List<Map<String, Object>> directPlayProfiles() {
        List<Map<String, Object>> profiles = new ArrayList<>();
        if (!videoCodecs.isEmpty()) {
            Map<String, Object> video = new LinkedHashMap<>();
            video.put("Container", "mp4,m4v,mov,mkv,webm,ts,m2ts");
            video.put("Type", "Video");
            video.put("VideoCodec", String.join(",", videoCodecs));
            if (!audioCodecs.isEmpty()) {
                video.put("AudioCodec", String.join(",", audioCodecs));
            }
            profiles.add(video);
        }
        return profiles;
    }

    private List<Map<String, Object>> transcodingProfiles() {
        List<Map<String, Object>> profiles = new ArrayList<>();
        Map<String, Object> hls = new LinkedHashMap<>();
        hls.put("Container", "ts");
        hls.put("Type", "Video");
        hls.put("VideoCodec", "h264");
        hls.put("AudioCodec", "aac");
        hls.put("Protocol", "hls");
        hls.put("Context", "Streaming");
        hls.put("ManifestSubtitles", "hls");
        profiles.add(hls);
        return profiles;
    }

    private List<Map<String, Object>> subtitleProfiles() {
        List<Map<String, Object>> profiles = new ArrayList<>();
        for (String format : subtitleFormats) {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("Format", format);
            profile.put("Method", "External");
            profiles.add(profile);
        }
        Map<String, Object> hls = new LinkedHashMap<>();
        hls.put("Format", "srt");
        hls.put("Method", "Hls");
        profiles.add(hls);
        return profiles;
    }

    private static List<String> normalized(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank() && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
