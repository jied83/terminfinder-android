package de.jied.terminfinder.data

import androidx.datastore.core.DataStore
import de.jied.terminfinder.FakeClock
import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.LocationCatalogSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

private const val WOHNSITZ = 120686
private const val ABMELDUNG = 120335

private val adlershof = listOf(CatalogEntry(122208, "Bürgeramt Adlershof"))
private val mitte = listOf(CatalogEntry(122217, "Bürgeramt Mitte"))

private class FakeLocationStore(initial: LocationCache = LocationCache()) : DataStore<LocationCache> {
    val state = MutableStateFlow(initial)

    override val data: Flow<LocationCache> = state

    override suspend fun updateData(transform: suspend (t: LocationCache) -> LocationCache): LocationCache {
        val updated = transform(state.value)
        state.update { updated }
        return updated
    }
}

private class FakeLocationSource(
    private val answers: Map<Int, List<CatalogEntry>> = mapOf(WOHNSITZ to adlershof, ABMELDUNG to mitte),
    private val failure: Throwable? = null,
) : LocationCatalogSource {
    var calls = 0

    override suspend fun fetch(serviceId: Int): List<CatalogEntry> {
        calls++
        failure?.let { throw it }
        return answers[serviceId].orEmpty()
    }
}

class CachedLocationCatalogTest {

    @Test
    fun theFirstLoadFetchesAndCachesWhileTheNextOneIsServedFromMemory() = runTest {
        val clock = FakeClock()
        val store = FakeLocationStore()
        val source = FakeLocationSource()
        val catalog = CachedLocationCatalog(store, source, clock)

        assertEquals(adlershof, catalog.load(WOHNSITZ))
        assertEquals(adlershof, catalog.load(WOHNSITZ))

        assertEquals(1, source.calls)
        assertEquals(adlershof, catalog.cached(WOHNSITZ))
        assertEquals(
            LocationCache(mapOf(WOHNSITZ.toString() to LocationCacheEntry(clock.now, adlershof))),
            store.state.value,
        )
    }

    @Test
    fun eachServiceKeepsItsOwnOffices() = runTest {
        val store = FakeLocationStore()
        val source = FakeLocationSource()
        val catalog = CachedLocationCatalog(store, source, FakeClock())

        assertEquals(adlershof, catalog.load(WOHNSITZ))
        assertEquals(mitte, catalog.load(ABMELDUNG))

        assertEquals(2, source.calls)
        assertEquals(adlershof, catalog.cached(WOHNSITZ))
        assertEquals(mitte, catalog.cached(ABMELDUNG))
        assertEquals(setOf(WOHNSITZ.toString(), ABMELDUNG.toString()), store.state.value.byService.keys)
    }

    @Test
    fun aFreshFileCacheIsUsedWithoutTheNetwork() = runTest {
        val clock = FakeClock()
        val store = FakeLocationStore(
            LocationCache(mapOf(WOHNSITZ.toString() to LocationCacheEntry(clock.now - 1.days, adlershof))),
        )
        val source = FakeLocationSource()

        assertEquals(adlershof, CachedLocationCatalog(store, source, clock).load(WOHNSITZ))
        assertEquals(0, source.calls)
    }

    @Test
    fun aCacheForAnotherServiceDoesNotAnswerForThisOne() = runTest {
        val clock = FakeClock()
        val store = FakeLocationStore(
            LocationCache(mapOf(ABMELDUNG.toString() to LocationCacheEntry(clock.now, mitte))),
        )
        val source = FakeLocationSource()

        assertEquals(adlershof, CachedLocationCatalog(store, source, clock).load(WOHNSITZ))
        assertEquals(1, source.calls)
        assertEquals(mitte, store.state.value.byService[ABMELDUNG.toString()]?.locations)
    }

    @Test
    fun aStaleCacheIsRefreshed() = runTest {
        val clock = FakeClock()
        val stale = listOf(CatalogEntry(1, "Old"))
        val store = FakeLocationStore(
            LocationCache(
                mapOf(WOHNSITZ.toString() to LocationCacheEntry(clock.now - CATALOG_TTL - 1.days, stale)),
            ),
        )
        val source = FakeLocationSource()

        assertEquals(adlershof, CachedLocationCatalog(store, source, clock).load(WOHNSITZ))
        assertEquals(1, source.calls)
    }

    @Test
    fun aFailedRefreshFallsBackToTheStaleCache() = runTest {
        val clock = FakeClock()
        val store = FakeLocationStore(
            LocationCache(
                mapOf(WOHNSITZ.toString() to LocationCacheEntry(clock.now - CATALOG_TTL - 1.days, adlershof)),
            ),
        )
        val source = FakeLocationSource(failure = IllegalStateException("blocked"))

        assertEquals(adlershof, CachedLocationCatalog(store, source, clock).load(WOHNSITZ))
    }

    @Test
    fun withoutAnyCacheTheFailureReachesTheCaller() = runTest {
        val store = FakeLocationStore()
        val catalog = CachedLocationCatalog(
            store,
            FakeLocationSource(failure = IllegalStateException("blocked")),
            FakeClock(),
        )

        assertFailsWith<IllegalStateException> { catalog.load(WOHNSITZ) }
        assertTrue(catalog.cached(WOHNSITZ).isEmpty())
    }

    @Test
    fun aServiceWithoutOfficesCountsAsAFailureAndIsNotCached() = runTest {
        val store = FakeLocationStore()
        val catalog = CachedLocationCatalog(store, FakeLocationSource(), FakeClock())

        assertFailsWith<IllegalStateException> { catalog.load(999) }
        assertEquals(LocationCache(), store.state.value)
    }
}
