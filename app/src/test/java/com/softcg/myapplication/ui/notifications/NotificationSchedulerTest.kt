package com.softcg.myapplication.ui.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NotificationSchedulerTest {

    private fun calendar(day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, day, hour, minute)
        }

    @Test
    fun `si la hora no ha pasado se programa para hoy`() {
        //GIVEN
        val now = calendar(28, 8, 15)

        //WHEN
        val trigger = NotificationScheduler.nextTriggerTime(now, 9, 0)

        //THEN
        assertEquals(calendar(28, 9, 0).timeInMillis, trigger.timeInMillis)
    }

    @Test
    fun `si la hora ya paso se programa para manana`() {
        //GIVEN
        val now = calendar(28, 12, 0)

        //WHEN
        val trigger = NotificationScheduler.nextTriggerTime(now, 11, 30)

        //THEN
        assertEquals(calendar(29, 11, 30).timeInMillis, trigger.timeInMillis)
    }

    @Test
    fun `calcular la alarma no modifica la fecha de referencia`() {
        //GIVEN
        val now = calendar(28, 12, 0)

        //WHEN
        NotificationScheduler.nextTriggerTime(now, 9, 0)

        //THEN
        assertEquals(calendar(28, 12, 0).timeInMillis, now.timeInMillis)
    }
}
