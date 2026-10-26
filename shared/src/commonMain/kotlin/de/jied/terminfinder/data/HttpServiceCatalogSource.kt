package de.jied.terminfinder.data

import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.ServiceCatalogSource
import io.ktor.client.engine.HttpClientEngineFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import terminwatcher.core.api.BASE_URL
import terminwatcher.core.api.DEFAULT_REQUEST_TIMEOUT
import terminwatcher.core.api.DEFAULT_USER_AGENT
import kotlin.time.Duration

const val CATALOG_URL: String = "$BASE_URL/dienstleistungen/"

class HttpServiceCatalogSource(
    private val engine: HttpClientEngineFactory<*>,
    private val requestTimeout: Duration = DEFAULT_REQUEST_TIMEOUT,
    private val userAgent: String = DEFAULT_USER_AGENT,
) : ServiceCatalogSource {

    override suspend fun fetch(): List<CatalogEntry> {
        val html = fetchPage(engine, CATALOG_URL, requestTimeout, userAgent)
        return withContext(Dispatchers.Default) { parseServices(html) }
    }
}

private val ENTRY = Regex(
    """href="(?:https://service\.berlin\.de)?/dienstleistung/(\d+)/?"[^>]*>(.*?)</a>""",
    RegexOption.DOT_MATCHES_ALL,
)

internal fun parseServices(html: String): List<CatalogEntry> = ENTRY.findAll(html)
    .mapNotNull { match ->
        val id = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        val name = plainText(match.groupValues[2])
        if (name.isEmpty()) null else CatalogEntry(id, name)
    }
    .distinctBy { it.id }
    .sortedBy { it.name }
    .toList()
