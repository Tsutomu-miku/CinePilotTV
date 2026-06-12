package tv.cinepilot.core.protocol;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {
    void save(SavedSession session);

    Optional<SavedSession> find(SessionScope scope);

    void revoke(SessionScope scope);

    /**
     * Return every saved session whose server id matches the given value and whose
     * client identity (name / device id / app version) matches the provided one.
     * Results are returned in the stable order dictated by the backing store so the
     * UI can present a consistent profile switcher list.
     */
    List<SavedSession> listForServer(String serverId, ClientIdentity client);

    /**
     * Return every saved session matching the given client identity, regardless of
     * server id. Useful for background workers that do not know which server the
     * user last interacted with.
     */
    List<SavedSession> listAll(ClientIdentity client);

    /**
     * Remember which scope was last used to enter the home screen so the app can
     * restore the correct profile at cold start without asking the user to pick.
     */
    void markActive(SessionScope scope);

    Optional<SessionScope> activeScope(String serverId, ClientIdentity client);
}

