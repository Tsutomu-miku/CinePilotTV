package tv.cinepilot.core.protocol;

import java.io.IOException;

public interface HttpTransport {
    ProtocolResponse send(MediaServerAddress address, ProtocolRequest request) throws IOException, InterruptedException;
}

