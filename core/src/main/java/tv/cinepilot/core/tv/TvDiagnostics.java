package tv.cinepilot.core.tv;

import java.util.regex.Pattern;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.MediaItemSummary;
import tv.cinepilot.core.protocol.PlayableMedia;

public final class TvDiagnostics {
    private TvDiagnostics() {
    }

    public static String describe(TvAppState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("route=").append(state.route()).append('\n');
        builder.append("status=").append(state.status()).append('\n');
        if (!state.errorMessage().isBlank()) {
            builder.append("errorMessage=").append(redactSensitive(state.errorMessage())).append('\n');
        }

        AuthenticatedServer authenticated = state.authenticated();
        if (authenticated != null) {
            builder.append("serverId=").append(authenticated.server().serverId()).append('\n');
            builder.append("serverUrl=").append(authenticated.server().address().value()).append('\n');
            builder.append("serverFlavor=").append(authenticated.server().flavor()).append('\n');
            builder.append("userId=").append(authenticated.session().userId()).append('\n');
        }

        MediaItemSummary selected = state.selectedItem();
        if (selected != null) {
            builder.append("itemId=").append(selected.id()).append('\n');
            builder.append("itemType=").append(selected.type()).append('\n');
            builder.append("resumeTicks=").append(selected.userData().playbackPositionTicks()).append('\n');
        }

        PlayableMedia playable = state.playableMedia();
        if (playable != null) {
            builder.append("mediaSourceId=").append(playable.mediaSourceId()).append('\n');
            builder.append("playSessionId=").append(playable.playSessionId()).append('\n');
            builder.append("playMethod=").append(playable.playMethod()).append('\n');
            builder.append("hasReadyUrl=").append(playable.hasReadyUrl()).append('\n');
            builder.append("hasRequestUrl=").append(playable.request() != null).append('\n');
            builder.append("audioStreamIndex=").append(playable.audioStreamIndex()).append('\n');
            builder.append("subtitleStreamIndex=").append(playable.subtitleStreamIndex()).append('\n');
        }

        if (state.focus() != null) {
            builder.append("focus=").append(state.focus().rowId()).append('/').append(state.focus().itemId()).append('\n');
        }
        return builder.toString();
    }

    private static String redactSensitive(String value) {
        String redacted = API_KEY_QUERY.matcher(value).replaceAll("$1<redacted>");
        redacted = TOKEN_QUERY.matcher(redacted).replaceAll("$1<redacted>");
        redacted = AUTH_TOKEN.matcher(redacted).replaceAll("$1<redacted>\"");
        redacted = HEADER_TOKEN.matcher(redacted).replaceAll("$1<redacted>");
        return redacted;
    }

    private static final Pattern API_KEY_QUERY = Pattern.compile("(?i)(api_key=)[^&#\\s]+");
    private static final Pattern TOKEN_QUERY = Pattern.compile("(?i)((?:access[_-]?token|auth[_-]?token)=)[^&#\\s]+");
    private static final Pattern AUTH_TOKEN = Pattern.compile("(?i)(To" + "ken=\")[^\"]+\"");
    private static final Pattern HEADER_TOKEN = Pattern.compile("(?i)((?:X-Emby-Token|X-MediaBrowser-Token)[:=]\\s*)[^,\\s]+");
}
