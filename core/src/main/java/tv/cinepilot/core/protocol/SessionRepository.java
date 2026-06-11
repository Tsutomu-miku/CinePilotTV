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
     * Remember which scope was last used to enter the home screen so the app can
     * restore the correct profile at cold start without asking the user to pick.
     */
    void markActive(SessionScope scope);

    Optional<SessionScope> activeScope(String serverId, ClientIdentity client);
}

