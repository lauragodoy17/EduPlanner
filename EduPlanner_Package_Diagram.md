# EduPlanner - Diagrama de Paquetes

Paquete base `com.softcg.myapplication`. Arquitectura MVVM en tres capas (UI → dominio → datos) con inyección de dependencias por Hilt. Las flechas indican "usa / depende de".

```mermaid
graph TD
    subgraph APP["raíz"]
        APPC[MvvmEduPlannerApp<br/>@HiltAndroidApp · App Check]
        SPLASH[PrincipalSplash]
    end

    subgraph UI["ui (presentación)"]
        LOGIN["ui.login<br/>MainActivity · Myviewmodel"]
        REGISTER["ui.register<br/>RegisterActivity · viewmodelregister"]
        INICIO["ui.Inicio<br/>InicioActivity · InicioViewModel · DatosEstudioViewModel"]
        DIALOGS["ui.Inicio.Dialogs<br/>formularios, hojas inferiores, AccionesTarea, InfoTarea"]
        FRAGS["ui.Inicio.Fragments<br/>Home · Agenda · Horario · Calificaciones · Asignaturas<br/>Perfil (Tabs, Views, Tienda, Enfoque) · Prediccion · Detalle · Ayuda"]
        MODELS["ui.Inicio.Models<br/>Tarea · Evento · TimelineItem · DateFilterItem"]
        COMPARTIR_UI["ui.compartir<br/>EscanerQrActivity · VisorQrView · CompartirViewModel"]
        TEMA_UI["ui.tema<br/>Temas · TemaDialog · TemaViewModel"]
        NOTIF["ui.notifications<br/>NotificationScheduler · AlarmNotification · EnfoqueNotification"]
    end

    subgraph DOMAIN["domain (casos de uso y reglas puras)"]
        D_ROOT["domain<br/>Login · LoginConGoogle · CreateAccount · getTareas<br/>CrearAsignatura · EditarTarea · EditarAsignatura"]
        D_GAMI["domain.gamificacion<br/>recompensas · niveles · racha · logros · misiones<br/>tienda · enfoque · asistencia · estadísticas"]
        D_PRED["domain.prediccion<br/>CatalogoModelo · ExtraerDatosPrediccion · PredecirTarea<br/>DetectarAreasPendientes · GuardarPercepciones · ReglasExtraccion"]
        D_IA["domain.ia<br/>RecomendarTareaUseCase · Fuentes"]
        D_COMP["domain.compartir<br/>CodigoQr · ContenidoCompartido · ImportarCompartido · TextosCompartir"]
        D_PREF["domain.preferencias<br/>TemaApp"]
    end

    subgraph DATA["data (acceso a datos)"]
        REPOS["data.Repositories<br/>Tareas · Eventos · Asignaturas · Calificaciones<br/>Gamificacion · Percepciones"]
        DB["data.database<br/>TareasDatabase v16 · Migrations · DuenoDatosLocales<br/>dao · entities"]
        NET["data.network<br/>FirebaseClient · AuthenticationService · CredencialGoogleService<br/>SesionUsuario · PrediccionService · GeminiRecomendaciones · VerificadorFuentesWeb"]
        PREFS["data.preferencias<br/>PreferenciasTema (SharedPreferences)"]
    end

    subgraph CORE["core (utilidades transversales)"]
        CORE_EX["core.ex<br/>DialogEx · EditTextEx · EstiloEx · RecompensaEx · ViewEx"]
        CORE_MISC["core<br/>Event · Reloj · dialog · delegate"]
        CORE_QR["core.qr<br/>GeneradorQr (ZXing)"]
    end

    subgraph DI["di (módulos Hilt)"]
        ROOM_M[RoomModule]
        GAMI_M["GamificacionModule<br/>Reloj · SesionUsuario"]
        IA_M["IaModule<br/>Gemini · VerificadorFuentes · ClienteHttp"]
    end

    subgraph EXT["Externos"]
        FIREBASE[(Firebase<br/>Auth · AI Logic · App Check · Analytics)]
        API[(API predictiva<br/>softcgeduplanner.duckdns.org)]
        ROOMDB[(Room / SQLite)]
        CAMARA[(CameraX + ML Kit)]
    end

    SPLASH --> LOGIN
    SPLASH --> INICIO
    LOGIN --> D_ROOT
    REGISTER --> D_ROOT
    INICIO --> FRAGS
    INICIO --> DIALOGS
    INICIO --> COMPARTIR_UI
    INICIO --> TEMA_UI
    INICIO --> NOTIF
    FRAGS --> DIALOGS
    FRAGS --> MODELS

    INICIO --> D_ROOT
    INICIO --> D_GAMI
    FRAGS --> D_GAMI
    FRAGS --> D_PRED
    FRAGS --> D_IA
    DIALOGS --> D_COMP
    COMPARTIR_UI --> D_COMP
    TEMA_UI --> D_PREF
    TEMA_UI --> PREFS

    D_ROOT --> REPOS
    D_ROOT --> NET
    D_GAMI --> REPOS
    D_PRED --> REPOS
    D_PRED --> NET
    D_IA --> D_PRED
    D_IA --> NET
    D_COMP --> REPOS
    D_COMP --> D_ROOT

    REPOS --> DB
    REPOS --> NET
    DB --> ROOMDB
    NET --> FIREBASE
    NET --> API
    COMPARTIR_UI --> CAMARA

    UI -.-> CORE
    DOMAIN -.-> CORE
    DI -.provee.-> DATA
    DI -.provee.-> DOMAIN
```

## Responsabilidades

| Paquete | Contenido |
|---|---|
| `ui` | Activities, Fragments, diálogos y ViewModels (`@HiltViewModel`, LiveData, eventos de un solo uso con `core/Event`). No accede a Room ni a Firebase directamente. |
| `domain` | Casos de uso (`@Inject constructor`, `operator fun invoke`) y objetos de reglas puras probados sin Android (`RachaReglas`, `NivelesConfig`, `ReglasExtraccion`, `CodigoQr`…). |
| `data` | Entidades y DAOs de Room, repositorios (filtrados por `uid`), clientes de red y preferencias del dispositivo. |
| `core` | Extensiones de vistas y diálogos, `Reloj` (inyectable para tests), generador de QR. |
| `di` | `RoomModule`, `GamificacionModule` (`Reloj`, `SesionUsuario`) e `IaModule` (Gemini, verificador de fuentes, cliente HTTP). |

## Recursos

- `res/values/themes.xml`: los cuatro temas (Claro, Oscuro, Azul, Clásico). `attrs_tema.xml` declara la paleta (`eduAcento`, `eduSuperficie`…); `res/color/<token>.xml` la expone al código como `R.color.<token>`.
- `res/values/styles.xml`: sistema de diseño `Edu.*` (textos, tarjetas, botones, campos, hojas).
- `res/navigation/nav_graph.xml`: destinos de `InicioActivity`. `res/menu/menu.xml`: menú lateral.
