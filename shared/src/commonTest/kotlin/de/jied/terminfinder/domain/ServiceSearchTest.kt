package de.jied.terminfinder.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceSearchTest {
    private val services = listOf(
        ServiceInfo(120686, "Wohnsitz - Alleinige Wohnung oder Hauptwohnung anmelden"),
        ServiceInfo(120335, "Abmeldung einer Wohnung"),
        ServiceInfo(121598, "Personalausweis beantragen"),
        ServiceInfo(326422, "Bürgeramt - Beratung"),
        ServiceInfo(120882, "Kraftfahrzeug - Erstzulassung eines Fahrzeugs (Neuzulassung) beantragen"),
    )

    @Test
    fun everyWordOfTheQueryHasToAppearInTheName() {
        assertEquals(
            listOf(ServiceInfo(120686, "Wohnsitz - Alleinige Wohnung oder Hauptwohnung anmelden")),
            ServiceSearch.match("wohnung anmelden", services),
        )
    }

    @Test
    fun aNameOpeningWithTheQueryComesFirst() {
        val found = ServiceSearch.match("wohn", services)

        assertEquals(listOf(120686, 120335), found.map { it.id })
    }

    @Test
    fun umlautsAreFoldedInBothDirections() {
        val expected = listOf(ServiceInfo(326422, "Bürgeramt - Beratung"))

        assertEquals(expected, ServiceSearch.match("bürgeramt", services))
        assertEquals(expected, ServiceSearch.match("burgeramt", services))
        assertEquals(expected, ServiceSearch.match("buergeramt", services))
    }

    @Test
    fun digitsSearchIdsByPrefix() {
        assertEquals(listOf(120335, 120686, 120882), ServiceSearch.match("120", services).map { it.id })
        assertEquals(emptyList(), ServiceSearch.match("999", services))
    }

    @Test
    fun aQueryTooShortOrWithoutMatchesYieldsNothing() {
        assertEquals(emptyList(), ServiceSearch.match("w", services))
        assertEquals(emptyList(), ServiceSearch.match("   ", services))
        assertEquals(emptyList(), ServiceSearch.match("hundesteuer", services))
    }

    @Test
    fun theLimitCapsTheList() {
        assertEquals(2, ServiceSearch.match("an", services, limit = 2).size)
    }
}
