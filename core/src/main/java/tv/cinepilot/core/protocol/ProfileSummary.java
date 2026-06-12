package tv.cinepilot.core.protocol;

/**
 * Compact representation of a saved profile ready for the profile switcher UI.
 * We deliberately do not expose the access token here -- the UI should only
 * see identity metadata and never have access to authentication credentials.
 */
public record ProfileSummary(
        String userId,
        String userName,
        String userImageTag,
        boolean isActive
) {
    public ProfileSummary {
        require(userId, "userId");
        if (userName == null) userName = "";
        if (userImageTag == null) userImageTag = "";
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
