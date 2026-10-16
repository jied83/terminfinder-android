package termin.android.check

import termin.android.domain.CheckRecord
import termin.android.domain.MonitorTask
import termin.android.domain.TerminChecker
import terminwatcher.core.api.TerminClient
import kotlin.time.Clock

class CoreTerminChecker(
    private val client: TerminClient,
    private val clock: Clock,
) : TerminChecker {

    override suspend fun check(task: MonitorTask): CheckRecord {
        val result = client.check(task.query)
        return CheckRecord(task.id, clock.now(), result.status, result.dates)
    }
}
