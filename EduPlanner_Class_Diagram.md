# Diagrama de Clases - EduPlanner

Clases principales y sus relaciones. Para no saturar, cada diagrama muestra un módulo; los campos y métodos son los relevantes, no todos. Todas las entidades llevan `uid` (dueño): los datos son por usuario.

## Datos (Room, base de datos versión 17)

```mermaid
classDiagram
    class TareasDatabase {
        +getTareasDao() TareasDao
        +getEventosDao() EventosDao
        +getAsignaturasDao() AsignaturasDao
        +getCalificacionesDao() CalificacionesDao
        +getGamificacionDao() GamificacionDao
        +getPercepcionesDao() PercepcionesDao
    }
    class TareaEntity {
        +Int? id
        +String titulo
        +String descrip
        +String asignatura
        +Int? asignaturaId
        +String fecha
        +Int prioridad
        +Long? entregadaMillis
        +String? tema
        +String? horaEntrega
        +String uid
    }
    class EventoEntity {
        +Int? id
        +String titulo
        +String descrip
        +String fecha
        +Int prioridad
        +String horaInicio
        +String horaFin
        +String? imagenUri
        +String uid
    }
    class AsignaturaEntity {
        +Int? id
        +String nombre
        +String tutor
        +Int duracion
        +String hora
        +Boolean lunes … domingo
        +String tipoCalificacion
        +Long fechaRegistro
        +String? areaModelo
        +Float? escalaMin
        +Float? escalaMax
        +String uid
    }
    class CalificacionEntity {
        +Int? id
        +String tipo
        +Float valor
        +String asignatura
        +Int? asignaturaId
        +String descripcion
        +Int? tareaId
        +String uid
    }
    class PercepcionEntity {
        +String tipo
        +String origen
        +Int asignaturaId
        +String? tema
        +Float? valor
        +String? texto
        +Long fechaMillis
    }
    class MovimientoEduCoinsEntity {
        +String clave «TIPO:referencia»
        +String tipo
        +Int cantidad
        +String descripcion
        +Long fechaMillis
    }
    class SesionEnfoqueEntity {
        +Long inicioMillis
        +Int minutos
        +String asignatura
        +Int? asignaturaId
        +String? metodo
        +Long? completadaMillis
    }
    class AsistenciaEntity {
        +Int asignaturaId
        +String fecha
        +Boolean asistio
    }
    class InventarioEntity {
        +String itemId
    }
    class AvatarEquipadoEntity {
        +String categoria
        +String itemId
    }
    class AvatarAparienciaEntity {
        +Int indicePiel
        +Int indicePelo
    }
    class MisionAsignadaEntity {
        +String periodo
        +String misionId
    }

    TareasDatabase --> TareaEntity
    TareasDatabase --> EventoEntity
    TareasDatabase --> AsignaturaEntity
    TareasDatabase --> CalificacionEntity
    TareasDatabase --> PercepcionEntity
    TareasDatabase --> MovimientoEduCoinsEntity
    TareasDatabase --> SesionEnfoqueEntity
    TareasDatabase --> AsistenciaEntity
    TareasDatabase --> InventarioEntity
    TareasDatabase --> AvatarEquipadoEntity
    TareasDatabase --> AvatarAparienciaEntity
    TareasDatabase --> MisionAsignadaEntity
    TareaEntity ..> AsignaturaEntity : asignaturaId / nombre
    CalificacionEntity ..> AsignaturaEntity : asignaturaId / nombre
    CalificacionEntity ..> TareaEntity : tareaId
    SesionEnfoqueEntity ..> AsignaturaEntity
    AsistenciaEntity ..> AsignaturaEntity
    PercepcionEntity ..> AsignaturaEntity
```

## Repositorios y servicios

```mermaid
classDiagram
    class SesionUsuario {
        <<interface>>
        +uid() String?
    }
    class DuenoDatosLocales {
        +asegurar(uid)
    }
    class TareasRepository {
        +getTareas() List~Tarea~
        +getTarea(id) Tarea?
        +insertTarea(tarea) Long
        +updateTarea(tarea) Boolean
        +marcarEntregada(id, millis) Boolean
        +deleteTarea(tarea)
    }
    class AsignaturasRepository {
        +getAllAsignaturas() List~Asignatura~
        +insertAsignatura(a) Long
        +actualizarAsignatura(a) Boolean
        +renombrarEnRegistros(id, viejo, nuevo)
        +guardarDatosModelo(id, area, min, max)
        +deleteAsignatura(a)
    }
    class CalificacionesRespository {
        +getAllCalificaciones()
        +getCalificacion(id)
        +insertCalificacion(c) Long
        +updateCalificacion(c) Boolean
        +moverDeTarea(tareaId, asignatura)
    }
    class EventosRepository {
        +getAllEventos()
        +getEvento(id)
        +insertEvento(e) Long
        +updateEvento(e)
        +deleteEvento(e)
    }
    class GamificacionRepository {
        +enTransaccion(bloque)
        +observarSaldo(uid) LiveData~Int~
    }
    class PercepcionesRepository
    class PreferenciasTema {
        +tema() TemaApp
        +guardar(tema)
    }
    class PrediccionService {
        +predecir(cuerpoJson, topN) RespuestaApi
    }
    class GeminiRecomendaciones {
        «GeneradorRecomendaciones»
    }
    class VerificadorFuentesWeb {
        «VerificadorFuentes»
    }
    class AuthenticationService
    class CredencialGoogleService {
        +obtenerIdToken(activity)
        +olvidarCuenta()
    }

    TareasRepository --> SesionUsuario
    AsignaturasRepository --> SesionUsuario
    CalificacionesRespository --> SesionUsuario
    EventosRepository --> SesionUsuario
    TareasRepository --> DuenoDatosLocales
    AsignaturasRepository ..> TareasRepository : renombra por id o nombre
```

