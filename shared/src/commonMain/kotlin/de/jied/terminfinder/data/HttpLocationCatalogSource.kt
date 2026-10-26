package de.jied.terminfinder.data

import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.LocationCatalogSource
import io.ktor.client.engine.HttpClientEngineFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import terminwatcher.core.api.BASE_URL
import terminwatcher.core.api.DEFAULT_REQUEST_TIMEOUT
import terminwatcher.core.api.DEFAULT_USER_AGENT
import kotlin.time.Duration

fun locationsUrl(serviceId: Int): String = "$BASE_URL/dienstleistung/$serviceId/"

class HttpLocationCatalogSource(
    private val engine: HttpClientEngineFactory<*>,
    private val requestTimeout: Duration = DEFAULT_REQUEST_TIMEOUT,
    private val userAgent: String = DEFAULT_USER_AGENT,
) : LocationCatalogSource {

    override suspend fun fetch(serviceId: Int): List<CatalogEntry> {
        val html = fetchPage(engine, locationsUrl(serviceId), requestTimeout, userAgent)
        return withContext(Dispatchers.Default) { parseLocations(html) }
    }
}

private val CHECKBOX = Regex("""<input[^>]*name="dienstleister\[]"[^>]*>""")

private val INPUT_ID = Regex("""\bid="([^"]+)"""")

private val INPUT_VALUE = Regex("""\bvalue="(\d+)"""")

private val LABEL = Regex("""<label[^>]*for="([^"]+)"[^>]*>(.*?)</label>""", RegexOption.DOT_MATCHES_ALL)

internal fun parseLocations(html: String): List<CatalogEntry> {
    val names = LABEL.findAll(html).associate { it.groupValues[1] to plainText(it.groupValues[2]) }
    return CHECKBOX.findAll(html)
        .mapNotNull { checkbox ->
            val tag = checkbox.value
            val id = INPUT_VALUE.find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            val name = INPUT_ID.find(tag)?.groupValues?.get(1)?.let(names::get).orEmpty()
            if (name.isEmpty()) null else CatalogEntry(id, name)
        }
        .distinctBy { it.id }
        .sortedBy { it.name }
        .toList()
}
