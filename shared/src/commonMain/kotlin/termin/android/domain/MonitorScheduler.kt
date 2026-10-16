package termin.android.domain

interface MonitorScheduler {
    suspend fun reschedule()
}
