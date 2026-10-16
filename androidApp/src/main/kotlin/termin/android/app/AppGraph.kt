package termin.android.app

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import io.ktor.client.engine.okhttp.OkHttp
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import termin.android.check.CoreTerminChecker
import termin.android.data.DataStoreCheckLog
import termin.android.data.DataStoreTaskRepository
import termin.android.data.StoredStateSerializer
import termin.android.domain.CheckLog
import termin.android.domain.DueMonitorRunner
import termin.android.domain.MonitorRunner
import termin.android.domain.MonitorScheduler
import termin.android.domain.TaskRepository
import terminwatcher.core.api.TerminCore
import kotlin.time.Clock

class AppGraph(context: Context) {
    private val clock: Clock = Clock.System

    private val store = DataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, StoredStateSerializer) {
            context.filesDir.resolve("monitor.json").toOkioPath()
        },
    )

    val tasks: TaskRepository = DataStoreTaskRepository(store)
    val log: CheckLog = DataStoreCheckLog(store)
    val scheduler: MonitorScheduler = AlarmMonitorScheduler(context, tasks, clock)
    val runner: MonitorRunner = DueMonitorRunner(
        tasks = tasks,
        log = log,
        checker = CoreTerminChecker(TerminCore.client(engine = OkHttp), clock),
        clock = clock,
    )
}
