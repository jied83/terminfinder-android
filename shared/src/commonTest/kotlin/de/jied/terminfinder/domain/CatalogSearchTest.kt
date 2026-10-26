package de.jied.terminfinder.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogSearchTest {
    private val services = listOf(
        CatalogEntry(120686, "Wohnsitz - Alleinige Wohnung oder Hauptwohnung anmelden"),
        CatalogEntry(120335, "Abmeldung einer Wohnung"),
        CatalogEntry(121598, "Personalausweis beantragen"),
        CatalogEntry(326422, "Bürgeramt - Beratung"),
        CatalogEntry(120882, "Kraftfahrzeug - Erstzulassung eines Fahrzeugs (Neuzulassung) beantragen"),
    )

    @Test
    fun everyWordOfTheQueryHasToAppearInTheName() {
        assertEquals(
            listOf(CatalogEntry(120686, "Wohnsitz - Alleinige Wohnung oder Hauptwohnung anmelden")),
            CatalogSearch.match("wohnung anmelden", services),
        )
    }

    @Test
    fun aNameOpeningWithTheQueryComesFirst() {
        val found = CatalogSearch.match("wohn", services)

        assertEquals(listOf(120686, 120335), found.map { it.id })
    }

    @Test
    fun umlautsAreFoldedInBothDirections() {
        val expected = listOf(CatalogEntry(326422, "Bürgeramt - Beratung"))

        assertEquals(expected, CatalogSearch.match("bürgeramt", services))
        assertEquals(expected, CatalogSearch.match("burgeramt", services))
        assertEquals(expected, CatalogSearch.match("buergeramt", services))
    }

    @Test
    fun digitsSearchIdsByPrefix() {
        assertEquals(listOf(120335, 120686, 120882), CatalogSearch.match("120", services).map { it.id })
        assertEquals(emptyList(), CatalogSearch.match("999", services))
    }

    @Test
    fun aQueryTooShortOrWithoutMatchesYieldsNothing() {
        assertEquals(emptyList(), CatalogSearch.match("w", services))
        assertEquals(emptyList(), CatalogSearch.match("   ", services))
        assertEquals(emptyList(), CatalogSearch.match("hundesteuer", services))
    }

    @Test
    fun theLimitCapsTheList() {
        assertEquals(2, CatalogSearch.match("an", services, limit = 2).size)
    }
}
