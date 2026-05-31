package tv.cinepilot.core.tv;

import java.util.ArrayDeque;
import java.util.Deque;
import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.protocol.MediaItemPage;

final class BrowseSession {
    static final int FOLDER_PAGE_SIZE = 50;

    private final Deque<TvAppState> backStack = new ArrayDeque<>();
    private FolderContext folderContext;

    void clear() {
        backStack.clear();
        folderContext = null;
    }

    TvAppState openFolder(TvAppState state, String parentId, String title, MediaItemPage page) {
        backStack.push(state);
        String rowTitle = title == null || title.isBlank() ? "子项目" : title;
        folderContext = new FolderContext(parentId, rowTitle, page.totalRecordCount(), page.startIndex());
        return folderState(state, page);
    }

    TvAppState openSearch(TvAppState state, String term, MediaItemPage page) {
        String query = term.trim();
        backStack.push(state);
        folderContext = null;
        return TvWorkflow.homeLoaded(
                state,
                AndroidCollections.singletonList(new HomeRow("search:" + query, "搜索：" + query, page.items()))
        );
    }

    boolean canGoBack() {
        return !backStack.isEmpty();
    }

    boolean canPageBackward() {
        return folderContext != null && folderContext.startIndex() > 0;
    }

    boolean canPageForward() {
        return folderContext != null
                && folderContext.startIndex() + FOLDER_PAGE_SIZE < folderContext.totalRecordCount();
    }

    String currentFolderParentId() {
        requireFolderContext();
        return folderContext.parentId();
    }

    int previousPageStartIndex() {
        requireFolderContext();
        return Math.max(0, folderContext.startIndex() - FOLDER_PAGE_SIZE);
    }

    int nextPageStartIndex() {
        requireFolderContext();
        return folderContext.startIndex() + FOLDER_PAGE_SIZE;
    }

    TvAppState updateFolderPage(TvAppState state, MediaItemPage page) {
        requireFolderContext();
        folderContext = folderContext.withPage(page.totalRecordCount(), page.startIndex());
        return folderState(state, page);
    }

    TvAppState backOrNull(TvAppState state) {
        if (state.route() != TvRoute.HOME || backStack.isEmpty()) {
            return null;
        }
        folderContext = null;
        return backStack.pop();
    }

    private void requireFolderContext() {
        if (folderContext == null) {
            throw new IllegalStateException("folder browse context is required");
        }
    }

    private TvAppState folderState(TvAppState state, MediaItemPage page) {
        String title = folderContext.title();
        if (folderContext.totalRecordCount() > FOLDER_PAGE_SIZE) {
            int first = folderContext.startIndex() + 1;
            int last = Math.min(folderContext.startIndex() + page.items().size(), folderContext.totalRecordCount());
            title = title + " " + first + "-" + last + "/" + folderContext.totalRecordCount();
        }
        return TvWorkflow.homeLoaded(
                state,
                AndroidCollections.singletonList(new HomeRow("folder:" + folderContext.parentId(), title, page.items()))
        );
    }

    private record FolderContext(
            String parentId,
            String title,
            int totalRecordCount,
            int startIndex
    ) {
        FolderContext withPage(int nextTotalRecordCount, int nextStartIndex) {
            return new FolderContext(parentId, title, nextTotalRecordCount, nextStartIndex);
        }
    }
}