## Dominio

```mermaid
classDiagram
    class CrearAsignaturaUseCase {
        +invoke(NuevaAsignatura) ResultadoCrearAsignatura
    }
    class EditarTareaUseCase {
        +invoke(Tarea) ResultadoEdicion
    }
    class EditarAsignaturaUseCase {
        +invoke(id, NuevaAsignatura) ResultadoEdicion
    }
    class OtorgarRecompensaUseCase {
        +invoke(TipoRecompensa, referencia) ResultadoRecompensa
    }
    class EntregarTareaUseCase
    class CalificarTareaUseCase
    class EvaluarProgresoUseCase
    class ComprarAccesorioUseCase
    class ExtraerDatosPrediccionUseCase {
        +deTarea(Tarea) Extraccion
        +deAsignatura(id) Extraccion
    }
    class PredecirTareaUseCase
    class RecomendarTareaUseCase {
        +invoke(Tarea) ResultadoRecomendacion
    }
    class ImportarCompartidoUseCase {
        +analizar(contenido) AnalisisImportacion
        +importar(contenido, destino) ResultadoImportacion
    }
    class CodigoQr {
        <<object>>
        +codificar(ContenidoCompartido) String
        +leer(String) LecturaQr
    }
    class CatalogoModelo {
        <<object>>
        +detectarArea(nombre) DeteccionArea
        +METODOS_RECOMENDABLES
    }
    class TipoRecompensa {
        <<enum>>
        cantidad · topeDiario
    }
    class TemaApp {
        <<enum>>
        CLARO · OSCURO · AZUL · CLASICO
    }
    class ContenidoCompartido {
        <<sealed>>
        Tarea · Evento · Asignatura · Nota
    }

    CrearAsignaturaUseCase --> OtorgarRecompensaUseCase
    CrearAsignaturaUseCase --> CatalogoModelo
    EditarAsignaturaUseCase --> GamificacionRepository : transacción
    EditarTareaUseCase --> CalificacionesRespository
    EntregarTareaUseCase --> OtorgarRecompensaUseCase
    CalificarTareaUseCase --> OtorgarRecompensaUseCase
    OtorgarRecompensaUseCase --> TipoRecompensa
    RecomendarTareaUseCase --> ExtraerDatosPrediccionUseCase
    RecomendarTareaUseCase --> PredecirTareaUseCase
    RecomendarTareaUseCase --> GeminiRecomendaciones
    RecomendarTareaUseCase --> VerificadorFuentesWeb
    PredecirTareaUseCase --> PrediccionService
    ImportarCompartidoUseCase --> CrearAsignaturaUseCase
    ImportarCompartidoUseCase --> OtorgarRecompensaUseCase
    ImportarCompartidoUseCase --> ContenidoCompartido
    CodigoQr --> ContenidoCompartido
```

## Presentación

```mermaid
classDiagram
    class InicioActivity {
        -InicioViewModel inicioViewModel
        -CompartirViewModel compartir
        -TemaViewModel temaViewModel
        +onCreate() aplica el tema elegido
    }
    class InicioViewModel
    class DatosEstudioViewModel
    class HomeFragment
    class AgendaFragment
    class ScheduleFragment
    class RatingsFragment
    class AsignaturasFragment
    class ProfileFragment
    class PrediccionFragment {
        pantalla "Tarea"
    }
    class PrediccionViewModel {
        +cargar(id)
        +editar(tarea)
        +borrar()
    }
    class DetalleFragment {
        evento · asignatura · calificación
    }
    class DetalleViewModel {
        +cargar(tipo, id)
        +actualizar…()
        +borrar()
    }
    class FichaDetalle {
        <<object>>
        +de(evento / asignatura / calificación) Ficha
    }
    class AccionesTarea {
        ver · entregar · calificar · borrar
    }
    class CompartirViewModel {
        +onQrLeido(texto)
        +onAgregar(contenido, destino)
    }
    class EscanerQrActivity
    class TemaViewModel {
        +elegir(TemaApp)
    }
    class TemaDialog

    InicioActivity --> InicioViewModel
    InicioActivity --> CompartirViewModel
    InicioActivity --> TemaViewModel
    InicioActivity ..> EscanerQrActivity : Contrato
    InicioActivity ..> TemaDialog
    HomeFragment --> AccionesTarea
    AgendaFragment --> AccionesTarea
    HomeFragment ..> PrediccionFragment : abrirTarea()
    HomeFragment ..> DetalleFragment : abrirDetalle()
    AgendaFragment ..> DetalleFragment
    ScheduleFragment ..> DetalleFragment
    AsignaturasFragment ..> DetalleFragment
    RatingsFragment ..> DetalleFragment
    PrediccionFragment --> PrediccionViewModel
    DetalleFragment --> DetalleViewModel
    DetalleFragment --> FichaDetalle
    PrediccionViewModel --> EditarTareaUseCase
    DetalleViewModel --> EditarAsignaturaUseCase
    CompartirViewModel --> ImportarCompartidoUseCase
    TemaViewModel --> PreferenciasTema
```
