package tv.cinepilot.core.protocol;

public record QuickConnectSession(
        String code,
        String secret,
        boolean authenticated
) {
    public QuickConnectSession {
        if (code == null) {
            code = "";
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("secret is required");
        }
    }
}
