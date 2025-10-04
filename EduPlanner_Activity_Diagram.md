# EduPlanner - Diagrama de Actividades

```mermaid
graph TD
    A[App Launch / PrincipalSplash] --> B{Firebase Auth Check}
    
    B -->|No User| C[MainActivity - Login Screen]
    B -->|User Authenticated| D[InicioActivity - Main Dashboard]
    
    C --> E[Login Process]
    C --> F[Register Process]
    
    E --> G{Login Validation}
    G -->|Valid| H[AuthenticationService.login]
    G -->|Invalid| I[Show Error Dialog]
    I --> C
    
    H --> J{Firebase Auth Result}
    J -->|Success| K[Navigate to PrincipalSplash]
    J -->|Error| L[Show Login Error Dialog]
    L --> C
    
    F --> M[RegisterActivity]
    M --> N[Register Validation]
    N --> O{Registration Valid?}
    O -->|Yes| P[AuthenticationService.createAccount]
    O -->|No| Q[Show Validation Errors]
    Q --> M
    
    P --> R{Firebase Registration}
    R -->|Success| S[Navigate to PrincipalSplash]
    R -->|Error| T[Show Registration Error]
    T --> M
    
    K --> D
    S --> D
    
    D --> U[Initialize Navigation & UI]
    U --> V[Setup Floating Action Buttons]
    U --> W[Create Notification Channels]
    U --> X[Schedule Daily Notifications]
    
    D --> Y[Main Navigation Menu]
    Y --> Z[HomeFragment - Dashboard]
    Y --> AA[AgendaFragment - Calendar]
    Y --> BB[RatingsFragment - Grades]
    Y --> CC[ScheduleFragment - Timetable]
    Y --> DD[AsignaturasFragment - Subjects]
    Y --> EE[HelpFragment - Support]
    
    Z --> FF[Load Tasks from Database]
    Z --> GG[Load Events from Database]
    Z --> HH{Has Content?}
    HH -->|Yes| II[Show Content Layout]
    HH -->|No| JJ[Show Empty State]
    
    D --> KK[Floating Action Button]
    KK --> LL[Add Task Dialog]
    KK --> MM[Add Event Dialog]
    KK --> NN[Add Grade Dialog]
    
    LL --> OO[Task Form Validation]
    OO --> PP[Save Task to Database]
    PP --> QQ[Update Home View]
    PP --> RR[Show Success Toast]
    
    MM --> SS[Event Form Validation]
    SS --> TT[Save Event to Database]
    TT --> UU[Update Home & Agenda Views]
    TT --> VV[Show Success Toast]
    
    NN --> WW[Grade Form Validation]
    WW --> XX[Save Grade to Database]
    XX --> YY[Update Ratings View]
    XX --> ZZ[Show Success Toast]
    
    AA --> AAA[Load Calendar Events]
    AAA --> BBB[Display Agenda Items]
    
    BB --> CCC[Load Grades by Subject]
    CCC --> DDD[Display Grade List]
    
    CC --> EEE[Load Class Schedule]
    EEE --> FFF[Display Timetable]
    
    DD --> GGG[Load Subjects List]
    GGG --> HHH[Display Subjects]
    DD --> III[Add/Edit Subject Dialog]
    
    Y --> JJJ[Logout Option]
    JJJ --> KKK[Firebase.signOut]
    KKK --> LLL[Clear Task Stack]
    LLL --> C
    
    D --> MMM[Background Notification System]
    MMM --> NNN[Schedule 9:00 AM Daily Reminder]
    MMM --> OOO[Schedule 11:30 AM Daily Reminder]
    NNN --> PPP[AlarmNotification Broadcast]
    OOO --> PPP
    
    PPP --> QQQ[Display Pending Tasks Notification]
    
    style A fill:#e1f5fe
    style D fill:#f3e5f5
    style C fill:#fff3e0
    style M fill:#fff3e0
    style Z fill:#e8f5e8
    style AA fill:#e8f5e8
    style BB fill:#e8f5e8
    style CC fill:#e8f5e8
    style DD fill:#e8f5e8
    style EE fill:#e8f5e8
```

## Descripción del Flujo de Actividades

### 1. **Inicio de Aplicación**
- **PrincipalSplash**: Pantalla de inicio que verifica el estado de autenticación
- **Firebase Auth Check**: Determina si el usuario está autenticado

### 2. **Flujo de Autenticación**
- **Login**: MainActivity con validación de campos y autenticación Firebase
- **Register**: RegisterActivity con validación de formulario y creación de cuenta
- **Error Handling**: Diálogos de error para fallos de autenticación

### 3. **Dashboard Principal (InicioActivity)**
- **Navigation Setup**: Configuración del menú lateral y navegación
- **Floating Action Buttons**: Botones flotantes para agregar contenido
- **Notification System**: Sistema de notificaciones programadas

### 4. **Fragmentos Principales**
- **HomeFragment**: Dashboard con tareas y eventos recientes
- **AgendaFragment**: Vista de calendario con eventos
- **RatingsFragment**: Gestión de calificaciones por asignatura
- **ScheduleFragment**: Horario de clases
- **AsignaturasFragment**: Gestión de materias
- **HelpFragment**: Ayuda y soporte

### 5. **Operaciones CRUD**
- **Tasks (Tareas)**: Crear, leer, actualizar tareas con prioridad
- **Events (Eventos)**: Gestión de eventos con fechas
- **Grades (Calificaciones)**: Sistema de notas por asignatura
- **Subjects (Asignaturas)**: Gestión de materias académicas

### 6. **Sistema de Notificaciones**
- **Daily Reminders**: Recordatorios automáticos a las 9:00 AM y 11:30 AM
- **Alarm Manager**: Gestión de alarmas persistentes
- **Notification Channels**: Canales de notificación personalizados

### 7. **Base de Datos (Room)**
- **Entities**: TareaEntity, EventoEntity, CalificacionEntity, AsignaturaEntity
- **DAOs**: Acceso a datos con operaciones CRUD
- **Repository Pattern**: Abstracción de acceso a datos

### 8. **Arquitectura MVVM**
- **ViewModels**: Lógica de negocio para cada fragmento
- **LiveData/StateFlow**: Observación reactiva de datos
- **Dependency Injection**: Hilt para inyección de dependencias

## Características Técnicas

- **Firebase Authentication**: Autenticación segura de usuarios
- **Room Database**: Persistencia local de datos (versión 9)
- **Navigation Component**: Navegación entre fragmentos
- **Material Design**: Interfaz de usuario moderna
- **Coroutines**: Programación asíncrona
- **Notifications**: Sistema de recordatorios automáticos