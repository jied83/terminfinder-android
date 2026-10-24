package de.jied.terminfinder.data

import de.jied.terminfinder.domain.ServiceCatalogSource
import de.jied.terminfinder.domain.ServiceInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
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

    override suspend fun fetch(): List<ServiceInfo> = newClient().use { client ->
        val html = client.get(CATALOG_URL) { browserHeaders() }.bodyAsText()
        withContext(Dispatchers.Default) { parseServices(html) }
    }

    private fun newClient() = HttpClient(engine) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = requestTimeout.inWholeMilliseconds
            connectTimeoutMillis = 15_000
        }
    }

    private fun HttpRequestBuilder.browserHeaders() {
        header("User-Agent", userAgent)
        header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        header("Accept-Language", "de-DE,de;q=0.9")
    }
}

private val ENTRY = Regex(
    """href="(?:https://service\.berlin\.de)?/dienstleistung/(\d+)/?"[^>]*>(.*?)</a>""",
    RegexOption.DOT_MATCHES_ALL,
)

private val TAGS = Regex("<[^>]*>")

private val NUMERIC_ENTITY = Regex("&#(\\d{1,6});")

internal fun parseServices(html: String): List<ServiceInfo> = ENTRY.findAll(html)
    .mapNotNull { match ->
        val id = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        val name = plainText(match.groupValues[2])
        if (name.isEmpty()) null else ServiceInfo(id, name)
    }
    .distinctBy { it.id }
    .sortedBy { it.name }
    .toList()

private fun plainText(markup: String): String = markup
    .replace(TAGS, "")
    .let { text -> NUMERIC_ENTITY.replace(text) { it.groupValues[1].toInt().toChar().toString() } }
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")
    .replace("­", "")
    .split(Regex("\\s+"))
    .filter { it.isNotEmpty() }
    .joinToString(" ")
