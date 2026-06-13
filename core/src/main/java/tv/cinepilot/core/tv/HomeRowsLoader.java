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
    private final int smartCollectionsPerRow;

    public HomeRowsLoader(MediaBrowserClient client) {
        this(client, 12, 3);
    }

    public HomeRowsLoader(MediaBrowserClient client, int rowLimit) {
        this(client, rowLimit, 3);
    }

    public HomeRowsLoader(MediaBrowserClient client, int rowLimit, int smartCollectionsPerRow) {
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        if (rowLimit <= 0) {
            throw new IllegalArgumentException("rowLimit must be positive");
        }
        if (smartCollectionsPerRow <= 0) {
            throw new IllegalArgumentException("smartCollectionsPerRow must be positive");
        }
        this.client = client;
        this.rowLimit = rowLimit;
        this.smartCollectionsPerRow = smartCollectionsPerRow;
    }

    public List<HomeRow> load(AuthenticatedServer authenticated) {
        return load(authenticated, MediaBrowseFilters.EMPTY, true);
    }

    public List<HomeRow> load(AuthenticatedServer authenticated, MediaBrowseFilters filters) {
        return load(authenticated, filters, true);
    }

    /**
     * Variant that lets the UI layer disable server-defined smart collection
     * rows. Keeping this as an explicit boolean (instead of reading shared
     * preferences from :core) preserves the clean protocol-vs-platform
     * module boundary that check.sh hygiene guards enforce.
     */
    public List<HomeRow> load(
            AuthenticatedServer authenticated,
            MediaBrowseFilters filters,
            boolean includeSmartCollections
    ) {
        MediaBrowseFilters safeFilters = filters == null ? MediaBrowseFilters.EMPTY : filters;
        List<HomeRow> rows = new ArrayList<>();
        MediaItemPage views = client.userViews(authenticated);
        addIfNotEmpty(rows, "views", "媒体库", views.items());
        if (!safeFilters.isStrict()) {
            addIfNotEmpty(rows, "resume", "继续观看", client.resumeItems(authenticated, rowLimit).items());
            addIfNotEmpty(rows, "next-up", "下一集", client.nextUpItems(authenticated, rowLimit).items());
            addIfNotEmpty(rows, "favorites", "收藏夹", client.favoriteItems(authenticated, rowLimit).items());
        }
        MediaItemPage boxSets = client.collections(authenticated, rowLimit);
        addIfNotEmpty(rows, "collections", "精选合集", boxSets.items());

        // Playlists row (P3-2)
        MediaItemPage playlists = client.playlists(authenticated, rowLimit);
        addIfNotEmpty(rows, "playlists", "播放列表", playlists.items());

        if (includeSmartCollections && safeFilters.isEmpty() && !boxSets.items().isEmpty()) {
            int smartLimit = Math.min(boxSets.items().size(), smartCollectionsPerRow);
            for (int i = 0; i < smartLimit; i++) {
                MediaItemSummary boxSet = boxSets.items().get(i);
                String rowId = "smart-collection:" + boxSet.id();
                String title = boxSet.name().isBlank() ? "合集" : boxSet.name();
                MediaItemPage children = client.collectionChildren(authenticated, boxSet.id(), rowLimit);
                addIfNotEmpty(rows, rowId, title, children.items());
            }
        }

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
