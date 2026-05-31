package tv.cinepilot.core.protocol;

import java.util.Optional;

public interface SessionRepository {
    void save(SavedSession session);

    Optional<SavedSession> find(SessionScope scope);

    void revoke(SessionScope scope);
}

