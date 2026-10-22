package de.jied.terminfinder.domain

object TaskInput {
    fun serviceId(raw: String): Int? = raw.trim().toIntOrNull()?.takeIf { it > 0 }

    fun locationIds(raw: String): List<Int>? {
        if (raw.isBlank()) return emptyList()
        return raw.split(',').map { part -> part.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null }
    }

    fun intervalMinutes(raw: String): Int? = raw.trim().toIntOrNull()?.takeIf(Ticks::isValidInterval)
}
