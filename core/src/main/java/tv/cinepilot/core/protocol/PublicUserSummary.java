package tv.cinepilot.core.protocol;

public record PublicUserSummary(
        String id,
        String name,
        boolean passwordRequired
) {
    public PublicUserSummary {
        require(id, "id");
        if (name == null) {
            name = "";
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
