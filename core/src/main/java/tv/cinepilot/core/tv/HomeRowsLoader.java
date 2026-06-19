package tv.cinepilot.core.tv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private final boolean concurrentLowPriorityRows;

    public HomeRowsLoader(MediaBrowserClient client) {
        this(client, 12, 3);
    }

    public HomeRowsLoader(MediaBrowserClient client, int rowLimit) {
        this(client, rowLimit, 3);
    }

    public HomeRowsLoader(MediaBrowserClient client, int rowLimit, int smartCollectionsPerRow) {
        this(client, rowLimit, smartCollectionsPerRow, false);
    }

    private HomeRowsLoader(
            MediaBrowserClient client,
            int rowLimit,
            int smartCollectionsPerRow,
            boolean concurrentLowPriorityRows
    ) {
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
        this.concurrentLowPriorityRows = concurrentLowPriorityRows;
    }

    public static HomeRowsLoader concurrent(MediaBrowserClient client) {
        return new HomeRowsLoader(client, 12, 3, true);
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
        if (concurrentLowPriorityRows) {
            return loadConcurrent(authenticated, safeFilters, includeSmartCollections);
        }
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

    private List<HomeRow> loadConcurrent(
            AuthenticatedServer authenticated,
            MediaBrowseFilters safeFilters,
            boolean includeSmartCollections
    ) {
        MediaItemPage views = client.userViews(authenticated);
        List<HomeRow> rows = new ArrayList<>();
        addIfNotEmpty(rows, "views", "媒体库", views.items());

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Future<MediaItemPage> resume = null;
            Future<MediaItemPage> nextUp = null;
            Future<MediaItemPage> favorites = null;
            if (!safeFilters.isStrict()) {
                resume = submit(pool, () -> client.resumeItems(authenticated, rowLimit));
                nextUp = submit(pool, () -> client.nextUpItems(authenticated, rowLimit));
                favorites = submit(pool, () -> client.favoriteItems(authenticated, rowLimit));
            }
            Future<MediaItemPage> boxSets = submit(pool, () -> client.collections(authenticated, rowLimit));
            Future<MediaItemPage> playlists = submit(pool, () -> client.playlists(authenticated, rowLimit));
            List<RowPageFuture> viewRows = submitViewRows(pool, authenticated, safeFilters, views.items());

            if (!safeFilters.isStrict()) {
                addIfNotEmpty(rows, "resume", "继续观看", await(resume).items());
                addIfNotEmpty(rows, "next-up", "下一集", await(nextUp).items());
                addIfNotEmpty(rows, "favorites", "收藏夹", await(favorites).items());
            }
            MediaItemPage boxSetPage = await(boxSets);
            addIfNotEmpty(rows, "collections", "精选合集", boxSetPage.items());
            addIfNotEmpty(rows, "playlists", "播放列表", await(playlists).items());

            if (includeSmartCollections && safeFilters.isEmpty() && !boxSetPage.items().isEmpty()) {
                addSmartCollectionRows(pool, rows, authenticated, boxSetPage.items());
            }
            for (RowPageFuture viewRow : viewRows) {
                addIfNotEmpty(rows, viewRow.rowId, viewRow.title, await(viewRow.future).items());
            }
        } finally {
            pool.shutdownNow();
        }
        return AndroidCollections.listCopy(rows);
    }

    private List<RowPageFuture> submitViewRows(
            ExecutorService pool,
            AuthenticatedServer authenticated,
            MediaBrowseFilters safeFilters,
            List<MediaItemSummary> views
    ) {
        List<RowPageFuture> futures = new ArrayList<>();
        for (MediaItemSummary view : views) {
            if (view.id().isBlank()) {
                continue;
            }
            if (safeFilters.isEmpty()) {
                futures.add(new RowPageFuture(
                        "latest:" + view.id(),
                        view.name().isBlank() ? "最新" : "最新 - " + view.name(),
                        submit(pool, () -> client.latestItems(authenticated, view.id(), rowLimit))
                ));
            } else {
                futures.add(new RowPageFuture(
                        "filtered:" + view.id(),
                        (view.name().isBlank() ? "筛选结果" : view.name()) + " · 筛选",
                        submit(pool, () -> filteredViewItems(authenticated, safeFilters, view))
                ));
            }
        }
        return futures;
    }

    private void addSmartCollectionRows(
            ExecutorService pool,
            List<HomeRow> rows,
            AuthenticatedServer authenticated,
            List<MediaItemSummary> boxSets
    ) {
        int smartLimit = Math.min(boxSets.size(), smartCollectionsPerRow);
        List<RowPageFuture> smartRows = new ArrayList<>();
        for (int i = 0; i < smartLimit; i++) {
            MediaItemSummary boxSet = boxSets.get(i);
            smartRows.add(new RowPageFuture(
                    "smart-collection:" + boxSet.id(),
                    boxSet.name().isBlank() ? "合集" : boxSet.name(),
                    submit(pool, () -> client.collectionChildren(authenticated, boxSet.id(), rowLimit))
            ));
        }
        for (RowPageFuture smartRow : smartRows) {
            addIfNotEmpty(rows, smartRow.rowId, smartRow.title, await(smartRow.future).items());
        }
    }

    private MediaItemPage filteredViewItems(
            AuthenticatedServer authenticated,
            MediaBrowseFilters safeFilters,
            MediaItemSummary view
    ) {
        ItemQuery.Builder query = ItemQuery.browse()
                .parentId(view.id())
                .recursive(true)
                .sortBy("DateCreated,SortName")
                .sortOrder("Descending")
                .limit(rowLimit);
        safeFilters.applyTo(query);
        return client.items(authenticated, query.build());
    }

    private static Future<MediaItemPage> submit(ExecutorService pool, Callable<MediaItemPage> task) {
        return pool.submit(task);
    }

    private static MediaItemPage await(Future<MediaItemPage> future) {
        if (future == null) {
            return MediaItemPage.empty();
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return MediaItemPage.empty();
        } catch (ExecutionException e) {
            return MediaItemPage.empty();
        }
    }

    private static final class RowPageFuture {
        private final String rowId;
        private final String title;
        private final Future<MediaItemPage> future;

        private RowPageFuture(String rowId, String title, Future<MediaItemPage> future) {
            this.rowId = rowId;
            this.title = title;
            this.future = future;
        }
    }

    private static void addIfNotEmpty(List<HomeRow> rows, String id, String title, List<MediaItemSummary> items) {
        if (items != null && !items.isEmpty()) {
            rows.add(new HomeRow(id, title, items));
        }
    }
}
