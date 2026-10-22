package de.jied.terminfinder

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import de.jied.terminfinder.domain.CheckLog
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.SettingsRepository
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.feature.edit.EditScreen
import de.jied.terminfinder.feature.edit.EditViewModel
import de.jied.terminfinder.feature.settings.SettingsScreen
import de.jied.terminfinder.feature.settings.SettingsViewModel
import de.jied.terminfinder.feature.setup.SetupScreen
import de.jied.terminfinder.feature.setup.SetupViewModel
import de.jied.terminfinder.feature.status.StatusScreen
import de.jied.terminfinder.feature.status.StatusViewModel
import de.jied.terminfinder.feature.tasks.TasksScreen
import de.jied.terminfinder.feature.tasks.TasksViewModel
import de.jied.terminfinder.ui.TerminTheme

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
