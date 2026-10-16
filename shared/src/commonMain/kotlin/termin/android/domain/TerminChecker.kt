package termin.android.domain

interface TerminChecker {
    suspend fun check(task: MonitorTask): CheckRecord
}
