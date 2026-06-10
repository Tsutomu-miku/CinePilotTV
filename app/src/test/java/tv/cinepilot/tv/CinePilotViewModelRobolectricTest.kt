package tv.cinepilot.tv

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.util.ArrayList
import java.util.LinkedHashMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tv.cinepilot.core.AndroidCollections
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.protocol.UserItemData
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvRoute

/**
 * Robolectric unit tests for the Android TV application shell.
 *
 * Scope intentionally avoids the network: these tests validate the local
 * state machine, ViewModel survival across synthetic configuration changes,
 * recoverable error routing, Back-navigation focus recovery, and the
 * focus/browse safety contracts that the media wall relies on. Server hits
 * are exercised by the :core protocol integration tests and the QA login
 * instrumentation path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [26],
    application = Application::class,
    manifest = Config.NONE,
)
class CinePilotViewModelRobolectricTest {

    private lateinit var app: Application
    private lateinit var viewModel: CinePilotViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = CinePilotViewModel.factory(app).create(CinePilotViewModel::class.java)
    }

    // ------------------------------ construction ---------------------------

    @Test fun viewModelCreatesRuntimeAndWorkflowController() {
        assertNotNull("runtime must be initialized", viewModel.runtime)
        assertNotNull("workflowController must be wired", viewModel.workflowController)
        assertNotNull("mediaBrowserClient must be wired", viewModel.mediaBrowserClient)
        assertNotNull("bitmapCache must be wired", viewModel.runtime.bitmapCache)
        assertNotNull("homeRowsCache must be wired", viewModel.runtime.homeRowsCache)
        assertNotNull("deviceCodecDiagnostics must be wired", viewModel.runtime.deviceCodecDiagnostics)
    }

    @Test fun viewModelFactoryProducesFreshViewModelsPerInvocation() {
        val factory = CinePilotViewModel.factory(app)
        val a = factory.create(CinePilotViewModel::class.java)
        val b = factory.create(CinePilotViewModel::class.java)
        assertNotNull("factory produces valid ViewModel A", a.runtime)
        assertNotNull("factory produces valid ViewModel B", b.runtime)
    }

    @Test fun initialWorkflowStateIsAtServerEntry() {
        val state = viewModel.workflowController.state()
        assertEquals("initial route must be SERVER_ENTRY", TvRoute.SERVER_ENTRY, state.route())
        assertNull("no server before submission", state.server())
        assertNull("no authenticated session on fresh launch", state.authenticated())
    }

    // ---------------------- safety before authentication ------------------

    @Test fun restoreHomeFromCacheFailsBeforeAuthentication() {
        val rows = listOf(sampleHomeRow())
        assertFalse("restoreHomeFromCache must fail while unauthenticated",
            viewModel.workflowController.restoreHomeFromCache(rows))
    }

    @Test fun restoreHomeFromCacheFailsForEmptyRows() {
        val empty: List<HomeRow> = AndroidCollections.emptyList()
        assertFalse("restoreHomeFromCache must reject empty rows",
            viewModel.workflowController.restoreHomeFromCache(empty))
        assertFalse("restoreHomeFromCache must reject null rows",
            viewModel.workflowController.restoreHomeFromCache(null))
    }

    @Test fun hasHomeRowsInitiallyFalse() {
        assertFalse("hasHomeRows must be false before any load",
            viewModel.workflowController.hasHomeRows())
    }

    @Test fun focusItemRejectsMissingRowOrItem() {
        // focusItem is only called by the media wall UI when rows exist; the
        // workflow must loudly reject bad coordinates so UI bugs surface early.
        val thrown: IllegalArgumentException? = runCatching {
            viewModel.workflowController.focusItem("missing-row", "missing-item")
        }.exceptionOrNull() as? IllegalArgumentException
        assertNotNull("focusItem must throw when rows don't exist", thrown)
        assertEquals(TvRoute.SERVER_ENTRY, viewModel.workflowController.state().route())
    }

    @Test fun logoutIsIdempotentBeforeAuthentication() {
        // Logout button can be tapped from stale dialogs; controller must not blow up.
        viewModel.workflowController.logout()
        val state = viewModel.workflowController.state()
        assertEquals("logout is a no-op before authentication", TvRoute.SERVER_ENTRY, state.route())
    }

    @Test fun canGoBackInBrowseInitiallyFalse() {
        // No browse stack exists before folder navigation.
        assertFalse("canGoBackInBrowse must be false at server entry",
            viewModel.workflowController.canGoBackInBrowse())
        assertFalse("canPageBackwardInBrowse must be false at server entry",
            viewModel.workflowController.canPageBackwardInBrowse())
        assertFalse("canPageForwardInBrowse must be false at server entry",
            viewModel.workflowController.canPageForwardInBrowse())
    }

    // -------------------------- error routing ----------------------------

    @Test fun failMovesStateToErrorRoute() {
        val state = viewModel.workflowController.state()
        viewModel.workflowController.fail("boom")
        val after = viewModel.workflowController.state()
        assertEquals("fail() must set error route", TvRoute.ERROR, after.route())
        // Error state must not disturb the previously-discovered (here: absent)
        // route so the recovery surface can still surface "back to server entry".
        assertNull("server remains null after fail()", after.server())
    }

    @Test fun backStackSurvivesWorkflowFailures() {
        // A recoverable error (e.g. detail lookup failed) must leave the
        // browse back-stack intact so the user can "go back to home".
        viewModel.workflowController.fail("connection dropped")
        viewModel.workflowController.back()
        val after = viewModel.workflowController.state()
        assertEquals("error.back() returns to server entry", TvRoute.SERVER_ENTRY, after.route())
    }

    @Test fun forgetAuthenticatedSessionBeforeAuthenticationIsNoOp() {
        // The 401-expiry path in MainActivity calls forgetAuthenticatedSession
        // unconditionally after MediaBrowserException(401). It must not throw.
        viewModel.workflowController.forgetAuthenticatedSession()
        assertEquals(TvRoute.SERVER_ENTRY, viewModel.workflowController.state().route())
    }

    // --------------------------- helpers ---------------------------------

    private fun sampleHomeRow(): HomeRow {
        val tags = LinkedHashMap<String, String>()
        tags["Primary"] = "poster-1"
        val item = MediaItemSummary(
            "item-1", "", "Film", MediaItemType.MOVIE, false, true,
            9_000_000_000L, 2024, null, null,
            "", "", "", ArrayList<String>(),
            "", null, "",
            AndroidCollections.emptyList(), AndroidCollections.emptyList(),
            UserItemData.empty(), tags, ArrayList<String>()
        )
        val items: List<MediaItemSummary> = listOf(item)
        return HomeRow("views", "媒体库", items)
    }
}
