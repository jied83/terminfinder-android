package de.jied.terminfinder.data

import de.jied.terminfinder.domain.ServiceInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceCatalogParserTest {

    @Test
    fun readsIdsAndNamesFromTheDirectoryMarkup() {
        val html = """
            <ul class="list--neutral">
              <li><a href="https://service.berlin.de/dienstleistung/120335/" class="title">Abmeldung einer Wohnung</a></li>
              <li><a class="title" href="/dienstleistung/121598/">Personal&#173;ausweis
                  <span class="hint">beantragen</span></a></li>
              <li><a href="https://service.berlin.de/dienstleistung/326417/">Anerkennung einer &quot;geeigneten Stelle&quot;</a></li>
            </ul>
        """.trimIndent()

        assertEquals(
            listOf(
                ServiceInfo(120335, "Abmeldung einer Wohnung"),
                ServiceInfo(326417, "Anerkennung einer \"geeigneten Stelle\""),
                ServiceInfo(121598, "Personalausweis beantragen"),
            ),
            parseServices(html),
        )
    }

    @Test
    fun skipsRepeatedAndEmptyLinks() {
        val html = """
            <a href="/dienstleistung/120686/">Wohnsitz anmelden</a>
            <a href="/dienstleistung/120686/">Wohnsitz anmelden</a>
            <a href="/dienstleistung/120686/"><i class="fas fa-info"></i></a>
        """.trimIndent()

        assertEquals(listOf(ServiceInfo(120686, "Wohnsitz anmelden")), parseServices(html))
    }

    @Test
    fun aPageWithoutServicesIsEmpty() {
        assertEquals(emptyList(), parseServices("<html><body>Unerwünschter Zugriff</body></html>"))
    }
}
