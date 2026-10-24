package de.jied.terminfinder.data

import androidx.datastore.core.DataStore
import de.jied.terminfinder.FakeClock
import de.jied.terminfinder.domain.ServiceCatalogSource
import de.jied.terminfinder.domain.ServiceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

private val services = listOf(ServiceInfo(120686, "Wohnsitz anmelden"))

private class FakeStore(initial: CatalogCache = CatalogCache()) : DataStore<CatalogCache> {
    val state = MutableStateFlow(initial)

    override val data: Flow<CatalogCache> = state

    override suspend fun updateData(transform: suspend (t: CatalogCache) -> CatalogCache): CatalogCache {
        val updated = transform(state.value)
        state.update { updated }
        return updated
    }
}

private class FakeSource(
    private val answer: List<ServiceInfo> = services,
    private val failure: Throwable? = null,
) : ServiceCatalogSource {
    var calls = 0

    override suspend fun fetch(): List<ServiceInfo> {
        calls++
        failure?.let { throw it }
        return answer
    }
}

class CachedServiceCatalogTest {

    @Test
    fun theFirstLoadFetchesAndCachesWhileTheNextOneIsServedFromMemory() = runTest {
        val clock = FakeClock()
        val store = FakeStore()
        val source = FakeSource()
        val catalog = CachedServiceCatalog(store, source, clock)

        assertEquals(services, catalog.load())
        assertEquals(services, catalog.load())

        assertEquals(1, source.calls)
        assertEquals(CatalogCache(clock.now, services), store.state.value)
        assertEquals(services, catalog.cached())
    }

    @Test
    fun aFreshFileCacheIsUsedWithoutTheNetwork() = runTest {
        val clock = FakeClock()
        val store = FakeStore(CatalogCache(clock.now - 1.days, services))
        val source = FakeSource()

        assertEquals(services, CachedServiceCatalog(store, source, clock).load())
        assertEquals(0, source.calls)
    }

    @Test
    fun aStaleCacheIsRefreshed() = runTest {
        val clock = FakeClock()
        val stale = listOf(ServiceInfo(1, "Old"))
        val store = FakeStore(CatalogCache(clock.now - CATALOG_TTL - 1.days, stale))
        val source = FakeSource()

        assertEquals(services, CachedServiceCatalog(store, source, clock).load())
        assertEquals(1, source.calls)
    }

    @Test
    fun aFailedRefreshFallsBackToTheStaleCache() = runTest {
        val clock = FakeClock()
        val store = FakeStore(CatalogCache(clock.now - CATALOG_TTL - 1.days, services))
        val source = FakeSource(failure = IllegalStateException("blocked"))

        assertEquals(services, CachedServiceCatalog(store, source, clock).load())
    }

    @Test
    fun withoutAnyCacheTheFailureReachesTheCaller() = runTest {
        val store = FakeStore()
        val catalog = CachedServiceCatalog(store, FakeSource(failure = IllegalStateException("blocked")), FakeClock())

        assertFailsWith<IllegalStateException> { catalog.load() }
        assertTrue(catalog.cached().isEmpty())
    }

    @Test
    fun anEmptyAnswerCountsAsAFailureAndIsNotCached() = runTest {
        val store = FakeStore()
        val catalog = CachedServiceCatalog(store, FakeSource(answer = emptyList()), FakeClock())

        assertFailsWith<IllegalStateException> { catalog.load() }
        assertEquals(CatalogCache(), store.state.value)
    }
}
