package de.jied.terminfinder.domain

import kotlinx.datetime.LocalDate
import terminwatcher.core.api.BookableDay
import terminwatcher.core.api.DateFilter

object DateMatch {

    fun filters(selection: DateSelection): List<DateFilter> = if (selection.isRange) {
        listOf(DateFilter.of(selection.from, selection.to, emptySet()))
    } else {
        selection.days.map { DateFilter.of(it, it, emptySet()) }
    }

    fun wanted(days: List<BookableDay>, selection: DateSelection, today: LocalDate): List<String> =
        filters(selection)
            .flatMap { it.matches(days, today) }
            .mapNotNull { it.isoDate }
            .distinct()
            .sorted()
}
