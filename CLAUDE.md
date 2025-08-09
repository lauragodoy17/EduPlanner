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
- Main database: `TareasDatabase.kt` (version 9)
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
- **Notifications**: Alarm-based notifications for tasks and events

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

### Dependencies Management
- Version catalog: `gradle/libs.versions.toml`
- Key libraries: Room, Hilt, Firebase, Navigation Component, Material Design, Coroutines
- Minimum SDK: 28, Target SDK: 34

### Database Migrations
- Current database version: 9
- Database allows main thread queries (configured for development)
- Uses `fallbackToDestructiveMigration()` for version conflicts