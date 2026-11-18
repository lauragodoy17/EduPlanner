# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EduPlanner is an Android application for educational planning and task management. The app uses MVVM architecture with modern Android development practices including Dagger Hilt for dependency injection, Room for local database, Firebase for authentication, and Navigation Component for UI navigation.

## Build Commands

- **Build project**: `./gradlew build`
- **Build debug APK**: `./gradlew assembleDebug`
- **Build release APK**: `./gradlew assembleRelease`
- **Run unit tests**: `./gradlew test`
- **Run instrumented tests**: `./gradlew connectedAndroidTest`
- **Clean build**: `./gradlew clean`
- **Install debug APK**: `./gradlew installDebug`

Gradle 8.4 + AGP 8.3 need **JDK 17** (Java 8 is too old and Java 25 is too new). From a terminal, point `JAVA_HOME` at a JDK 17 first, e.g. `C:\Users\laura\.jdks\jbr-17.0.14`.

## Architecture

### MVVM Pattern
The app follows MVVM (Model-View-ViewModel) architecture:
- **Models**: Data entities in `data/database/entities/`
- **Views**: Fragments in `ui/Inicio/Fragments/`
- **ViewModels**: Business logic in corresponding ViewModel classes

### Key Components

#### Dependency Injection (Dagger Hilt)
- Main application class: `MvvmEduPlannerApp.kt` annotated with `@HiltAndroidApp`
- DI configuration: `di/RoomModule.kt` provides Room database and repository dependencies
- All ViewModels and repositories use `@Inject` constructor injection

#### Database (Room)
- Main database: `TareasDatabase.kt` (version 13)
- Entities: `TareaEntity`, `EventoEntity`, `AsignaturaEntity`, `CalificacionEntity`
- DAOs: Separate DAO classes for each entity in `data/database/dao/`
- Repositories: Repository pattern implementation in `data/Repositories/`

#### Navigation Structure
The app uses Navigation Component with 6 main fragments:
- **HomeFragment** (`id_home_fragment`): Main dashboard with tasks and events
- **AgendaFragment** (`id_diary_fragment`): Calendar and agenda view
- **RatingsFragment** (`id_ratings_fragment`): Grades/calificaciones management
- **ScheduleFragment** (`id_schedule_fragment`): Class schedule (horario)
- **AsignaturasFragment** (`id_subjects_fragment`): Subject management
- **HelpFragment** (`id_help_fragment`): Help and feedback

`InicioActivity` hosts the NavHost, drawer, bottom bar and floating buttons, and only coordinates. The add/edit bottom sheets live in `ui/Inicio/Dialogs/` (`TareaDialog`, `EventoDialog`, `CalificacionDialog`). Event editing is opened from `HomeFragment`. Image picking uses `registerForActivityResult` in whichever Activity/Fragment opens `EventoDialog`, which receives the result through `mostrarImagen()`. Shared helpers: `Dialog.showAsBottomSheet()` in `core/ex/DialogEx.kt`, and `EditText.showDatePicker()`/`showTimePicker()` in `core/ex/EditTextEx.kt`.

#### Firebase Integration
- Authentication: Firebase Auth for user login/registration
- Analytics: Firebase Analytics enabled
- Configuration: `google-services.json` in app module

### Key Features
- **Task Management**: Create, edit, and track tasks (tareas)
- **Event Scheduling**: Manage calendar events with notifications
- **Subject Management**: Organize academic subjects (asignaturas)
- **Grade Tracking**: Record and monitor academic grades (calificaciones)
- **Schedule View**: Visual class schedule display
- **Notifications**: Alarm-based daily reminders (9:00 and 11:30) scheduled by `ui/notifications/NotificationScheduler` and shown by `AlarmNotification`

### Design
The UI uses a purple palette (the green theme was retired). Use the named colors in `values/colors.xml`: `purple_primary` (#8B5A9F), `purple_accent` (#9C27B0), `purple_soft` (#E1BEE7) and `purple_surface` (#F3E5F5). Forms follow `sheet_agregar_evento.xml`: outlined `TextInputLayout`s with 16dp corners and a 58dp `MaterialButton`. List cards follow `item_asignatura_modern.xml`.

## Development Guidelines

### Code Structure
- Package structure follows feature-based organization under `com.softcg.myapplication`
- ViewModels are paired with their corresponding Fragments
- Adapters are located within their respective feature folders
- Models/data classes are in `Models/` subfolders within features

### Testing
- Unit tests: Located in `src/test/`
- Integration tests: Located in `src/androidTest/`
- Test frameworks: JUnit 4, Mockito, MockK, and Espresso for UI testing
- ViewModel tests use `InstantTaskExecutorRule` plus `core/MainDispatcherRule` (test sources) so `viewModelScope` runs synchronously. `unitTests.isReturnDefaultValues = true` lets `android.util.Log` calls pass in JVM tests

### Dependencies Management
- Version catalog: `gradle/libs.versions.toml` (all Gradle plugins are declared there: Kotlin 1.9.20, KSP 1.9.20-1.0.14, Hilt 2.50, Google Services). Keep the Kotlin and KSP versions in sync.
- Key libraries: Room, Hilt, Firebase, Navigation Component, Material Design, Coroutines
- Minimum SDK: 28, Target SDK: 34

### Database Migrations
- Current database version: 13
- Database allows main thread queries (configured for development)
- Uses `fallbackToDestructiveMigration()` for version conflicts