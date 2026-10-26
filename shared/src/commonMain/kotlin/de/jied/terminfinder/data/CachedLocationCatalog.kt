package de.jied.terminfinder.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.LocationCatalog
import de.jied.terminfinder.domain.LocationCatalogSource
import de.jied.terminfinder.logDebug
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class LocationCacheEntry(
    val fetchedAt: Instant? = null,
    val locations: List<CatalogEntry> = emptyList(),
)

@Serializable
data class LocationCache(
    val byService: Map<String, LocationCacheEntry> = emptyMap(),
)

class CachedLocationCatalog(
    private val store: DataStore<LocationCache>,
    private val source: LocationCatalogSource,
    private val clock: Clock,
    private val ttl: Duration = CATALOG_TTL,
) : LocationCatalog {

    private val mutex = Mutex()
    private var memory: Map<Int, List<CatalogEntry>> = emptyMap()

    override fun cached(serviceId: Int): List<CatalogEntry> = memory[serviceId].orEmpty()

    override suspend fun load(serviceId: Int): List<CatalogEntry> = mutex.withLock {
        memory[serviceId]?.takeIf { it.isNotEmpty() }?.let { return it }

        val cached = store.data.first().byService[serviceId.toString()]
        if (cached != null && cached.locations.isNotEmpty() && cached.fetchedAt != null &&
            clock.now() - cached.fetchedAt < ttl
        ) {
            return remember(serviceId, cached.locations)
        }

        val fetched = try {
            source.fetch(serviceId).ifEmpty { error("the service page listed no offices") }
        } catch (failure: Throwable) {
            logDebug("location fetch for service $serviceId failed: $failure")
            val stale = cached?.locations.orEmpty()
            if (stale.isEmpty()) throw failure
            return remember(serviceId, stale)
        }
        logDebug("fetched ${fetched.size} offices for service $serviceId")
        store.updateData { cache ->
            LocationCache(cache.byService + (serviceId.toString() to LocationCacheEntry(clock.now(), fetched)))
        }
        return remember(serviceId, fetched)
    }

    private fun remember(serviceId: Int, locations: List<CatalogEntry>): List<CatalogEntry> {
        memory = memory + (serviceId to locations)
        return locations
    }
}

object LocationCacheSerializer : OkioSerializer<LocationCache> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: LocationCache = LocationCache()

    override suspend fun readFrom(source: BufferedSource): LocationCache =
        runCatching { json.decodeFromString(LocationCache.serializer(), source.readUtf8()) }
            .getOrElse { defaultValue } // a damaged cache is simply refetched

    override suspend fun writeTo(t: LocationCache, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(LocationCache.serializer(), t))
    }
}
