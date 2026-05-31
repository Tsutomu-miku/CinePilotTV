package tv.cinepilot.core.tv;

import java.util.ArrayList;
import java.util.List;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.MediaBrowserClient;
import tv.cinepilot.core.protocol.MediaItemPage;
import tv.cinepilot.core.protocol.MediaItemSummary;

public final class HomeRowsLoader {
    private final MediaBrowserClient client;
    private final int rowLimit;

    public HomeRowsLoader(MediaBrowserClient client) {
        this(client, 24);
    }

    public HomeRowsLoader(MediaBrowserClient client, int rowLimit) {
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        if (rowLimit <= 0) {
            throw new IllegalArgumentException("rowLimit must be positive");
        }
        this.client = client;
        this.rowLimit = rowLimit;
    }

    public List<HomeRow> load(AuthenticatedServer authenticated) {
        List<HomeRow> rows = new ArrayList<>();
        addIfNotEmpty(rows, "views", "媒体库", client.userViews(authenticated).items());
        addIfNotEmpty(rows, "resume", "继续观看", client.resumeItems(authenticated, rowLimit).items());
        addIfNotEmpty(rows, "next-up", "下一集", client.nextUpItems(authenticated, rowLimit).items());

        MediaItemPage views = client.userViews(authenticated);
        for (MediaItemSummary view : views.items()) {
            if (view.id().isBlank()) {
                continue;
            }
            addIfNotEmpty(
                    rows,
                    "latest:" + view.id(),
                    view.name().isBlank() ? "最新" : "最新 - " + view.name(),
                    client.latestItems(authenticated, view.id(), rowLimit).items()
            );
        }
        return List.copyOf(rows);
    }

    private static void addIfNotEmpty(List<HomeRow> rows, String id, String title, List<MediaItemSummary> items) {
        if (items != null && !items.isEmpty()) {
            rows.add(new HomeRow(id, title, items));
        }
    }
}
