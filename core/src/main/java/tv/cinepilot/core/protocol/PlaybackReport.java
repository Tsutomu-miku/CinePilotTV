package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public record PlaybackReport(
        String itemId,
        String mediaSourceId,
        String playSessionId,
        PlayMethod playMethod,
        boolean canSeek,
        boolean paused,
        long positionTicks,
        Integer audioStreamIndex,
        Integer subtitleStreamIndex,
        Float playbackRate
) {
    public PlaybackReport {
        require(itemId, "itemId");
        require(mediaSourceId, "mediaSourceId");
        require(playSessionId, "playSessionId");
        if (playMethod == null) {
            throw new IllegalArgumentException("playMethod is required");
        }
        if (positionTicks < 0) {
            throw new IllegalArgumentException("positionTicks must be zero or greater");
        }
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("QueueableMediaTypes", new String[]{"Video"});
        payload.put("CanSeek", canSeek);
        payload.put("ItemId", itemId);
        payload.put("MediaSourceId", mediaSourceId);
        payload.put("PlaySessionId", playSessionId);
        payload.put("PlayMethod", playMethod.wireName());
        payload.put("IsPaused", paused);
        payload.put("IsMuted", false);
        payload.put("PositionTicks", positionTicks);
        if (audioStreamIndex != null) {
            payload.put("AudioStreamIndex", audioStreamIndex);
        }
        if (subtitleStreamIndex != null) {
            payload.put("SubtitleStreamIndex", subtitleStreamIndex);
        }
        if (playbackRate != null) {
            payload.put("PlaybackRate", playbackRate);
        }
        return payload;
    }

    public Map<String, Object> toProgressPayload(PlaybackEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        Map<String, Object> payload = toPayload();
        payload.put("EventName", event.wireName());
        return payload;
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

