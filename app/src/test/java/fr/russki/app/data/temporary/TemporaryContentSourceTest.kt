package fr.russki.app.data.temporary

import fr.russki.app.domain.model.HomeContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryContentSourceTest {

    @Test
    fun loadHome_returnsDashboardFromTemporarySource() = runBlocking {
        val content = TemporaryContentSource.loadHome()
        assertTrue(content is HomeContent.Dashboard)
        assertEquals(TemporaryContentSource.SOURCE_TAG, "temporary-preview")
    }

    @Test
    fun loadModules_isNotEmpty() = runBlocking {
        val modules = TemporaryContentSource.loadModules()
        assertTrue(modules.isNotEmpty())
    }

    @Test
    fun loadReviewDueCount_isZeroForEmptyStatePhase() = runBlocking {
        assertEquals(0, TemporaryContentSource.loadReviewDueCount())
    }
}
