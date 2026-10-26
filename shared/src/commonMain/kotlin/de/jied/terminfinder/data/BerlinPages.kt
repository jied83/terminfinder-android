package de.jied.terminfinder.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlin.time.Duration

internal suspend fun fetchPage(
    engine: HttpClientEngineFactory<*>,
    url: String,
    requestTimeout: Duration,
    userAgent: String,
): String {
    val client = HttpClient(engine) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = requestTimeout.inWholeMilliseconds
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        }
    }
    return client.use { it.get(url) { browserHeaders(userAgent) }.bodyAsText() }
}

private const val CONNECT_TIMEOUT_MILLIS = 15_000L

private fun HttpRequestBuilder.browserHeaders(userAgent: String) {
    header("User-Agent", userAgent)
    header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    header("Accept-Language", "de-DE,de;q=0.9")
}

private val TAGS = Regex("<[^>]*>")

private val NUMERIC_ENTITY = Regex("&#(\\d{1,6});")

internal fun plainText(markup: String): String = markup
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
