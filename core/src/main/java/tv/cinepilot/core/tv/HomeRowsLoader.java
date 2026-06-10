package tv.cinepilot.core.tv;

import java.util.ArrayList;
import java.util.List;
import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.protocol.AuthenticatedServer;
import tv.cinepilot.core.protocol.ItemQuery;
import tv.cinepilot.core.protocol.MediaBrowseFilters;
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
        return load(authenticated, MediaBrowseFilters.EMPTY);
    }

    public List<HomeRow> load(AuthenticatedServer authenticated, MediaBrowseFilters filters) {
        MediaBrowseFilters safeFilters = filters == null ? MediaBrowseFilters.EMPTY : filters;
        List<HomeRow> rows = new ArrayList<>();
        addIfNotEmpty(rows, "views", "媒体库", client.userViews(authenticated).items());
        if (!safeFilters.isStrict()) {
            addIfNotEmpty(rows, "resume", "继续观看", client.resumeItems(authenticated, rowLimit).items());
            addIfNotEmpty(rows, "next-up", "下一集", client.nextUpItems(authenticated, rowLimit).items());
            addIfNotEmpty(rows, "favorites", "收藏夹", client.favoriteItems(authenticated, rowLimit).items());
        }
        addIfNotEmpty(rows, "collections", "精选合集", client.collections(authenticated, rowLimit).items());

        MediaItemPage views = client.userViews(authenticated);
        for (MediaItemSummary view : views.items()) {
            if (view.id().isBlank()) {
                continue;
            }
            MediaItemPage page;
            String rowId;
            String title;
            if (safeFilters.isEmpty()) {
                page = client.latestItems(authenticated, view.id(), rowLimit);
                rowId = "latest:" + view.id();
                title = view.name().isBlank() ? "最新" : "最新 - " + view.name();
            } else {
                ItemQuery.Builder query = ItemQuery.browse()
                        .parentId(view.id())
                        .recursive(true)
                        .sortBy("DateCreated,SortName")
                        .sortOrder("Descending")
                        .limit(rowLimit);
                safeFilters.applyTo(query);
                page = client.items(authenticated, query.build());
                rowId = "filtered:" + view.id();
                title = (view.name().isBlank() ? "筛选结果" : view.name()) + " · 筛选";
            }
            addIfNotEmpty(rows, rowId, title, page.items());
        }
        return AndroidCollections.listCopy(rows);
    }

    private static void addIfNotEmpty(List<HomeRow> rows, String id, String title, List<MediaItemSummary> items) {
        if (items != null && !items.isEmpty()) {
            rows.add(new HomeRow(id, title, items));
        }
    }
}
