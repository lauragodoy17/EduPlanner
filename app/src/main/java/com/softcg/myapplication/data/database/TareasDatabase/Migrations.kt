package com.softcg.myapplication.data.database.TareasDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 13 → 14: EduCoins, asistencia, inventario y avatar; fecha de registro de las asignaturas
 * y la calificación que completa una tarea. Conserva todos los datos existentes.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Las asignaturas existentes se consideran registradas en el momento de la migración:
        // solo sus clases futuras podrán confirmar asistencia.
        db.execSQL("ALTER TABLE `asignaturas_table` ADD COLUMN `fecha_registro` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `asignaturas_table` SET `fecha_registro` = CAST(strftime('%s','now') AS INTEGER) * 1000")

        db.execSQL("ALTER TABLE `calificaciones_table` ADD COLUMN `tarea_id` INTEGER")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_calificaciones_table_tarea_id` ON `calificaciones_table` (`tarea_id`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `educoins_movimientos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `uid` TEXT NOT NULL, `clave` TEXT NOT NULL, `tipo` TEXT NOT NULL, `cantidad` INTEGER NOT NULL, `descripcion` TEXT NOT NULL, `fecha_millis` INTEGER NOT NULL, `dia` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_educoins_movimientos_uid_clave` ON `educoins_movimientos` (`uid`, `clave`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `asistencias` (`uid` TEXT NOT NULL, `asignatura_id` INTEGER NOT NULL, `fecha` TEXT NOT NULL, `asistio` INTEGER NOT NULL, `inicio_millis` INTEGER NOT NULL, `fin_millis` INTEGER NOT NULL, `confirmada_millis` INTEGER NOT NULL, PRIMARY KEY(`uid`, `asignatura_id`, `fecha`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `inventario` (`uid` TEXT NOT NULL, `item_id` TEXT NOT NULL, `comprado_millis` INTEGER NOT NULL, PRIMARY KEY(`uid`, `item_id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `avatar_equipado` (`uid` TEXT NOT NULL, `categoria` TEXT NOT NULL, `item_id` TEXT NOT NULL, PRIMARY KEY(`uid`, `categoria`))")
    }
}

/**
 * 14 → 15: entrega de tareas sin esperar la nota, misiones asignadas, sesiones de enfoque y apariencia
 * del avatar. Niveles, racha y logros no necesitan tablas: se derivan de los movimientos ya guardados.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tareas_table` ADD COLUMN `entregada_millis` INTEGER")
        db.execSQL("CREATE TABLE IF NOT EXISTS `avatar_apariencia` (`uid` TEXT NOT NULL, `indice_piel` INTEGER NOT NULL, `indice_pelo` INTEGER NOT NULL, PRIMARY KEY(`uid`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `misiones_asignadas` (`uid` TEXT NOT NULL, `periodo` TEXT NOT NULL, `mision_id` TEXT NOT NULL, PRIMARY KEY(`uid`, `periodo`, `mision_id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sesiones_enfoque` (`uid` TEXT NOT NULL, `inicio_millis` INTEGER NOT NULL, `minutos` INTEGER NOT NULL, `asignatura` TEXT NOT NULL, `completada_millis` INTEGER, PRIMARY KEY(`uid`, `inicio_millis`))")
    }
}
