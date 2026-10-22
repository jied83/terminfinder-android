package de.jied.terminfinder.domain

interface MonitorScheduler {
    suspend fun reschedule()
}
