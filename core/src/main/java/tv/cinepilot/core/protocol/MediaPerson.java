package tv.cinepilot.core.protocol;

public record MediaPerson(
        String id,
        String name,
        String role,
        String type,
        String primaryImageTag
) {
    public MediaPerson {
        if (id == null) {
            id = "";
        }
        if (name == null) {
            name = "";
        }
        if (role == null) {
            role = "";
        }
        if (type == null) {
            type = "";
        }
        if (primaryImageTag == null) {
            primaryImageTag = "";
        }
    }
}
