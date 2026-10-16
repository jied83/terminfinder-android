package termin.android.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.first
import termin.android.domain.MonitorScheduler
import termin.android.domain.TaskRepository
import termin.android.domain.Ticks
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class AlarmMonitorScheduler(
    private val context: Context,
    private val tasks: TaskRepository,
    private val clock: Clock,
) : MonitorScheduler {
    private val alarmManager = requireNotNull(context.getSystemService<AlarmManager>())

    override suspend fun reschedule() {
        val period = Ticks.period(tasks.observeAll().first().map { it.intervalMinutes })
        val alarm = alarmIntent()
        when {
            period == null -> alarmManager.cancel(alarm)
            AlarmManagerCompat.canScheduleExactAlarms(alarmManager) -> AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                (clock.now() + period.minutes).toEpochMilliseconds(),
                alarm,
            )
        }
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, MonitorAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
