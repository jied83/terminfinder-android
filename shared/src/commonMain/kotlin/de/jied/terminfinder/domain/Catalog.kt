package de.jied.terminfinder.domain

import kotlinx.serialization.Serializable

@Serializable
data class CatalogEntry(val id: Int, val name: String)

interface ServiceCatalog {
    suspend fun load(): List<CatalogEntry>
    fun cached(): List<CatalogEntry>
}

fun interface ServiceCatalogSource {
    suspend fun fetch(): List<CatalogEntry>
}

interface LocationCatalog {
    suspend fun load(serviceId: Int): List<CatalogEntry>
    fun cached(serviceId: Int): List<CatalogEntry>
}

fun interface LocationCatalogSource {
    suspend fun fetch(serviceId: Int): List<CatalogEntry>
}
