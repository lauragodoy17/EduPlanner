# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EduPlanner is an Android application for educational planning and task management, aimed at students. Besides tasks, events, subjects, grades and schedule, it has a **gamification module** (EduCoins, XP/levels, streaks, achievements, missions, shop and avatar, focus sessions, class attendance), a **predictive model** (remote API that recommends study methods) and **AI recommendations** (Gemini via Firebase AI Logic). Code, identifiers, comments and UI strings are in **Spanish**; keep new code consistent with that.

Stack: MVVM, Dagger Hilt, Room, Firebase (Auth, Analytics, AI Logic, App Check), Navigation Component, ViewBinding, Coroutines + LiveData.

## Build Commands

- **Build project**: `./gradlew build`
- **Build debug APK**: `./gradlew assembleDebug`
- **Run unit tests**: `./gradlew test` (single class: `./gradlew testDebugUnitTest --tests "com.softcg.myapplication.domain.gamificacion.TiendaUseCasesTest"`)
- **Run instrumented tests**: `./gradlew connectedAndroidTest` (needs a device/emulator; **uninstalls the app afterwards**, see Testing). To run one class without touching the installed app's data: `./gradlew assembleDebugAndroidTest`, `adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`, `adb shell am instrument -w -e class com.softcg.myapplication.VistasInflanTest com.softcg.myapplication.test/androidx.test.runner.AndroidJUnitRunner`, then `adb uninstall com.softcg.myapplication.test` (in Git Bash prefix `MSYS_NO_PATHCONV=1`).
- **Clean build**: `./gradlew clean`
- **Install debug APK**: `./gradlew installDebug`

