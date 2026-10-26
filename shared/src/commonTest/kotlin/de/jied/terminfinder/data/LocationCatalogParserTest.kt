package de.jied.terminfinder.data

import de.jied.terminfinder.domain.CatalogEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationCatalogParserTest {

    private val html = """
        <div class="form-check">
            <input name="dienstleister[]" data-checkgroup-group="12" type="checkbox" id="listitem12-1"
                class="form-check-input" value="122208" />
            <label for="listitem12-1" class="form-check-label">B&#252;rgeramt Adlershof
            </label>
        </div>
        <div class="form-check">
            <input name="dienstleister[]" type="checkbox" id="listitem12-2" class="form-check-input"
                value="122217" />
            <label for="listitem12-2" class="form-check-label">B&#252;rgeramt&#173; Mitte</label>
        </div>
    """.trimIndent()

    @Test
    fun readsOfficesSortedByName() {
        assertEquals(
            listOf(
                CatalogEntry(122208, "Bürgeramt Adlershof"),
                CatalogEntry(122217, "Bürgeramt Mitte"),
            ),
            parseLocations(html),
        )
    }

    @Test
    fun ignoresCheckboxesOfOtherForms() {
        val other = """
            <input name="anliegen[]" type="checkbox" id="x1" value="120686" />
            <label for="x1">Wohnsitz anmelden</label>
        """.trimIndent()
        assertEquals(emptyList(), parseLocations(other))
    }

    @Test
    fun skipsOfficesWhoseLabelIsMissing() {
        val orphan = """<input name="dienstleister[]" type="checkbox" id="gone" value="122208" />"""
        assertEquals(emptyList(), parseLocations(orphan))
    }

    @Test
    fun buildsTheServicePageUrl() {
        assertEquals("https://service.berlin.de/dienstleistung/120686/", locationsUrl(120686))
    }
}
