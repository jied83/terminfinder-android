package de.jied.terminfinder.domain

interface NewDatesNotifier {
    suspend fun notify(task: MonitorTask, dates: List<String>)
}