Gradle 8.4 + AGP 8.3 need **JDK 17** (Java 8 is too old and Java 25 is too new). On this machine it is at `C:\Users\juane\.jdks\jbr-17.0.14` (Android Studio's bundled JBR in `C:\Program Files\Android\Android Studio\jbr` also works). `.claude/settings.local.json` sets `JAVA_HOME` for Claude Code sessions.

## Architecture

### Layers
- **UI** (`ui/`): Activities, Fragments, Dialogs, ViewModels, Adapters. ViewModels are `@HiltViewModel` and expose LiveData; one-shot UI events use `core/Event.kt`.
- **Domain** (`domain/`): use cases with `@Inject constructor` and `operator fun invoke`. Pure rule/config objects (e.g. `RachaReglas`, `NivelesConfig`) hold the business logic so it can be unit-tested without Android.
  - `domain/gamificacion/`: rewards, levels, streak, achievements, missions, shop, focus, attendance.
  - `domain/prediccion/`: builds the request for the predictive API and parses its result.
  - `domain/ia/`: AI task recommendation (combines prediction + Gemini + source verification).
  - Root: login, create account, get tasks.
- **Data** (`data/`): Room entities/DAOs (`data/database/`), repositories (`data/Repositories/`), network clients (`data/network/`).

### Dependency Injection (Dagger Hilt)
- Application class: `MvvmEduPlannerApp.kt` (`@HiltAndroidApp`); it also initializes Firebase App Check.
- `di/RoomModule.kt`: database and DAOs. Repositories have `@Inject constructor` and need no module.
- `di/GamificacionModule.kt`: binds `Reloj` → `RelojSistema` and `SesionUsuario` → `FirebaseSesionUsuario`.
- `di/IaModule.kt`: binds `GeneradorRecomendaciones` → `GeminiRecomendaciones`, `VerificadorFuentes` → `VerificadorFuentesWeb`, `ClienteHttp` → `ClienteHttpUrlConnection`.
- Inject `core/Reloj` instead of calling `now()` and `SesionUsuario` instead of touching FirebaseAuth directly, so tests can replace them.

### Database (Room)
- Database: `data/database/TareasDatabase/TareasDatabase.kt`, **version 17**, `exportSchema = false`.
- Entities: `TareaEntity`, `EventoEntity`, `AsignaturaEntity`, `CalificacionEntity`, `PercepcionEntity`, and the gamification entities in `GamificacionEntities.kt` (`MovimientoEduCoinsEntity`, `AsistenciaEntity`, `InventarioEntity`, `AvatarEquipadoEntity`, `AvatarAparienciaEntity`, `MisionAsignadaEntity`, `SesionEnfoqueEntity`).
- DAOs: `TareasDao`, `EventosDao`, `AsignaturasDao`, `CalificacionesDao`, `GamificacionDao`, `PercepcionesDao`.
- **All data is per user** (`uid` column). Rows created before v16 have `uid = ''`; `data/database/DuenoDatosLocales` lets the first user who signs in adopt them. Always filter queries by `uid`.
- Main-thread queries are allowed (`allowMainThreadQueries()`), with `fallbackToDestructiveMigration()` as a last resort.

### Database Migrations
- Real migrations live in `data/database/TareasDatabase/Migrations.kt` (`MIGRATION_13_14` … `MIGRATION_16_17`; 17 adds the optional task delivery time `hora_entrega`) and are registered in `RoomModule`. User data must survive upgrades: **do not rely on the destructive fallback**.
- When changing the schema: bump the version, add a `MIGRATION_X_Y` whose SQL matches exactly what Room expects for the entity (column types, NOT NULL, defaults, indices), register it in `RoomModule`, and extend `androidTest/.../data/MigrationTest.kt` (it builds a v13 database and migrates it to the latest version).

### Gamification
- EduCoins are an append-only ledger (`educoins_movimientos`); the balance is the sum. Each movement has a unique key `"<TIPO>:<referencia>"` per user, so the same activity is never rewarded twice, and `TipoRecompensa.topeDiario` caps rewards per day.
- `RecompensasConfig.kt` (`TipoRecompensa`) is the single source of reward values; `NivelesConfig.kt` of levels (1 XP per EduCoin earned; spending never lowers the level). Levels, streak and achievements have no tables: they are derived from the movements.
- Use `OtorgarRecompensaUseCase` to grant rewards; inside an existing transaction use its `...EnTransaccion` variants through `GamificacionRepository.enTransaccion { }`.
- Catalogs: `LogrosCatalogo`, `MisionesConfig`, `AccesoriosCatalogo`, `MensajesMotivacion`.
- `InicioActivity` shows the coin counter, queues celebrations (level up, achievements) and periodically checks class attendance (`AsistenciaDialog`).

### Grade scales
- `domain/calificaciones/EscalaCalificacion` is the single source of how a grade is entered, validated (`contiene`, `ajustar`), shown (`formatear`, `EscalaCalificacion.texto`) and converted for the model (`a10`). Students can only pick `TipoEscala.ELEGIBLES` (0-10, 1-5, 0-100 %, letras A-F, aprobado/reprobado); `PERSONALIZADA` exists only to read legacy subjects with their own range. Grades are stored as numbers: letters F..A = 0..4, aprobado = 1 / reprobado = 0.
- `grade_avg` (`ReglasExtraccion.notas`) averages each grade converted with its own scale by `a10`: numeric scales use `nota / máximo × 10` (3 of 5 = 6 of 10 = 60 %), letters and aprobado/reprobado the midpoint of their usual percentage band (A 9.5, B 8.5, C 7.5, D 6.5, F 3.0; aprobado 8.0, reprobado 3.0). Values are read from their decimal text to avoid Float error.
- The scale is chosen **only when the subject is created** (`include_selector_escala.xml` + `Dialogs/SelectorEscala` in `AsignaturaDialog`) and stored as `tipo_calificacion` = `etiquetaGuardada` plus `escala_min`/`escala_max` (`AsignaturasRepository.guardarEscala`). `Asignatura.escala` reads it back; legacy subjects without a reliable scale return null.
- Once a subject has at least one grade the scale is locked (`EscalaBloqueadaUseCase`; `EditarAsignaturaUseCase` returns `ResultadoEdicion.EscalaBloqueada`, the edit form shows it disabled). A legacy subject with no scale can receive one once (`GuardarEscalaAsignaturaUseCase`), asked inline in the grade sheets or in `DatosAsignaturaDialog`.
- Grade input is `include_entrada_calificacion.xml` + `Dialogs/EntradaCalificacion` (slider with − / + for numeric scales, chips for letters and aprobado/reprobado), used by `CalificacionDialog` (subject first, then the input adapts) and `CalificarTareaDialog`. `CalificarTareaUseCase` also rejects values outside the subject's scale. Repositories attach the scale when reading (`Calificacion.escala`/`textoValor`, `Tarea.escalaNota`/`textoCalificacion`) so every screen shows "4,5", "85 %", "A" or "Aprobado".

### Predictive model and AI
- `data/network/PrediccionService`: `POST https://softcgeduplanner.duckdns.org/predict?top_n=N` with the user's Firebase ID token as Bearer. Results are a `RespuestaApi` sealed class (no exceptions).
- `domain/prediccion/CatalogoModelo` mirrors the categories the model was trained on (from the API's `GET /metadata`), **without accents** ("Matematicas"). Only send values from this catalog; if the model is retrained, update it.
- Subject area (`area_modelo`) is detected from the subject name by `CatalogoModelo.detectarArea()` (`DeteccionArea.Unica` / `Ambigua` / `Ninguna`). Only `Unica` is saved automatically: on creation (`CrearAsignaturaUseCase`) and, for older subjects, at startup (`DetectarAreasPendientesUseCase`, run from `InicioViewModel.init`). Ambiguous or unmatched names are asked: `AreaAsignaturaDialog` in the task flow, `DatosAsignaturaDialog` elsewhere; texts in `TextosArea`. To recognize more names, extend `PALABRAS_POR_AREA`.
- "Datos para predicciones" (`ExtraerDatosPrediccionUseCase` → `EstadoDato` per `ItemDato`) shows at most: AREA (only if missing), TEMA (only if the task has no title), NOTAS (no button: "registra más notas…"; `AccionDato.ESCALA` only when notes lack a scale), ESTUDIO (hours/week + method, one item; `EstudioDeclaradoDialog` asks them separately) and PERCEPCION (interest + difficulty, one item; `InteresDificultadDialog`). Attendance is never asked: it is assumed 100 % until `ReglasExtraccion.MIN_CLASES` answers (`DatosPrediccion.asistenciaSupuesta`, not sent to the API and hidden from the Gemini prompt). Declared hours (`TIPO_HORAS` percepción) count until there are 14 days of study sessions. Declarations are saved by `GuardarPercepcionesUseCase`; `GuardarDatosAsignaturaUseCase` is only for area and grade scale.
- `ExtraerDatosPrediccionUseCase` builds `DatosPrediccion` from the student's real data (subjects with `area_modelo`, study sessions, attendance, `percepciones`); `DatosEstudioUseCases`/`DatosEstudioDialogs` collect the missing ones.
- `data/network/GeminiRecomendaciones`: Gemini through Firebase AI Logic (`firebase-ai:16.2.0`, pinned outside the BoM because 17+ requires Kotlin 2) with a JSON response schema. The method must be one of `CatalogoModelo.METODOS_RECOMENDABLES`; when there is a prediction Gemini cannot change it. Suggested web sources are checked by `VerificadorFuentesWeb`.
- Recommendations appear on the task screen (`PrediccionFragment`) **only when the student presses "Generar recomendaciones"** (opening the screen never calls the API or Gemini, to avoid spending AI credits; a recommendation already generated for the task is shown from `RecomendarTareaUseCase.enCache`): after `POST /predict` the ViewModel turns the response into a `ContextoPrediccion` with `RecomendarTareaUseCase.contextoDeRespuesta()` and calls `conContexto()`, so the API is not called twice; the button then asks for another plan. They are painted in `include_recomendacion_ia.xml` by `pintarRecomendacion()` (`Dialogs/RecomendacionIa.kt`).
- **App Check is required** by AI Logic. `AppCheckProveedor` has a `debug` (debug provider; register the token printed in Logcat under "DebugAppCheckProvider" in the Firebase console) and a `release` (Play Integrity) variant in `src/debug/` and `src/release/`.

### Navigation Structure
Activities: `PrincipalSplash` (launcher) → `ui/login/MainActivity` / `ui/register/RegisterActivity` → `ui/Inicio/InicioActivity`.

`InicioActivity` hosts the NavHost (`res/navigation/nav_graph.xml`), drawer (`res/menu/menu.xml`: the six main destinations, then a single divider and the "Opciones" submenu with Tema de la app, Ayuda y opiniones and Cerrar sesión), bottom bar and floating buttons, and only coordinates. Destinations:
- **HomeFragment** (`id_home_fragment`): dashboard with tasks and events.
- **AgendaFragment** (`id_diary_fragment`): calendar and agenda.
- **ScheduleFragment** (`id_schedule_fragment`): class schedule (horario); tapping a class card opens its subject in `DetalleFragment`.
- **RatingsFragment** (`id_ratings_fragment`): grades.
- **AsignaturasFragment** (`id_subjects_fragment`): subjects.
- **ProfileFragment** (`id_profile_fragment`): progress hub with ViewPager2 tabs (`Perfil/Tabs/`: Estadísticas, Misiones, Logros, Avatar).
- **TiendaFragment** (`id_shop_fragment`), **EnfoqueFragment** (`id_focus_fragment`), **PrediccionFragment** (`id_prediction_fragment`, titled "Tarea": opened from a task card, shows the task info and a "Predicción y recomendaciones" section): sub-screens (`InicioActivity.SUBPANTALLAS`). Task info: `include_info_tarea.xml` + `pintarInfoTarea()` in `Dialogs/InfoTarea.kt`; below it, Entregar (Calificar once delivered) through `AccionesTarea`. "Ver más" in the card menus opens this screen too.
- **DetalleFragment** (`id_detail_fragment`, `Fragments/Detalle/`): screen of an event, subject or grade, opened by tapping its card (`abrirDetalle(TipoDetalle, id)`; tasks use `abrirTarea()`). The header title comes from `TipoDetalle.pantalla` and the highlighted tab is the one it was opened from. `FichaDetalle` builds the label → value rows. Both item screens have edit / delete / share icons top-right (`include_acciones_detalle.xml` + `configurarAccionesDetalle()`).
- **HelpFragment** (`id_help_fragment`): help and feedback.

Bottom sheets and dialogs live in `ui/Inicio/Dialogs/` (`TareaDialog`, `AsignaturaDialog`, `EventoDialog`, `CalificacionDialog`, `CalificarTareaDialog`, `AsistenciaDialog`, `ConfirmarCompraDialog`, `LogroDialog`, `SubidaNivelDialog`, `RutaNivelesDialog`, `DatosEstudioDialogs`). Every task belongs to a subject: `TareaDialog` only saves a subject from the list, and its last option "+ Agregar asignatura" hides the task form, opens `AsignaturaDialog` and returns with the new subject selected (`InicioViewModel.onCrearAsignaturaDesdeTarea` → `asignaturaParaTarea`). The form dialogs also edit when given an item (`TareaDialog(tarea=)`, `AsignaturaDialog(asignatura=)`, `CalificacionDialog(calificacion=)`, `EventoDialog(evento=)`). Edits go through `domain/EditarUseCases.kt`: `EditarTareaUseCase` (a graded task's grade follows it to a new subject) and `EditarAsignaturaUseCase` (rejects duplicate names; renaming updates tasks, grades and study sessions that reference the old name, in one transaction). Subjects are created through `domain/CrearAsignaturaUseCase` (insert + class reward) from both Asignaturas and the task form. Image picking uses `registerForActivityResult` in whichever Activity/Fragment opens `EventoDialog`, which receives the result through `mostrarImagen()`. Shared helpers: `Dialog.showAsBottomSheet()` in `core/ex/DialogEx.kt`, `EditText.showDatePicker()`/`showTimePicker()` in `core/ex/EditTextEx.kt`, plus `EstiloEx`, `RecompensaEx`, `ViewEx`.

Custom views for progress: `Perfil/AvatarView`, `Perfil/Views/` (`ActividadHeatmapView`, `AnilloProgresoView`, `BarrasSemanaView`, `ConfettiView`).

### Sharing by QR
- `domain/compartir/`: `ContenidoCompartido` (task, event, subject or grade, without ids, state, perceptions or event image), `CodigoQr` (format `EDUPLANNER:<version>:<Base64 URL-safe JSON>`; `leer()` validates and clamps everything, returns `LecturaQr`), `ImportarCompartidoUseCase` (`analizar()` matches the subject by name ignoring accents/case; `importar()` with an optional `DestinoAsignatura` to create the missing subject or reassign to an existing one) and `TextosCompartir` (texts shared by both sheets). Imported items give the same registration EduCoins as manual entry, under the same conditions and daily caps (`ResultadoImportacion.Importado.recompensa`). Do not change `TipoCompartido.clave` or the JSON keys: printed codes would stop working; bump `CodigoQr.VERSION` for incompatible changes.
- Sharing: `CompartirQrDialog` (QR drawn by `core/qr/GeneradorQr` with ZXing; raises screen brightness). Opened from the share icon in the task/detail screens, and from "Compartir por QR" in the card menus (`menudos.xml`; hidden in Agenda).
- Reading: `ui/compartir/EscanerQrActivity` (CameraX 1.4.2 — keep it ≥ 1.4 so its native libs stay 16 KB-aligned; 1.5+ needs compileSdk 35 — + ML Kit barcode-scanning with the bundled model, QR format only; camera permission with rationale/settings fallback, torch, tap to focus, `VisorQrView` overlay). Non-EduPlanner codes show a brief notice and scanning continues. It returns the text through `EscanerQrActivity.Contrato`; `InicioActivity` ("Leer QR" FAB) passes it to `ui/compartir/CompartirViewModel`, shows `ImportarCompartidoDialog` (confirm or cancel; choose to create or reassign the subject) and then navigates to the screen where the item appears.

### Task actions
`ui/Inicio/Dialogs/AccionesTarea` coordinates deliver/grade/delete for Home, Agenda and the task screen (its Entregar/Calificar button); after delivering, `Context.preguntar()` offers "Añadir calificación" / "Más tarde"; difficulty is asked once per task when the flow ends.

### Notifications
- **What is notified** (`domain/notificaciones/ReglasAvisos`, pure and unit-tested): class 10 min before it starts; daily summary at 9:00 with that day's pending tasks and events (skipped if there is nothing); a pending task 1 hour and 5 minutes before its deadline and 1 minute after it expires; and when a class ends, an attendance question with "Sí, asistí" / "No asistí" buttons. Task deadline = `fecha` + optional `horaEntrega`, or 23:59 (`ReglasEntrega.limite`; "a tiempo" uses the same limit).
- **How it works with the app closed**: `ReprogramarAvisosUseCase` builds a `PlanAvisos` from the stored data and hands it to `ProgramadorAvisos` (bound in `di/AvisosModule` to `ui/notifications/ProgramadorAlarmas`: exact `AlarmManager` alarms, remembered in SharedPreferences so stale ones are cancelled). It runs on app start, whenever `tareas_table`/`asignaturas_table`/`asistencias`/`calificaciones_table` change (`VigilanteAvisos`, a Room `InvalidationTracker` observer started in `MvvmEduPlannerApp`, debounced), after each alarm or button press, and on boot/time change/app update (`ReprogramarAvisosReceiver`). Sign-out cancels everything.
- **At fire time** `AvisoReceiver` calls `ConstruirAvisoUseCase`, which re-reads the data and returns null if the notice no longer applies (task delivered/deleted, attendance answered…); texts live in `TextosAvisos`. `PublicadorAvisos` draws it (colour by urgency, large round icon, expandable text, buttons; tag `aviso|<clave>`, one notification per task or class that each new notice replaces) and retires shown notices that are no longer in `PlanAvisos.vigentes`. Buttons go to `AccionAvisoReceiver` → `ResponderAvisoUseCase` (attendance via `ConfirmarAsistenciaUseCase`, delivery via `EntregarTareaUseCase`); the result replaces the notice under tag `respuesta|<clave>` and disappears after a few seconds.
- Receivers extend `ReceptorConHilt` (Hilt needs `super.onReceive`, abstract in `BroadcastReceiver`) and use `goAsync`. Tapping a notice opens `InicioActivity` with `IntentsAvisos.EXTRA_DESTINO` (task screen, schedule or attendance dialog). Channels (`CanalesNotificacion`): clases, tareas, asistencia, resumen_dia and the historical `myChannel` for focus sessions. Android 13+ permission is explained and requested once (`PreferenciasAvisos`).
- `NotificationScheduler` only keeps the focus-session alarm (`EnfoqueNotification`). Do not change `TipoAviso.clave`: it is stored in pending alarms. `androidTest/.../PublicadorAvisosTest` publishes one notice of each type (`-e dejarVisibles true` keeps them on screen for review).

### Firebase
- `app/google-services.json` (Firebase project config). Analytics enabled.
- Auth via `data/network/AuthenticationService` / `FirebaseClient`: email + password (`LoginUseCase`) and Google (`LoginConGoogleUseCase`).
- Google sign-in uses Credential Manager (`androidx.credentials` + `googleid`), not the deprecated `GoogleSignInClient`. `data/network/CredencialGoogleService` gets the ID token and needs the Activity, so the flow is: `MiViewModel.onGoogleSelected()` → event `solicitarCredencialGoogle` → `MainActivity` opens the picker → `MiViewModel.onResultadoGoogle()` → use case → Firebase. The web client id is `R.string.default_web_client_id`, generated from the type-3 OAuth client in `google-services.json`. Sign-out (`InicioActivity`) calls `CredencialGoogleService.olvidarCuenta()` before `FirebaseAuth.signOut()`.
- Google sign-in only works with keystores whose SHA-1 is registered in Firebase. The local debug keystore is registered; add the release/Play signing SHA-1 before publishing.

### Design and themes
- **Every color is a theme token.** Four themes (Opciones > "Tema de la app" in the drawer): Claro (original purple), Oscuro, Azul and Clásico (green). Each token is a theme attribute declared in `values/attrs_tema.xml` (`eduAcento`, `eduSuperficie`, `eduTextPrimary`…) and valued per theme in `values/themes.xml` (`Base.Theme.MyApplication` = Claro; `Tema.EduPlanner.Azul/Clasico` override the palette; `Tema.EduPlanner.Oscuro` starts from a dark Material parent, so it repeats the common items).
- Use tokens, never hex: in XML `?attr/eduAcento` (also valid as a background); in Kotlin `context.getColor(R.color.acento)` with a themed (Activity/View) context. `res/color/<token>.xml` are one-item selectors pointing to the attribute. Theme items that the framework reads as resources (`android:textColorPrimary/Secondary/Hint`) must use `@color/text_*`, not `?attr`, or NavigationView crashes.
- Tokens: brand `marca_profunda`, `marca`, `acento`, `sobre_acento` (text/icons on the accent), `marca_clara`, `marca_suave`, `marca_superficie`; `degradado_inicio`/`degradado_fin` (header and hero gradients, always dark enough for white text); surfaces `fondo_app`, `superficie`, `borde_suave`, `pista_clara`, `velo_oscuro`; text `text_primary`, `text_secondary`, `text_hint`; `estado_exito`/`aviso`/`error` and `_suave`; `evento`/`evento_suave`; `logro_bloqueado`.
- Fixed colors that never change (`values/colors.xml`): `white` (only text/icons on the brand gradient, the camera or the QR), `black`, `blanco_70`/`blanco_25`, `xp_dorado`, `logro_bronce/plata/oro`, `google_border`, `marca_lanzador` (splash). Illustrations (avatar, accessories, coin, Google logo) keep their own colors.
- The chosen theme is stored per device by `data/preferencias/PreferenciasTema` (SharedPreferences, read synchronously) as `domain/preferencias/TemaApp`. `ui/tema/Temas.kt`: `TemaApp.estilo`, `Activity.aplicarTemaElegido()` (call **before** `super.onCreate`, done in `InicioActivity` and `EscanerQrActivity`) and `ajustarBarrasDelSistema()`. `TemaDialog` + `TemaViewModel` change it and `InicioActivity` calls `recreate()`. Login, register and splash always use `Theme.MyApplication` (Claro). To add a theme: a full palette in `themes.xml`, an entry in `TemaApp` and its style in `Temas.kt`; `VistasInflanTest` inflates every layout with every theme.
- Forms follow `sheet_agregar_evento.xml`: outlined `TextInputLayout`s with 16dp corners and a 58dp `MaterialButton`. List cards follow `item_asignatura_modern.xml`.

## Development Guidelines

### Code Structure
- Base package `com.softcg.myapplication`, feature-based under `ui/Inicio/Fragments/<Feature>/` with `Adapters/` and `Models/` subfolders.
- ViewModels are paired with their Fragments.

### Testing
- **`connectedAndroidTest` uninstalls the app when it finishes, wiping the device's local Room data and the signed-in session.** Ask before running it on a device or emulator with real data.
- Unit tests in `src/test/` cover domain rules/use cases, repositories, ViewModels, dialog parsing and the scheduler. Instrumented tests in `src/androidTest/` cover migrations, gamification persistence with real Room, and layout inflation (`VistasInflanTest`).
- Frameworks: JUnit 4, MockK (preferred in newer tests), Mockito/mockito-kotlin, coroutines-test, Espresso.
- ViewModel tests use `InstantTaskExecutorRule` plus `core/MainDispatcherRule` (test sources) so `viewModelScope` runs synchronously. `unitTests.isReturnDefaultValues = true` lets `android.util.Log` calls pass in JVM tests; a real `org.json` is on the test classpath.
- `core/TestUtils.kt` (test sources): `RelojFijo` (fixed clock, zone America/Bogota), `LocalDateTime.millis()`, and `GamificacionRepository.ejecutarTransaccionesDirectamente()` to run `enTransaccion` blocks in JVM tests.

### Dependencies Management
- Version catalog: `gradle/libs.versions.toml` declares all Gradle plugins (AGP 8.3.0, Kotlin 1.9.20, KSP 1.9.20-1.0.14, Hilt 2.50, Google Services 4.4.1). Keep the Kotlin and KSP versions in sync. Most libraries are still declared inline in `app/build.gradle.kts`.
- Firebase BoM 32.8.0. The project is on **Kotlin 1.9**: don't upgrade to libraries that require Kotlin 2 (e.g. `firebase-ai` 17+, BoM 33.16+) without migrating Kotlin.
- Java/Kotlin target 1.8; ViewBinding enabled; Hilt `enableAggregatingTask = false`.
- Minimum SDK: 28, Target/Compile SDK: 34. Application id/namespace: `com.softcg.myapplication`.

### Other docs
`EduPlanner_Package_Diagram.md`, `EduPlanner_Class_Diagram.md`, `EduPlanner_Activity_Diagram.md` and `EduPlanner_Navigation_Map.md` at the root are Mermaid diagrams (in Spanish) of the current architecture, main classes, flows and navigation. Keep them in sync when adding screens, use cases or flows; if they disagree with the code, the code wins.
