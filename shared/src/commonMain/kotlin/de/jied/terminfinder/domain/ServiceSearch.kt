package de.jied.terminfinder.domain

object ServiceSearch {
    const val MIN_QUERY_LENGTH: Int = 2
    const val LIMIT: Int = 20

    data class Entry(val service: ServiceInfo, val folded: String)

    fun index(services: List<ServiceInfo>): List<Entry> = services.map { Entry(it, normalize(it.name)) }

    fun search(query: String, index: List<Entry>, limit: Int = LIMIT): List<ServiceInfo> {
        val raw = query.trim()
        if (raw.length < MIN_QUERY_LENGTH) return emptyList()
        if (raw.all(Char::isDigit)) {
            return index.map { it.service }
                .filter { it.id.toString().startsWith(raw) }
                .sortedBy { it.id }
                .take(limit)
        }
        val words = normalize(raw).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        return index.asSequence()
            .mapNotNull { entry -> rank(entry.folded, words)?.let { entry.service to it } }
            .sortedWith(compareBy({ it.second }, { it.first.name.length }, { it.first.name }))
            .take(limit)
            .map { it.first }
            .toList()
    }

    fun match(query: String, services: List<ServiceInfo>, limit: Int = LIMIT): List<ServiceInfo> =
        search(query, index(services), limit)

    private fun rank(folded: String, words: List<String>): Int? {
        if (words.any { it !in folded }) return null
        val starts = folded.split(' ')
        return when {
            folded.startsWith(words.first()) -> 0
            words.all { word -> starts.any { it.startsWith(word) } } -> 1
            else -> 2
        }
    }

    fun normalize(text: String): String {
        val builder = StringBuilder(text.length)
        for (char in text.lowercase()) {
            when (char) {
                SOFT_HYPHEN -> Unit
                'ä' -> builder.append('a')
                'ö' -> builder.append('o')
                'ü' -> builder.append('u')
                'ß' -> builder.append("ss")
                else -> builder.append(if (char.isLetterOrDigit()) char else ' ')
            }
        }
        return builder.toString()
            .replace("ae", "a")
            .replace("oe", "o")
            .replace("ue", "u")
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    private const val SOFT_HYPHEN = '­'
}
