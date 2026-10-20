package termin.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import termin.android.logDebug

class MonitorAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        logDebug("alarm fired, starting MonitorService")
        ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
    }
}
