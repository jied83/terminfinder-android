package termin.android.app

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        val wakeLock = requireNotNull(getSystemService<PowerManager>())
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "termin:monitor")
            .apply { acquire(WAKE_LOCK_TIMEOUT.inWholeMilliseconds) }
        scope.launch {
            try {
                graph.scheduler.reschedule()
                graph.runner.runDue()
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.monitor_notification_title))
        .setOngoing(true)
        .build()
        .also {
            NotificationManagerCompat.from(this).createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(getString(R.string.monitor_channel_name))
                    .build(),
            )
        }

    private companion object {
        const val CHANNEL_ID = "monitor"
        const val NOTIFICATION_ID = 1
        val WAKE_LOCK_TIMEOUT = 10.minutes
    }
}
