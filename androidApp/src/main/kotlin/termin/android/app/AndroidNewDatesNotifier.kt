package termin.android.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import termin.android.domain.MonitorTask
import termin.android.domain.NewDatesNotifier
import termin.android.logDebug

class AndroidNewDatesNotifier(private val context: Context) : NewDatesNotifier {
    private val manager = NotificationManagerCompat.from(context)

    override suspend fun notify(task: MonitorTask, dates: List<String>) {
        if (!manager.areNotificationsEnabled()) {
            logDebug("notify: task=${task.id} skipped, notifications disabled for the app")
            return
        }
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.alert_channel_name))
                .setDescription(context.getString(R.string.alert_channel_description))
                .build(),
        )
        val where = if (task.locations.isEmpty()) {
            context.getString(R.string.alert_all_offices)
        } else {
            context.getString(R.string.alert_offices, task.locations.joinToString(", "))
        }
        val what = dates.joinToString(", ", transform = ::germanDate)
        logDebug("notify: task=${task.id} dates=$dates url=${task.query.calendarUrl}")
        manager.notify(
            NOTIFICATION_ID_BASE + task.id.toInt(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.alert_title, task.serviceId))
                .setContentText("$where · $what")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$where\n$what"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(bookingIntent(task))
                .build(),
        )
    }

    private fun bookingIntent(task: MonitorTask): PendingIntent = PendingIntent.getActivity(
        context,
        task.id.toInt(),
        Intent(Intent.ACTION_VIEW, task.query.calendarUrl.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val CHANNEL_ID = "new-dates"
        const val NOTIFICATION_ID_BASE = 1000
    }
}

private fun germanDate(date: String): String {
    val parts = date.split('-')
    if (parts.size != 3) return date
    val (year, month, day) = parts
    return "$day.$month.$year"
}
