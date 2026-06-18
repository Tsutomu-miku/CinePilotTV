package tv.cinepilot.plugin.spi;

/**
 * Base interface every CinePilot plugin must implement.
 *
 * <p>Plugins are discovered via {@link java.util.ServiceLoader}, so each
 * jar must declare its concrete implementation class in a resource file
 * named {@code META-INF/services/tv.cinepilot.plugin.spi.CinePilotPlugin}.
 *
 * <p>A single plugin may implement any combination of the marker sub-types
 * ({@link UserDataSyncPlugin}, {@link PlaybackSyncPlugin}) -- the host
 * casts to whichever interfaces the class declares and dispatches hooks
 * accordingly.
 *
 * <p>Implementations MUST have a public no-arg constructor, as ServiceLoader
 * requires one. All expensive work (token refresh, HTTP client setup)
 * should be deferred to {@link #onLoaded(PluginSettingsStore)} so that
 * plugin discovery stays lightweight.
 */
public interface CinePilotPlugin {
    PluginDescriptor descriptor();

    /**
     * Invoked once by the plugin host after a plugin has been discovered
     * and its per-plugin settings store has been allocated. Plugins that
     * need upstream auth can read stored credentials here.
     */
    default void onLoaded(PluginSettingsStore store) { }

    /**
     * Invoked when the user explicitly disables the plugin from settings,
     * or once during clean shutdown. Plugins should release HTTP clients
     * and cancel in-flight work.
     */
    default void onUnloaded() { }

    /**
     * @return the current plugin status -- used by the host to render the
     *     plugin settings screen (displays "未授权" if auth is required,
     *     "已连接" if connected, etc.).
     */
    default PluginStatus status(PluginSettingsStore store) { return PluginStatus.READY; }

    /**
     * Verify a user-provided credential against the plugin's upstream.
     *
     * <p>For tokens this typically means issuing a lightweight {@code /me}
     * call. The returned {@link AuthVerificationResult} tells the host
     * whether to persist the credential via
     * {@link #saveAuth(PluginSettingsStore, String)} and what user-facing
     * message to display.
     *
     * <p>Implementations MUST NOT mutate the settings store from inside
     * this method: persistence is handled by the host through
     * {@link #saveAuth(PluginSettingsStore, String)} only on success.
     *
     * @param store the plugin's private settings store
     * @param credential the raw credential (PAT, OAuth code, ...) provided
     *     by the user in the plugin settings screen
     * @return result -- see {@link AuthVerificationResult}
     */
    default AuthVerificationResult verifyAuth(PluginSettingsStore store, String credential) {
        return AuthVerificationResult.success("");
    }

    /**
     * Persist a credential that has passed
     * {@link #verifyAuth(PluginSettingsStore, String)} to the settings
     * store.
     *
     * <p>The host calls this method only after
     * {@link #verifyAuth(PluginSettingsStore, String)} returns success.
     * Plugins can additionally store any derived metadata (user id,
     * scopes, ...) alongside the credential.
     *
     * <p>The default implementation writes the credential under the
     * conventional {@code "token"} key. Plugins that use a different key
     * scheme or need to erase stale data first should override this.
     */
    default void saveAuth(PluginSettingsStore store, String credential) {
        store.putString("token", credential);
    }

    /**
     * Clear any credentials or cached auth state for this plugin.
     *
     * <p>The host calls this method when the user explicitly presses
     * "断开连接" in the plugin settings screen.
     */
    default void clearAuth(PluginSettingsStore store) {
        store.remove("token");
        store.remove("lastError");
    }
}
