package termin.android.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import termin.android.App

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(RequestPermission()) { requestExactAlarms() }
    private val exactAlarmAccess = registerForActivityResult(StartActivityForResult()) { requestBatteryExemption() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(graph.tasks, graph.log, graph.scheduler) }
        if (savedInstanceState == null) requestNotifications()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { graph.scheduler.reschedule() }
    }

    private fun requestNotifications() {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) requestExactAlarms() else notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestExactAlarms() {
        val alarmManager = requireNotNull(getSystemService<AlarmManager>())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || AlarmManagerCompat.canScheduleExactAlarms(alarmManager)) {
            requestBatteryExemption()
        } else {
            exactAlarmAccess.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()))
        }
    }

    private fun requestBatteryExemption() {
        if (!requireNotNull(getSystemService<PowerManager>()).isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()))
        }
    }

    private fun packageUri() = "package:$packageName".toUri()
}
