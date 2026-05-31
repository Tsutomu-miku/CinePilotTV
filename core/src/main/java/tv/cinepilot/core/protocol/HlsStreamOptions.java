package tv.cinepilot.core.protocol;

public record HlsStreamOptions(
        String itemId,
        String mediaSourceId,
        String playSessionId,
        String container,
        Long startTimeTicks,
        Integer audioStreamIndex,
        Integer subtitleStreamIndex,
        Integer videoStreamIndex,
        Integer maxAudioChannels,
        Integer maxWidth,
        Integer maxHeight,
        Integer videoBitRate,
        String videoCodec,
        String audioCodec,
        String subtitleMethod,
        Boolean staticStream,
        Boolean enableAutoStreamCopy,
        Boolean allowVideoStreamCopy,
        Boolean allowAudioStreamCopy
) {
    public HlsStreamOptions {
        require(itemId, "itemId");
        require(mediaSourceId, "mediaSourceId");
        if (container == null || container.isBlank()) {
            container = "ts";
        }
    }

    public static Builder builder(String itemId, String mediaSourceId) {
        return new Builder(itemId, mediaSourceId);
    }

    void applyTo(ProtocolRequest.Builder builder, ClientIdentity client) {
        query(builder, "MediaSourceId", mediaSourceId);
        query(builder, "DeviceId", client.deviceId());
        query(builder, "PlaySessionId", playSessionId);
        query(builder, "Container", container);
        query(builder, "StartTimeTicks", startTimeTicks);
        query(builder, "AudioStreamIndex", audioStreamIndex);
        query(builder, "SubtitleStreamIndex", subtitleStreamIndex);
        query(builder, "VideoStreamIndex", videoStreamIndex);
        query(builder, "MaxAudioChannels", maxAudioChannels);
        query(builder, "MaxWidth", maxWidth);
        query(builder, "MaxHeight", maxHeight);
        query(builder, "VideoBitRate", videoBitRate);
        query(builder, "VideoCodec", videoCodec);
        query(builder, "AudioCodec", audioCodec);
        query(builder, "SubtitleMethod", subtitleMethod);
        query(builder, "Static", staticStream);
        query(builder, "EnableAutoStreamCopy", enableAutoStreamCopy);
        query(builder, "AllowVideoStreamCopy", allowVideoStreamCopy);
        query(builder, "AllowAudioStreamCopy", allowAudioStreamCopy);
    }

    private static void query(ProtocolRequest.Builder builder, String name, Object value) {
        if (value != null) {
            builder.query(name, String.valueOf(value));
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    public static final class Builder {
        private final String itemId;
        private final String mediaSourceId;
        private String playSessionId;
        private String container = "ts";
        private Long startTimeTicks;
        private Integer audioStreamIndex;
        private Integer subtitleStreamIndex;
        private Integer videoStreamIndex;
        private Integer maxAudioChannels;
        private Integer maxWidth;
        private Integer maxHeight;
        private Integer videoBitRate;
        private String videoCodec;
        private String audioCodec;
        private String subtitleMethod;
        private Boolean staticStream;
        private Boolean enableAutoStreamCopy = true;
        private Boolean allowVideoStreamCopy = true;
        private Boolean allowAudioStreamCopy = true;

        private Builder(String itemId, String mediaSourceId) {
            this.itemId = itemId;
            this.mediaSourceId = mediaSourceId;
        }

        public Builder playSessionId(String value) {
            playSessionId = value;
            return this;
        }

        public Builder container(String value) {
            container = value;
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

        public Builder videoStreamIndex(int value) {
            videoStreamIndex = value;
            return this;
        }

        public Builder maxAudioChannels(int value) {
            maxAudioChannels = value;
            return this;
        }

        public Builder maxWidth(int value) {
            maxWidth = value;
            return this;
        }

        public Builder maxHeight(int value) {
            maxHeight = value;
            return this;
        }

        public Builder videoBitRate(int value) {
            videoBitRate = value;
            return this;
        }

        public Builder videoCodec(String value) {
            videoCodec = value;
            return this;
        }

        public Builder audioCodec(String value) {
            audioCodec = value;
            return this;
        }

        public Builder subtitleMethod(String value) {
            subtitleMethod = value;
            return this;
        }

        public Builder staticStream(boolean value) {
            staticStream = value;
            return this;
        }

        public Builder enableAutoStreamCopy(boolean value) {
            enableAutoStreamCopy = value;
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

        public HlsStreamOptions build() {
            return new HlsStreamOptions(
                    itemId,
                    mediaSourceId,
                    playSessionId,
                    container,
                    startTimeTicks,
                    audioStreamIndex,
                    subtitleStreamIndex,
                    videoStreamIndex,
                    maxAudioChannels,
                    maxWidth,
                    maxHeight,
                    videoBitRate,
                    videoCodec,
                    audioCodec,
                    subtitleMethod,
                    staticStream,
                    enableAutoStreamCopy,
                    allowVideoStreamCopy,
                    allowAudioStreamCopy
            );
        }
    }
}

