package tv.cinepilot.core.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemorySessionRepository implements SessionRepository {
    private final Map<SessionScope, SavedSession> sessions = new LinkedHashMap<>();
    private SessionScope active;

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

    @Override
    public List<SavedSession> listForServer(String serverId, ClientIdentity client) {
        List<SavedSession> out = new ArrayList<>();
        for (SavedSession saved : sessions.values()) {
            SessionScope scope = saved.scope();
            if (!scope.serverId().equals(serverId)) continue;
            if (!scope.clientName().equals(client.clientName())) continue;
            if (!scope.deviceId().equals(client.deviceId())) continue;
            if (!scope.appVersion().equals(client.version())) continue;
            out.add(saved);
        }
        return out;
    }

    @Override
    public List<SavedSession> listAll(ClientIdentity client) {
        List<SavedSession> out = new ArrayList<>();
        for (SavedSession saved : sessions.values()) {
            SessionScope scope = saved.scope();
            if (!scope.clientName().equals(client.clientName())) continue;
            if (!scope.deviceId().equals(client.deviceId())) continue;
            if (!scope.appVersion().equals(client.version())) continue;
            out.add(saved);
        }
        return out;
    }

    @Override
    public void markActive(SessionScope scope) {
        this.active = scope;
    }

    @Override
    public Optional<SessionScope> activeScope(String serverId, ClientIdentity client) {
        if (active == null) return Optional.empty();
        if (!active.serverId().equals(serverId)) return Optional.empty();
        if (!active.clientName().equals(client.clientName())) return Optional.empty();
        if (!active.deviceId().equals(client.deviceId())) return Optional.empty();
        if (!active.appVersion().equals(client.version())) return Optional.empty();
        return Optional.of(active);
    }
}

