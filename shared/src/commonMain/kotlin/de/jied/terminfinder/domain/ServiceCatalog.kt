package de.jied.terminfinder.domain

import kotlinx.serialization.Serializable

@Serializable
data class ServiceInfo(val id: Int, val name: String)

interface ServiceCatalog {
    suspend fun load(): List<ServiceInfo>
    fun cached(): List<ServiceInfo>
}

fun interface ServiceCatalogSource {
    suspend fun fetch(): List<ServiceInfo>
}
