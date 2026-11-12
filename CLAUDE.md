# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EduPlanner is an Android application for educational planning and task management, with a gamification module (EduCoins, levels, achievements, missions, avatar shop, focus sessions, attendance) and a client for the predictive-models API. It uses MVVM with Dagger Hilt, Room, Firebase Auth and the Navigation Component, on XML views (no Compose). Code, comments, UI text and most identifiers are in Spanish; keep that convention.

## Build Commands

- **Build project**: `./gradlew build`
- **Build debug APK**: `./gradlew assembleDebug`
- **Build release APK**: `./gradlew assembleRelease`
- **Run unit tests**: `./gradlew testDebugUnitTest` (one class: `--tests "*PrediccionViewModelTest*"`)
- **Run instrumented tests**: `./gradlew connectedAndroidTest`
- **Clean build**: `./gradlew clean`
- **Install debug APK**: `./gradlew installDebug`

Gradle 8.4 + AGP 8.3 need **JDK 17** (Java 8 is too old and Java 25 is too new). From a terminal, point `JAVA_HOME` at a JDK 17 first: `C:\Users\juane\.jdks\jbr-17.0.14` (in Git Bash: `export JAVA_HOME=/c/Users/juane/.jdks/jbr-17.0.14`).

## Architecture

Layers under `com.softcg.myapplication`:
- `data/` — Room (`database/`), repositories (`Repositories/`), Firebase and HTTP clients (`network/`)
- `domain/` — use cases (`operator fun invoke`), the gamification rules in `domain/gamificacion/` and the API call in `domain/prediccion/`
- `ui/` — Activities, Fragments, ViewModels, Adapters, Dialogs; screen models in `Models/` subfolders
- `di/` — Hilt modules; `core/` — shared helpers (`Event`, `Reloj`, `ex/` extensions)

### MVVM Pattern
- Views: Fragments in `ui/Inicio/Fragments/<Feature>/` with views found by `findViewById` (`viewBinding` is enabled but not used by the fragments).
- ViewModels: `@HiltViewModel` + `@Inject constructor(useCases...)`. Expose `LiveData` backed by a private `MutableLiveData`; one-shot results go through `core/Event` (`getContentIfNotHandled()`); results are sealed classes (`ResultadoX`). Work runs in `viewModelScope.launch`.
- Shared ViewModels are obtained with `activityViewModels()` (e.g. `HomeViewModel`, `InicioViewModel`); screen-local ones with `viewModels()`.

### Key Components

#### Dependency Injection (Dagger Hilt)
- Main application class: `MvvmEduPlannerApp.kt` annotated with `@HiltAndroidApp`
- `di/RoomModule.kt` provides the Room database, DAOs and repositories; `di/GamificacionModule.kt` binds `Reloj` and `SesionUsuario`
- Classes with an `@Inject constructor` (use cases, `FirebaseClient`, `PrediccionService`) need no module

#### Database (Room)
- Main database: `TareasDatabase.kt` (**version 15**); migrations `MIGRATION_13_14` and `MIGRATION_14_15` in `TareasDatabase/Migrations.kt`, plus `fallbackToDestructiveMigration()`
- Entities: `TareaEntity`, `EventoEntity`, `AsignaturaEntity`, `CalificacionEntity`, and the gamification entities in `GamificacionEntities.kt`
- DAOs in `data/database/dao/`; repositories in `data/Repositories/`
- Main-thread queries are allowed (development setting)

#### Navigation Structure
`res/navigation/nav_graph.xml`, start destination `id_home_fragment`:
- **HomeFragment** (`id_home_fragment`): timeline of tasks and events for the selected day
- **AgendaFragment** (`id_diary_fragment`), **RatingsFragment** (`id_ratings_fragment`), **ScheduleFragment** (`id_schedule_fragment`), **AsignaturasFragment** (`id_subjects_fragment`), **HelpFragment** (`id_help_fragment`)
- **ProfileFragment** (`id_profile_fragment`): progress, with tabs (`Perfil/Tabs/`); argument `ARG_PESTANA`
- Sub-screens: **TiendaFragment** (`id_shop_fragment`), **EnfoqueFragment** (`id_focus_fragment`), **PrediccionFragment** (`id_prediction_fragment`)

`InicioActivity` hosts the NavHost, drawer, custom header, bottom bar and floating buttons, and only coordinates. Destinations in its `SUBPANTALLAS` set show a back arrow instead of the hamburger; when adding one, also decide which bottom tab it highlights (`setupBottomNavigation`). Navigation arguments are passed with `bundleOf` and `ARG_*` constants in the destination's companion object (no Safe Args). The add/edit bottom sheets live in `ui/Inicio/Dialogs/` (`TareaDialog`, `EventoDialog`, `CalificacionDialog`, ...). Event editing is opened from `HomeFragment`. Image picking uses `registerForActivityResult` in whichever Activity/Fragment opens `EventoDialog`, which receives the result through `mostrarImagen()`. Shared helpers: `Dialog.showAsBottomSheet()` in `core/ex/DialogEx.kt`, `EditText.showDatePicker()`/`showTimePicker()` in `core/ex/EditTextEx.kt`, and `pintarEtiqueta`/`pintarPrioridad`/`confirmar`/`mostrarAviso` in `core/ex/EstiloEx.kt`.

