# EduPlanner - Diagrama de Actividades

Flujos principales tal como están implementados. Los nombres entre paréntesis son las clases que intervienen.

## 1. Inicio de sesión

```mermaid
graph TD
    A([Abrir la app]) --> B[PrincipalSplash]
    B --> C{¿Hay sesión de Firebase?}
    C -->|Sí| Z[InicioActivity]
    C -->|No| D[MainActivity]
    D --> E{¿Cómo entra?}
    E -->|Correo y contraseña| F[LoginUseCase → AuthenticationService]
    E -->|Google| G[MiViewModel.onGoogleSelected → la Activity abre Credential Manager]
    G --> H[CredencialGoogleService obtiene el ID token]
    H --> I[LoginConGoogleUseCase → Firebase]
    E -->|Crear cuenta| J[RegisterActivity → CreateAccountUseCase]
    F --> K{¿Correcto?}
    I --> K
    J --> K
    K -->|No| L[Mensaje de error] --> D
    K -->|Sí| M[DuenoDatosLocales: el primer usuario adopta los datos sin dueño] --> Z
```

## 2. Crear una tarea

```mermaid
graph TD
    A[Botón + → Añadir tarea] --> B[TareaDialog]
    B --> C{¿Asignatura?}
    C -->|Elige una de la lista| D[Guardar]
    C -->|+ Agregar asignatura| E[Se oculta la tarea · AsignaturaDialog]
    E --> F[CrearAsignaturaUseCase: guarda, detecta el área y premia la clase]
    F --> G{¿Área detectada?}
    G -->|Ambigua o ninguna| H[AreaAsignaturaDialog]
    G -->|Única| I[Vuelve la tarea con la asignatura elegida]
    H --> I --> D
    D --> J[InicioViewModel.saveTarea]
    J --> K[OtorgarRecompensaUseCase · TAREA_REGISTRADA con tope diario]
    K --> L[Aviso +N EduCoins · se recarga Inicio]
```

## 3. Entregar y calificar una tarea

```mermaid
graph TD
    A[Pantalla de la tarea / menú ⋮] --> B[Entregar]
    B --> C[EntregarTareaUseCase: marca entregada y premia si es a tiempo]
    C --> D[Celebración]
    D --> E{¿Ya tienes la calificación?}
    E -->|Añadir calificación| F[CalificarTareaDialog → CalificarTareaUseCase]
    E -->|Más tarde| G[La tarea queda ENTREGADA]
    F --> H[COMPLETADA · la nota entra en el promedio de la asignatura]
    G --> I[¿Qué tan difícil fue? · una vez por tarea]
    H --> I
```

## 4. Predicción y recomendación de estudio

```mermaid
graph TD
    A[Tocar una tarea → pantalla Tarea] --> B[ExtraerDatosPrediccionUseCase]
    B --> C{¿Están todos los datos?}
    C -->|No| D["Lista de lo que falta: área, notas, tiempo y método, interés y dificultad"]
    D --> E[DatosEstudioDialogs → GuardarPercepcionesUseCase / GuardarDatosAsignaturaUseCase]
    E --> B
    C -->|Sí| F[PredecirTareaUseCase → PrediccionService POST /predict]
    F --> G[Respuesta del modelo: método recomendado y confianza]
    G --> H[Generar recomendaciones]
    H --> I[RecomendarTareaUseCase: Gemini explica el método · VerificadorFuentesWeb comprueba la bibliografía]
    I --> J[Consejo, pasos, tiempo estimado y fuentes verificadas]
```

La asistencia se supone del 100 % hasta que haya suficientes clases confirmadas (`AsistenciaDialog`); nunca se le pide al estudiante.

## 5. Ver, editar, borrar y compartir

