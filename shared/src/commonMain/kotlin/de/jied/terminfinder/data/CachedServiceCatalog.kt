package de.jied.terminfinder.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.ServiceCatalog
import de.jied.terminfinder.domain.ServiceCatalogSource
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

val CATALOG_TTL: Duration = 30.days

@Serializable
data class CatalogCache(
    val fetchedAt: Instant? = null,
    val services: List<CatalogEntry> = emptyList(),
)

class CachedServiceCatalog(
    private val store: DataStore<CatalogCache>,
    private val source: ServiceCatalogSource,
    private val clock: Clock,
    private val ttl: Duration = CATALOG_TTL,
) : ServiceCatalog {

    private val mutex = Mutex()
    private var memory: List<CatalogEntry> = emptyList()

    override fun cached(): List<CatalogEntry> = memory

    override suspend fun load(): List<CatalogEntry> = mutex.withLock {
        memory.takeIf { it.isNotEmpty() }?.let { return it }

        val cache = store.data.first()
        if (cache.services.isNotEmpty() && cache.fetchedAt != null && clock.now() - cache.fetchedAt < ttl) {
            return remember(cache.services)
        }

        val fetched = try {
            source.fetch().ifEmpty { error("the directory page yielded no services") }
        } catch (failure: Throwable) {
            logDebug("catalog fetch failed: $failure")
            if (cache.services.isEmpty()) throw failure
            return remember(cache.services)
        }
        logDebug("catalog fetched ${fetched.size} services")
        store.updateData { CatalogCache(clock.now(), fetched) }
        return remember(fetched)
    }

    private fun remember(services: List<CatalogEntry>): List<CatalogEntry> {
        memory = services
        return services
    }
}

object CatalogCacheSerializer : OkioSerializer<CatalogCache> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: CatalogCache = CatalogCache()

    override suspend fun readFrom(source: BufferedSource): CatalogCache =
        runCatching { json.decodeFromString(CatalogCache.serializer(), source.readUtf8()) }
            .getOrElse { defaultValue } // a damaged cache is simply refetched

    override suspend fun writeTo(t: CatalogCache, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(CatalogCache.serializer(), t))
    }
}
