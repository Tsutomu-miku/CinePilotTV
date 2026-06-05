package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public record PlaybackInfoOptions(
        Long maxStreamingBitrate,
        Long startTimeTicks,
        Integer audioStreamIndex,
        Integer subtitleStreamIndex,
        Integer maxAudioChannels,
        String mediaSourceId,
        String liveStreamId,
        Boolean autoOpenLiveStream,
        Boolean enableDirectPlay,
        Boolean enableDirectStream,
        Boolean enableTranscoding,
        Boolean allowVideoStreamCopy,
        Boolean allowAudioStreamCopy,
        Boolean alwaysBurnInSubtitleWhenTranscoding,
        PlaybackDeviceProfile deviceProfile
) {
    public static PlaybackInfoOptions defaults() {
        return new Builder().build();
    }

    void applyTo(ProtocolRequest.Builder builder) {
        query(builder, "MaxStreamingBitrate", maxStreamingBitrate);
        query(builder, "StartTimeTicks", startTimeTicks);
        query(builder, "AudioStreamIndex", audioStreamIndex);
        query(builder, "SubtitleStreamIndex", subtitleStreamIndex);
        query(builder, "MaxAudioChannels", maxAudioChannels);
        query(builder, "MediaSourceId", mediaSourceId);
        query(builder, "LiveStreamId", liveStreamId);
        query(builder, "AutoOpenLiveStream", autoOpenLiveStream);
        query(builder, "EnableDirectPlay", enableDirectPlay);
        query(builder, "EnableDirectStream", enableDirectStream);
        query(builder, "EnableTranscoding", enableTranscoding);
        query(builder, "AllowVideoStreamCopy", allowVideoStreamCopy);
        query(builder, "AllowAudioStreamCopy", allowAudioStreamCopy);
        query(builder, "AlwaysBurnInSubtitleWhenTranscoding", alwaysBurnInSubtitleWhenTranscoding);
    }

    boolean requiresPostBody() {
        return deviceProfile != null && deviceProfile.hasCodecHints();
    }

    String bodyJson(String userId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("UserId", userId);
        body.put("MaxStreamingBitrate", maxStreamingBitrate);
        body.put("StartTimeTicks", startTimeTicks);
        body.put("AudioStreamIndex", audioStreamIndex);
        body.put("SubtitleStreamIndex", subtitleStreamIndex);
        body.put("MaxAudioChannels", maxAudioChannels);
        body.put("MediaSourceId", mediaSourceId);
        body.put("LiveStreamId", liveStreamId);
        body.put("AutoOpenLiveStream", autoOpenLiveStream);
        body.put("EnableDirectPlay", enableDirectPlay);
        body.put("EnableDirectStream", enableDirectStream);
        body.put("EnableTranscoding", enableTranscoding);
        body.put("AllowVideoStreamCopy", allowVideoStreamCopy);
        body.put("AllowAudioStreamCopy", allowAudioStreamCopy);
        body.put("AlwaysBurnInSubtitleWhenTranscoding", alwaysBurnInSubtitleWhenTranscoding);
        if (requiresPostBody()) {
            body.put("DeviceProfile", deviceProfile.toPayload());
        }
        return JsonPayload.object(body);
    }

    private static void query(ProtocolRequest.Builder builder, String name, Object value) {
        if (value != null) {
            builder.query(name, String.valueOf(value));
        }
    }

    public static final class Builder {
        private Long maxStreamingBitrate;
        private Long startTimeTicks;
        private Integer audioStreamIndex;
        private Integer subtitleStreamIndex;
        private Integer maxAudioChannels;
        private String mediaSourceId;
        private String liveStreamId;
        private Boolean autoOpenLiveStream;
        private Boolean enableDirectPlay = true;
        private Boolean enableDirectStream = true;
        private Boolean enableTranscoding = true;
        private Boolean allowVideoStreamCopy = true;
        private Boolean allowAudioStreamCopy = true;
        private Boolean alwaysBurnInSubtitleWhenTranscoding;
        private PlaybackDeviceProfile deviceProfile;

        public Builder maxStreamingBitrate(long value) {
            maxStreamingBitrate = value;
            return this;
        }

        public Builder startTimeTicks(long value) {
            startTimeTicks = value;
            return this;
        }

        public Builder audioStreamIndex(int value) {
            audioStreamIndex = value;
            return this;
        }

        public Builder subtitleStreamIndex(int value) {
            subtitleStreamIndex = value;
            return this;
        }

        public Builder maxAudioChannels(int value) {
            maxAudioChannels = value;
            return this;
        }

        public Builder mediaSourceId(String value) {
            mediaSourceId = value;
            return this;
        }

        public Builder liveStreamId(String value) {
            liveStreamId = value;
            return this;
        }

        public Builder autoOpenLiveStream(boolean value) {
            autoOpenLiveStream = value;
            return this;
        }

        public Builder enableDirectPlay(boolean value) {
            enableDirectPlay = value;
            return this;
        }

        public Builder enableDirectStream(boolean value) {
            enableDirectStream = value;
            return this;
        }

        public Builder enableTranscoding(boolean value) {
            enableTranscoding = value;
            return this;
        }

        public Builder allowVideoStreamCopy(boolean value) {
            allowVideoStreamCopy = value;
            return this;
        }

        public Builder allowAudioStreamCopy(boolean value) {
            allowAudioStreamCopy = value;
            return this;
        }

        public Builder alwaysBurnInSubtitleWhenTranscoding(boolean value) {
            alwaysBurnInSubtitleWhenTranscoding = value;
            return this;
        }

        public Builder deviceProfile(PlaybackDeviceProfile value) {
            deviceProfile = value;
            return this;
        }

        public PlaybackInfoOptions build() {
            return new PlaybackInfoOptions(
                    maxStreamingBitrate,
                    startTimeTicks,
                    audioStreamIndex,
                    subtitleStreamIndex,
                    maxAudioChannels,
                    mediaSourceId,
                    liveStreamId,
                    autoOpenLiveStream,
                    enableDirectPlay,
                    enableDirectStream,
                    enableTranscoding,
                    allowVideoStreamCopy,
                    allowAudioStreamCopy,
                    alwaysBurnInSubtitleWhenTranscoding,
                    deviceProfile
            );
        }
    }
}
