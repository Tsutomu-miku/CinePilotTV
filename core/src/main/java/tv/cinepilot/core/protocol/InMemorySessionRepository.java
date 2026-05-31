package tv.cinepilot.core.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemorySessionRepository implements SessionRepository {
    private final Map<SessionScope, SavedSession> sessions = new LinkedHashMap<>();

    @Override
    public void save(SavedSession session) {
        sessions.put(session.scope(), session);
    }

    @Override
    public Optional<SavedSession> find(SessionScope scope) {
        return Optional.ofNullable(sessions.get(scope));
    }

    @Override
    public void revoke(SessionScope scope) {
        sessions.remove(scope);
    }
}

