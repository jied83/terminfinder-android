package termin.android

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import termin.android.domain.CheckLog
import termin.android.domain.MonitorScheduler
import termin.android.domain.SettingsRepository
import termin.android.domain.TaskRepository
import termin.android.feature.edit.EditScreen
import termin.android.feature.edit.EditViewModel
import termin.android.feature.settings.SettingsScreen
import termin.android.feature.settings.SettingsViewModel
import termin.android.feature.setup.SetupScreen
import termin.android.feature.setup.SetupViewModel
import termin.android.feature.status.StatusScreen
import termin.android.feature.status.StatusViewModel
import termin.android.feature.tasks.TasksScreen
import termin.android.feature.tasks.TasksViewModel
import termin.android.ui.TerminTheme

@Serializable
private data object TasksRoute

@Serializable
private data object SetupRoute

@Serializable
private data class StatusRoute(val taskId: Long)

@Serializable
private data class EditRoute(val taskId: Long)

@Serializable
private data object SettingsRoute

@Composable
fun App(
    tasks: TaskRepository,
    log: CheckLog,
    scheduler: MonitorScheduler,
    settings: SettingsRepository,
) {
    TerminTheme {
        val navController = rememberNavController()
        NavHost(navController, startDestination = TasksRoute) {
            composable<TasksRoute> {
                TasksScreen(
                    store = viewModel { TasksViewModel(tasks, log) },
                    onOpen = { navController.navigate(StatusRoute(it)) },
                    onAdd = { navController.navigate(SetupRoute) },
                    onSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<SetupRoute> {
                SetupScreen(
                    store = viewModel { SetupViewModel(tasks, scheduler) },
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<StatusRoute> { entry ->
                val taskId = entry.toRoute<StatusRoute>().taskId
                StatusScreen(
                    store = viewModel { StatusViewModel(taskId, tasks, log, scheduler) },
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(EditRoute(it)) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    store = viewModel { SettingsViewModel(settings) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EditRoute> { entry ->
                val taskId = entry.toRoute<EditRoute>().taskId
                EditScreen(
                    store = viewModel { EditViewModel(taskId, tasks, scheduler) },
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
