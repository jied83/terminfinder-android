package de.jied.terminfinder.app

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import io.ktor.client.engine.okhttp.OkHttp
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import de.jied.terminfinder.check.CoreTerminChecker
import de.jied.terminfinder.data.DataStoreCheckLog
import de.jied.terminfinder.data.DataStoreSettingsRepository
import de.jied.terminfinder.data.DataStoreTaskRepository
import de.jied.terminfinder.data.StoredStateSerializer
import de.jied.terminfinder.domain.CheckLog
import de.jied.terminfinder.domain.DueMonitorRunner
import de.jied.terminfinder.domain.MonitorRunner
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.SettingsRepository
import de.jied.terminfinder.domain.TaskRepository
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
    val settings: SettingsRepository = DataStoreSettingsRepository(store)
    val scheduler: MonitorScheduler = AlarmMonitorScheduler(context, tasks, clock)
    val runner: MonitorRunner = DueMonitorRunner(
        tasks = tasks,
        log = log,
        checker = CoreTerminChecker(TerminCore.client(engine = OkHttp), clock),
        clock = clock,
        settings = settings,
        notifier = AndroidNewDatesNotifier(context),
    )
}
