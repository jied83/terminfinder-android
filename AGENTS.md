# AGENTS.md

Guidance for coding agents (and humans) working in this repository.

## Overview

Termin Finder is an Android app that watches [service.berlin.de](https://service.berlin.de) for free
appointment (*Termin*) slots and notifies the user when new dates appear.

Scraping and parsing of the Berlin pages live in the separate
[termin-core](https://github.com/jied83/termin-core) library (JitPack artifact
`com.github.jied83.termin-core:termin-core`, Kotlin package `terminwatcher.core.api`). Do not
re-implement page parsing here; fix it in termin-core and bump the version in
`gradle/libs.versions.toml`.

## Commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew :androidApp:assembleDebug` |
| Install on device/emulator | `./gradlew :androidApp:installDebug` |
| Run all unit tests | `./gradlew :shared:testAndroidHostTest` |
| Run one test class | `./gradlew :shared:testAndroidHostTest --tests "de.jied.terminfinder.feature.SetupViewModelTest"` |
| Run one test method | `./gradlew :shared:testAndroidHostTest --tests "*TicksTest.periodIsGcd*"` |
| Lint | `./gradlew lint` |
| Clean | `./gradlew clean` |
| App logs | `adb logcat -s Termin` |

Test reports: `shared/build/reports/tests/testAndroidHostTest/index.html`.

## Modules and stack

| Module | Role |
|---|---|
| `:shared` | Kotlin Multiplatform library (`com.android.kotlin.multiplatform.library`), currently Android-only. Holds all UI, domain and data code in `commonMain`. |
| `:androidApp` | Thin Android shell: `Application`, `MainActivity`, manual DI (`AppGraph`), alarm scheduling, foreground service, notifications. |

Stack: Kotlin 2.4, AGP 9.3, Compose Multiplatform + Material3, Navigation Compose with type-safe
`@Serializable` routes, DataStore (okio + kotlinx.serialization JSON), Ktor client with the OkHttp
engine, coroutines/Flow. minSdk 26, compile/target SDK 37, JVM 17.

There is **no DI framework**: dependencies are created in `androidApp/.../AppGraph.kt` and passed
into `App(...)`, which builds each ViewModel with `viewModel { ... }`.

All versions live in `gradle/libs.versions.toml`; never hard-code a version in a build script.

## Source layout

`shared/src/commonMain/kotlin/de/jied/terminfinder/`

| Package | Contents |
|---|---|
| `domain/` | Interfaces and pure logic: `TaskRepository`, `CheckLog`, `SettingsRepository`, `ServiceCatalog`, `LocationCatalog`, `TerminChecker`, `MonitorScheduler`, `MonitorRunner`, `NewDatesNotifier`; `DueMonitorRunner`, `Ticks`, `CatalogSearch`, `TaskInput`, models in `Model.kt` |
| `data/` | Implementations: DataStore repositories, cached catalogs, HTTP catalog sources, `StoredState` |
| `check/` | `CoreTerminChecker`, the adapter to termin-core |
| `feature/<name>/` | One package per screen: `tasks`, `setup`, `status`, `edit`, `settings` |
| `mvi/` | The small MVI framework (see below) |
| `ui/` | Shared composables (`CatalogPicker`, `FormField`, `OfficeField`, …), formatting, theme |
| `App.kt` | `NavHost`, routes, ViewModel construction, message wiring |

Platform code: `shared/src/androidMain` (`expect/actual`, e.g. `logDebug`, dynamic color scheme).

Resources: shared strings and icons in `shared/src/commonMain/composeResources`
(generated class `de.jied.terminfinder.resources.Res`); notification/app strings in
`androidApp/src/main/res`. Every string exists in English (`values/`) **and** German (`values-de/`).

## Architecture: MVI

Each feature package follows the same shape:

| File | Contains |
|---|---|
| `XContract.kt` | `XState` (immutable data class), `XIntent`, `XEffect`, `XMessage` sealed interfaces |
| `XViewModel.kt` | `class XViewModel : MviViewModel<XIntent, XState, XEffect, XMessage>` |
| `XEffectHandler.kt` | `class XEffectHandler : EffectHandler<XEffect, XIntent>`; the only place with I/O |
| `XMessages.kt` | `class XMessages : MessageController<XMessage>`; maps messages to navigation callbacks |
| `XScreen.kt` | `@Composable fun XScreen(state, dispatch, ...)`; stateless |

Rules:

- `dispatch(intent)` calls `reduce { ... }`. The reducer is pure and returns a `Transition` built
  with `noEffect`, `perform(effect)` or `send(message)` (see `mvi/Transition.kt`).
- **Effects** run in `viewModelScope` on a background dispatcher. The handler returns a follow-up
  intent (or `null`), which is dispatched back into the reducer. Results that carry a key (query,
  service id) let the reducer drop stale responses.
- A new effect with the same `keyOf(effect)` (default: its class) cancels the running one. This is
  how debounced search works. Always rethrow `CancellationException` in handlers.
- **Messages** are one-shot UI events (navigate, open URL). `App.kt` collects them with
  `store.CollectMessages(XMessages(...))`.
- Screens without side effects use `NoEffects` (see `TasksViewModel`).
- Screens receive only `state` and `dispatch`; they never touch repositories.
- ViewModels that run effects take `background: CoroutineDispatcher = Dispatchers.Default` so tests
  can inject a test dispatcher.

Adding a screen:

1. Create `feature/<name>/` with the files above.
2. Add a `@Serializable` route and a `composable<Route>` block in `App.kt`.
3. Add strings in both `values/` and `values-de/`.
4. Add `<Name>ViewModelTest` in `shared/src/commonTest/.../feature/`.

## Background monitoring

```
AlarmMonitorScheduler ──exact alarm──▶ MonitorAlarmReceiver
        ▲                                     │
        │                                     ▼
        └──── reschedule() ◀──── MonitorService (foreground, dataSync, wake lock)
                                              │
                                              ▼
                                 DueMonitorRunner.runDue()
```

- One alarm fires every `Ticks.period(...)` minutes: the GCD of all task intervals
  (minimum `Ticks.MIN_MINUTES` = 2; intervals must be multiples of it).
- `runDue()` is guarded by a mutex. It checks each due task through `TerminChecker`, appends a
  `CheckRecord` to `CheckLog`, stores a service name it learns, and calls `NewDatesNotifier`
  when new dates appear and notifications are enabled in settings.
- Any code that adds, edits or deletes a task must call `MonitorScheduler.reschedule()`.
- Without the exact-alarm permission no alarm is scheduled; this is logged, not thrown.

## Persistence

- `monitor.json`: one DataStore holding `StoredState` (tasks, check records, known dates,
  settings). `DataStoreTaskRepository`, `DataStoreCheckLog` and `DataStoreSettingsRepository` all
  share it.
- `services.json` and `locations.json`: catalog caches used by `CachedServiceCatalog` and
  `CachedLocationCatalog`.
- There are no migrations. JSON is read with `ignoreUnknownKeys = true`, so give every new field
  in a `@Serializable` model a default value and never rename existing fields.

## Testing

- Tests live in `shared/src/commonTest` and use `kotlin.test` plus `kotlinx-coroutines-test`.
- They run as Android host (JVM) tests with `isReturnDefaultValues = true`. There are no
  instrumented or Robolectric tests; keep logic out of `androidApp` so it stays testable.
- **No mocking library.** Use the hand-written fakes in `commonTest/.../Fakes.kt`: `FakeTasks`,
  `FakeLog`, `FakeSettings`, `FakeCatalog`, `FakeLocationCatalog`, `FakeNotifier`,
  `FakeScheduler`, `FakeClock`, `FakeChecker`, and the `task(id, intervalMinutes)` builder.
  Extend or add a fake there when a new interface appears.
- ViewModel tests extend `MainDispatcherTest`, which sets `Dispatchers.Main` to an
  `UnconfinedTestDispatcher`. Pass its `dispatcher` as the ViewModel's `background` dispatcher so
  effects finish inside `dispatch()` and you can assert on `state.value` right away.
- Pass `Duration.ZERO` as the debounce to `SetupEffectHandler` in tests.
- Assert messages with `viewModel.messages.first()` inside `runTest`.
- Pure logic (`Ticks`, `CatalogSearch`, `TaskInput`, catalog parsers) gets plain unit tests.
- Any change to a reducer, effect handler, repository or `DueMonitorRunner` needs a test.

Example:

```kotlin
class TasksViewModelTest : MainDispatcherTest() {
    @Test
    fun intentsBecomeNavigationEffects() = runTest {
        val viewModel = TasksViewModel(FakeTasks(), FakeLog())
        viewModel.dispatch(TasksIntent.Open(7))
        assertEquals(TasksMessage.ToStatus(7), viewModel.messages.first())
    }
}
```

## Conventions

- Package root `de.jied.terminfinder`; app module `de.jied.terminfinder.app`.
- Keep `commonMain` free of Android APIs so an iOS target can be added later. Put platform code
  in `androidMain` behind `expect/actual`, or in `androidApp` behind a `domain` interface.
- Use `logDebug(...)` for logging (tag `Termin`), not `println` or `android.util.Log` directly.
- Use `kotlin.time.Clock` and `Instant` (inject `Clock`; use `FakeClock` in tests).
- Official Kotlin code style (`kotlin.code.style=official`), trailing commas, no wildcard imports.
- Comments explain *why*, not *what*.
- Before finishing a change, run `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`.

## Releases

Releases are built by the manual GitHub Actions workflow `.github/workflows/release.yml`
(Actions → Release → Run workflow, or `gh workflow run release.yml`). It:

1. Adds 100 to `versionCode` in `version.properties` (the *build* segment of
   `api-version-build-variant`). Change `versionName` there by hand.
2. Runs the unit tests and builds a signed APK and AAB.
3. Only if that succeeds, commits the bump to `main`, tags `v{versionCode}`, and publishes a
   GitHub Release with both files attached.

Required repo secrets: `KEYSTORE_BASE64` (`base64 -i release.jks`), `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Gradle reads them from the `ANDROID_KEYSTORE_PATH`,
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` environment variables;
without these, a local `assembleRelease` is unsigned. Never commit a keystore. Pull after each release, since the
workflow pushes to `main`.
