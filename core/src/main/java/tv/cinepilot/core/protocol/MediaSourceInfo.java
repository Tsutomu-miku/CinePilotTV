package tv.cinepilot.core.protocol;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;

public record MediaSourceInfo(
        String id,
        String container,
        String directStreamUrl,
        String transcodingUrl,
        boolean supportsDirectPlay,
        boolean supportsDirectStream,
        boolean supportsTranscoding,
        String name,
        String path,
        long sizeBytes,
        long bitRate,
        List<MediaStreamInfo> mediaStreams
) {
    public MediaSourceInfo {
        require(id, "id");
        if (container == null) {
            container = "";
        }
        if (name == null) {
            name = "";
        }
        if (path == null) {
            path = "";
        }
        if (sizeBytes < 0) {
            sizeBytes = 0;
        }
        if (bitRate < 0) {
            bitRate = 0;
        }
        mediaStreams = AndroidCollections.listCopy(mediaStreams);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    public static final class Builder {
        private final String id;
        private String container = "";
        private String directStreamUrl;
        private String transcodingUrl;
        private boolean supportsDirectPlay;
        private boolean supportsDirectStream;
        private boolean supportsTranscoding;
        private String name = "";
        private String path = "";
        private long sizeBytes;
        private long bitRate;
        private List<MediaStreamInfo> mediaStreams = AndroidCollections.emptyList();

        private Builder(String id) {
            this.id = id;
        }

        public Builder container(String value) {
            container = value;
            return this;
        }

        public Builder directStreamUrl(String value) {
            directStreamUrl = value;
            return this;
        }

        public Builder transcodingUrl(String value) {
            transcodingUrl = value;
            return this;
        }

        public Builder supportsDirectPlay(boolean value) {
            supportsDirectPlay = value;
            return this;
        }

        public Builder supportsDirectStream(boolean value) {
            supportsDirectStream = value;
            return this;
        }

        public Builder supportsTranscoding(boolean value) {
            supportsTranscoding = value;
            return this;
        }

        public Builder name(String value) {
            name = value;
            return this;
        }

        public Builder path(String value) {
            path = value;
            return this;
        }

        public Builder bitRate(long value) {
            bitRate = value;
            return this;
        }

        public Builder sizeBytes(long value) {
            sizeBytes = value;
            return this;
        }

        public Builder mediaStreams(List<MediaStreamInfo> value) {
            mediaStreams = value;
            return this;
        }

        public MediaSourceInfo build() {
            return new MediaSourceInfo(
                    id,
                    container,
                    directStreamUrl,
                    transcodingUrl,
                    supportsDirectPlay,
                    supportsDirectStream,
                    supportsTranscoding,
                    name,
                    path,
                    sizeBytes,
                    bitRate,
                    mediaStreams
            );
        }
    }
}
