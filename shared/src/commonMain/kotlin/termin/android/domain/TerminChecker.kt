package termin.android.domain

interface TerminChecker {
    suspend fun check(task: MonitorTask, knownDates: Set<String>): CheckRecord
}
