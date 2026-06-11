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
}
