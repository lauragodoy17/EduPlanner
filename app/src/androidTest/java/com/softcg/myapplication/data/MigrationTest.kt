package com.softcg.myapplication.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcg.myapplication.data.database.TareasDatabase.MIGRATION_13_14
import com.softcg.myapplication.data.database.TareasDatabase.MIGRATION_14_15
import com.softcg.myapplication.data.database.TareasDatabase.MIGRATION_15_16
import com.softcg.myapplication.data.database.DuenoDatosLocales
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Crea una base con el esquema exacto de la versión 13 (el que generaba Room antes de este cambio),
 * la migra hasta la 16 sin la migración destructiva y comprueba que Room la acepta y que no se perdió nada.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val nombre = "migracion-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(nombre)
        crearBaseVersion13()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(nombre)
    }

    private fun crearBaseVersion13() {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(nombre), null)
        db.execSQL("CREATE TABLE IF NOT EXISTS `tareas_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `titulo` TEXT NOT NULL, `descrip` TEXT NOT NULL, `asignatura` TEXT NOT NULL, `fecha` TEXT NOT NULL, `prioridad` INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tareas_table_id` ON `tareas_table` (`id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `eventos_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `titulo` TEXT NOT NULL, `descrip` TEXT NOT NULL, `fecha` TEXT NOT NULL, `prioridad` INTEGER NOT NULL, `horaInicio` TEXT NOT NULL, `horaFin` TEXT NOT NULL, `imagenUri` TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `asignaturas_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `nombre` TEXT NOT NULL, `tutor` TEXT NOT NULL, `Duracion` INTEGER NOT NULL, `hora` TEXT NOT NULL, `lunes` INTEGER NOT NULL, `martes` INTEGER NOT NULL, `miercoles` INTEGER NOT NULL, `jueves` INTEGER NOT NULL, `viernes` INTEGER NOT NULL, `sabado` INTEGER NOT NULL, `domingo` INTEGER NOT NULL, `tipo_calificacion` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `calificaciones_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `tipo` TEXT NOT NULL, `valor` REAL NOT NULL, `asignatura` TEXT NOT NULL, `descripcion` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b132ed1433b2d9b34820e9b062072543')")

        db.execSQL("INSERT INTO tareas_table (titulo, descrip, asignatura, fecha, prioridad) VALUES ('Ensayo', 'Dos páginas', 'Historia', '2026-10-28', 1)")
        db.execSQL("INSERT INTO eventos_table (titulo, descrip, fecha, prioridad, horaInicio, horaFin, imagenUri) VALUES ('Feria', '', '2026-10-30', 2, '10:00', '12:00', NULL)")
        db.execSQL("INSERT INTO asignaturas_table (nombre, tutor, Duracion, hora, lunes, martes, miercoles, jueves, viernes, sabado, domingo, tipo_calificacion) VALUES ('Física', 'Ana', 2, '14:0', 0, 0, 1, 0, 0, 0, 0, 'Numérico (0-10)')")
        db.execSQL("INSERT INTO asignaturas_table (nombre, tutor, Duracion, hora, lunes, martes, miercoles, jueves, viernes, sabado, domingo, tipo_calificacion) VALUES ('Historia', 'Luis', 2, '8:0', 1, 0, 0, 0, 0, 0, 0, '')")
        db.execSQL("INSERT INTO calificaciones_table (tipo, valor, asignatura, descripcion) VALUES ('Numerico (1-5)', 4.5, 'Física', 'Parcial')")
        db.version = 13
        db.close()
    }

    @Test
    fun lasMigracionesDesdeLa13ConservanLosDatosYRoomAceptaElEsquema() {
        val antes = System.currentTimeMillis()

        // Sin fallbackToDestructiveMigration: si la migración no cuadra con las entidades, Room lanza una excepción
        val db = Room.databaseBuilder(context, TareasDatabase::class.java, nombre)
            .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
            .allowMainThreadQueries()
            .build()

        try {
            // Hasta que alguien entra, las filas anteriores a la 16 no tienen dueño
            assertEquals(1, db.getTareasDao().getAllTareas("").size)
            assertTrue(db.getTareasDao().getAllTareas("usuario-1").isEmpty())

            // El primer usuario que entra las adopta; otro usuario no las ve
            DuenoDatosLocales(db).asegurar("usuario-1")
            assertTrue(db.getTareasDao().getAllTareas("").isEmpty())
            assertTrue(db.getTareasDao().getAllTareas("usuario-2").isEmpty())

            val tareas = db.getTareasDao().getAllTareasConCalificacion("usuario-1")
            assertEquals(1, tareas.size)
            assertEquals("Ensayo", tareas[0].tarea.titulo)
            assertNull(tareas[0].calificacion)
            assertNull("Las tareas existentes siguen sin entregar", tareas[0].tarea.entregadaMillis)
            assertNull("Sin tema hasta que el estudiante lo elija", tareas[0].tarea.tema)

            assertEquals("Feria", db.getEventosDao().getAllEventos("usuario-1").single().titulo)

            val asignaturas = db.getAsignaturasDao().getAllAsignaturas("usuario-1")
            val fisica = asignaturas.single { it.nombre == "Física" }
            val historia = asignaturas.single { it.nombre == "Historia" }
            assertTrue("Las asignaturas existentes quedan registradas al migrar", fisica.fechaRegistro >= antes - 2_000)
            assertNull("El área del modelo la confirma el estudiante, no la migración", fisica.areaModelo)

            assertEquals("La tarea queda enlazada por id a su asignatura", historia.id, tareas[0].tarea.asignaturaId)

            val calificacion = db.getCalificacionesDao().getAllCalificaciones("usuario-1").single()
            assertEquals(4.5F, calificacion.valor)
            assertNull(calificacion.tareaId)
            assertEquals(fisica.id, calificacion.asignaturaId)

            assertEquals(0, runBlocking { db.getGamificacionDao().contarClave("usuario-1", "cualquiera") })
            assertNull(runBlocking { db.getGamificacionDao().sesionEnfoqueActiva("usuario-1") })
            assertTrue(runBlocking { db.getGamificacionDao().misionesAsignadas("usuario-1", "D:2026-11-03") }.isEmpty())
            assertTrue(runBlocking { db.getPercepcionesDao().todas("usuario-1") }.isEmpty())
        } finally {
            db.close()
        }
    }
}