```mermaid
graph TD
    A[Tocar una tarjeta] --> B{¿Qué es?}
    B -->|Tarea| C[PrediccionFragment]
    B -->|Evento, asignatura o calificación| D[DetalleFragment]
    C --> E{Icono}
    D --> E
    E -->|Editar| F[Formulario relleno · Guardar cambios]
    F --> G[EditarTareaUseCase / EditarAsignaturaUseCase / repositorio]
    G --> H[Al renombrar una asignatura, sus tareas, notas y sesiones pasan al nombre nuevo]
    E -->|Borrar| I[Confirmar → borrar → volver con aviso]
    E -->|Compartir| J[CompartirQrDialog: CodigoQr EDUPLANNER:1:…]
```

## 6. Recibir algo por QR

```mermaid
graph TD
    A[Botón + → Leer QR] --> B{¿Permiso de cámara?}
    B -->|No| C[Explicación · Permitir o Abrir ajustes]
    B -->|Sí| D[EscanerQrActivity: CameraX + ML Kit, solo QR]
    D --> E{CodigoQr.leer}
    E -->|No es de EduPlanner / dañado / versión nueva| F[Aviso breve, sigue escaneando] --> D
    E -->|Válido| G[ImportarCompartidoUseCase.analizar]
    G --> H[ImportarCompartidoDialog con lo compartido]
    H -->|Cancelar| X([Fin])
    H --> I{¿La asignatura existe aquí?}
    I -->|Sí| J[Agregar]
    I -->|No| K[Crearla o asignarla a una existente] --> J
    J --> L[importar: guarda y da los EduCoins de registrarlo, con tope diario]
    L --> M[Lleva a la pantalla donde quedó]
```

## 7. Gamificación

```mermaid
graph TD
    A[Actividad real: registrar, entregar, calificar, asistir, enfocarse] --> B[OtorgarRecompensaUseCase]
    B --> C{¿Clave TIPO:referencia nueva y dentro del tope diario?}
    C -->|No| D[Sin monedas]
    C -->|Sí| E[Movimiento en educoins_movimientos]
    E --> F[EvaluarProgresoUseCase]
    F --> G[Nivel · racha · logros · misiones derivados de los movimientos]
    G --> H[InicioActivity encola celebraciones: subida de nivel, logros]
    E --> I[Tienda: ComprarAccesorioUseCase → Avatar]
```

## 8. Notificaciones (con la app cerrada)

```mermaid
graph TD
    A[Cambia una tarea, asignatura, asistencia o nota · abrir la app · reiniciar el teléfono] --> B[ReprogramarAvisosUseCase]
    B --> C[ReglasAvisos.planificar: clase −10 min · resumen 9:00 · tarea −1 h, −5 min y al vencer · asistencia al terminar la clase]
    C --> D[ProgramadorAlarmas: alarmas exactas en AlarmManager · retira lo que ya no aplica]
    D -->|llega la hora| E[AvisoReceiver]
    E --> F{ConstruirAvisoUseCase: ¿sigue aplicando?}
    F -->|No: entregada, borrada, ya respondida| G([No se muestra])
    F -->|Sí| H[PublicadorAvisos: notificación con color, icono y botones]
    H -->|Sí, asistí / No asistí / Marcar como entregada| I[AccionAvisoReceiver → ResponderAvisoUseCase]
    I --> J[La notificación muestra el resultado · +EduCoins]
    H -->|Tocar| K[InicioActivity abre la tarea, el horario o la pregunta de asistencia]
    E --> B
    I --> B
```

## 9. Cambiar el tema

```mermaid
graph TD
    A[Menú lateral → Opciones → Tema de la app] --> B[TemaDialog: muestra de cada tema con sus colores]
    B --> C[TemaViewModel.elegir]
    C --> D{¿Es distinto al actual?}
    D -->|No| E([Nada cambia])
    D -->|Sí| F[PreferenciasTema.guardar]
    F --> G[InicioActivity.recreate]
    G --> H[aplicarTemaElegido antes de crear la pantalla · la pantalla se pinta con la paleta nueva]
```
