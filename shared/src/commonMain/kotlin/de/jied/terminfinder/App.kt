package de.jied.terminfinder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import de.jied.terminfinder.domain.CheckLog
import de.jied.terminfinder.domain.LocationCatalog
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.ServiceCatalog
import de.jied.terminfinder.domain.SettingsRepository
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.feature.edit.EditEffectHandler
import de.jied.terminfinder.feature.edit.EditMessages
import de.jied.terminfinder.feature.edit.EditScreen
import de.jied.terminfinder.feature.edit.EditViewModel
import de.jied.terminfinder.feature.settings.SettingsEffectHandler
import de.jied.terminfinder.feature.settings.SettingsScreen
import de.jied.terminfinder.feature.settings.SettingsViewModel
import de.jied.terminfinder.feature.setup.SetupEffectHandler
import de.jied.terminfinder.feature.setup.SetupMessages
import de.jied.terminfinder.feature.setup.SetupScreen
import de.jied.terminfinder.feature.setup.SetupViewModel
import de.jied.terminfinder.feature.status.StatusEffectHandler
import de.jied.terminfinder.feature.status.StatusMessages
import de.jied.terminfinder.feature.status.StatusScreen
import de.jied.terminfinder.feature.status.StatusViewModel
import de.jied.terminfinder.feature.tasks.TasksMessages
import de.jied.terminfinder.feature.tasks.TasksScreen
import de.jied.terminfinder.feature.tasks.TasksViewModel
import de.jied.terminfinder.mvi.CollectMessages
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
    catalog: ServiceCatalog,
    locations: LocationCatalog,
) {
    TerminTheme {
        val navController = rememberNavController()
        NavHost(navController, startDestination = TasksRoute) {
            composable<TasksRoute> {
                val store = viewModel { TasksViewModel(tasks, log) }
                val state by store.state.collectAsStateWithLifecycle()
                store.CollectMessages(
                    remember(navController) {
                        TasksMessages(
                            onOpen = { navController.navigate(StatusRoute(it)) },
                            onAdd = { navController.navigate(SetupRoute) },
                            onSettings = { navController.navigate(SettingsRoute) },
                        )
                    },
                )
                TasksScreen(state, store::dispatch)
            }
            composable<SetupRoute> {
                val store = viewModel { SetupViewModel(SetupEffectHandler(tasks, scheduler, catalog, locations)) }
                val state by store.state.collectAsStateWithLifecycle()
                store.CollectMessages(
                    remember(navController) { SetupMessages(onDone = { navController.popBackStack() }) },
                )
                SetupScreen(state, store::dispatch, onBack = { navController.popBackStack() })
            }
            composable<StatusRoute> { entry ->
                val taskId = entry.toRoute<StatusRoute>().taskId
                val store = viewModel { StatusViewModel(taskId, tasks, log, StatusEffectHandler(tasks, scheduler)) }
                val state by store.state.collectAsStateWithLifecycle()
                val uriHandler = LocalUriHandler.current
                store.CollectMessages(
                    remember(navController, uriHandler) {
                        StatusMessages(
                            onOpenUrl = uriHandler::openUri,
                            onEdit = { navController.navigate(EditRoute(it)) },
                            onDeleted = { navController.popBackStack() },
                        )
                    },
                )
                StatusScreen(state, store::dispatch, onBack = { navController.popBackStack() })
            }
            composable<SettingsRoute> {
                val store = viewModel { SettingsViewModel(settings, SettingsEffectHandler(settings)) }
                val state by store.state.collectAsStateWithLifecycle()
                SettingsScreen(state, store::dispatch, onBack = { navController.popBackStack() })
            }
            composable<EditRoute> { entry ->
                val taskId = entry.toRoute<EditRoute>().taskId
                val store = viewModel { EditViewModel(taskId, EditEffectHandler(tasks, scheduler, locations)) }
                val state by store.state.collectAsStateWithLifecycle()
                store.CollectMessages(
                    remember(navController) { EditMessages(onDone = { navController.popBackStack() }) },
                )
                EditScreen(state, store::dispatch, onBack = { navController.popBackStack() })
            }
        }
    }
}