#### Firebase Integration
- Authentication: Firebase Auth (email/password) through `data/network/FirebaseClient`; `SesionUsuario` gives the current uid (gamification data is stored per uid)
- Project `eduplanner-84c30` (`app/google-services.json`); Analytics enabled

#### Predictive-models API
- Backend: the FastAPI project in `C:\Users\juane\Downloads\modelos_exportados` (Docker on an Oracle Cloud VM behind nginx), base URL `https://softcgeduplanner.duckdns.org` (`PrediccionService.URL_BASE`).
- `data/network/PrediccionService` sends `POST /predict?top_n=` with `HttpURLConnection` on `Dispatchers.IO` (no Retrofit/OkHttp in the project) and the header `Authorization: Bearer <currentUser.getIdToken(false)>`; the API rejects requests without a valid token of the same Firebase project. It returns `RespuestaApi` (`Http(codigo, cuerpo)` / `SinSesion` / `ErrorRed`).
- `domain/prediccion/PredecirTareaUseCase` + `DatosPrediccion` (the 8 model fields, serialized with `org.json` to the API's snake_case names). Categories must be unaccented (`Matematicas`, `Algebra lineal`, `Pomodoro`): accented values don't fail but are ignored by the model.
- Tapping a **task** card in `HomeFragment` opens `PrediccionFragment`, which for now always sends `DatosPrediccion.PRUEBA` (not the task's data) and shows the request JSON and the raw response (HTTP code + JSON). The request is sent once per screen (`consultarSiHaceFalta()`), not again on rotation.
- `AndroidManifest.xml` declares `INTERNET` for this.

### Key Features
- **Task Management**: Create, edit, deliver and grade tasks (tareas)
- **Event Scheduling**: Manage calendar events with notifications
- **Subject Management**: Organize academic subjects (asignaturas)
- **Grade Tracking**: Record and monitor academic grades (calificaciones)
- **Schedule View**: Visual class schedule display
- **Gamification**: EduCoins, levels, achievements, missions, shop and avatar, focus sessions, attendance check (`domain/gamificacion/`)
- **Notifications**: Alarm-based daily reminders (9:00 and 11:30) scheduled by `ui/notifications/NotificationScheduler` and shown by `AlarmNotification`; `EnfoqueNotification` for the end of a focus session

### Design
The UI uses a purple palette (the green theme was retired). Use the named colors in `values/colors.xml` (`purple_primary` #8B5A9F, `purple_accent` #9C27B0, `purple_soft` #E1BEE7, `purple_surface` #F3E5F5, `fondo_app`, `superficie`, `text_primary`/`text_secondary`, and the `estado_exito`/`estado_aviso`/`estado_error` pairs) and the design system in `values/styles.xml` (`Edu.Texto.*`, `Edu.Tarjeta`, `Edu.Boton*`, `Edu.Campo`) instead of inline values. Full screens are a `NestedScrollView` on `@color/fondo_app` with `@dimen/margen_pantalla` side padding and ~96dp bottom padding (the floating button overlaps). Forms follow `sheet_agregar_evento.xml`: outlined `TextInputLayout`s with 16dp corners and a 58dp `MaterialButton`. List cards follow `item_asignatura_modern.xml`.

## Development Guidelines

### Code Structure
- Package structure follows feature-based organization under `com.softcg.myapplication`
- ViewModels are paired with their corresponding Fragments
- Adapters are located within their respective feature folders
- Models/data classes are in `Models/` subfolders within features

### Testing
- Unit tests: `src/test/`; integration tests (Room migrations, view inflation): `src/androidTest/`
- Test frameworks: JUnit 4, Mockito, MockK, and Espresso for UI testing
- ViewModel tests use `InstantTaskExecutorRule` plus `core/MainDispatcherRule` (test sources) so `viewModelScope` runs synchronously; `core/TestUtils.kt` has `RelojFijo`. `unitTests.isReturnDefaultValues = true` lets `android.util.Log` calls pass in JVM tests — but it also means **`org.json` is a stub in JVM tests** (methods return null/defaults), so mock the use case instead of asserting on JSON there.

### Dependencies Management
- Version catalog: `gradle/libs.versions.toml` (all Gradle plugins are declared there: Kotlin 1.9.20, KSP 1.9.20-1.0.14, Hilt 2.50, Google Services). Keep the Kotlin and KSP versions in sync. Most library versions are still declared inline in `app/build.gradle.kts`.
- Key libraries: Room 2.6.1, Hilt 2.50, Firebase BoM 32.8.0, Navigation 2.7.0, Material 1.11.0, Coroutines 1.6.4 (with `kotlinx-coroutines-play-services` for `Task.await()`), ViewPager2
- Minimum SDK: 28, Target SDK: 34, Java/JVM target 1.8
