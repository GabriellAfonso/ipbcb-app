package com.ipb.castelobranco.core.domain.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class SchedulePushMessageTest {

    private val tuesday = ScheduleSlot(LocalDate.of(2026, 10, 27), "Terça de Oração", LocalTime.of(19, 30))
    private val sunday = ScheduleSlot(LocalDate.of(2026, 11, 1), "Culto Dominical", LocalTime.of(19, 0))

    private val twoSlots = """[{"date":"2026-10-27","service":"Terça de Oração","time":"19:30"},""" +
        """{"date":"2026-11-01","service":"Culto Dominical","time":"19:00"}]"""
    private val oneSlot = """[{"date":"2026-10-27","service":"Terça de Oração","time":"19:30"}]"""

    private fun slots(json: String) = mapOf("type" to "schedule_week", "slots" to json)

    @Test
    fun `week keeps every slot in the payload order`() {
        assertEquals(
            PushMessage.ScheduleWeek(listOf(tuesday, sunday)),
            PushMessage.parse(slots(twoSlots)),
        )
    }

    @Test
    fun `eve carries its single slot`() {
        assertEquals(
            PushMessage.ScheduleEve(tuesday),
            PushMessage.parse(mapOf("type" to "schedule_eve", "slots" to oneSlot)),
        )
    }

    @Test
    fun `eve with other than one slot is ignored`() {
        assertNull(PushMessage.parse(mapOf("type" to "schedule_eve", "slots" to twoSlots)))
        assertNull(PushMessage.parse(mapOf("type" to "schedule_eve", "slots" to "[]")))
    }

    @Test
    fun `published carries its month and slots`() {
        assertEquals(
            PushMessage.SchedulePublished(YearMonth.of(2026, 11), listOf(tuesday, sunday)),
            PushMessage.parse(mapOf("type" to "schedule_published", "month" to "2026-11", "slots" to twoSlots)),
        )
    }

    @Test
    fun `published without a valid month is ignored`() {
        assertNull(PushMessage.parse(mapOf("type" to "schedule_published", "slots" to oneSlot)))
        assertNull(PushMessage.parse(mapOf("type" to "schedule_published", "month" to "11/2026", "slots" to oneSlot)))
    }

    @Test
    fun `malformed slots are ignored`() {
        assertNull(PushMessage.parse(mapOf("type" to "schedule_week")))
        assertNull(PushMessage.parse(slots("not json")))
        assertNull(PushMessage.parse(slots("""{"date":"2026-10-27"}""")))
        assertNull(PushMessage.parse(slots("[]")))
        assertNull(PushMessage.parse(slots("""[{"date":"27/10/2026","service":"Terça","time":"19:30"}]""")))
        assertNull(PushMessage.parse(slots("""[{"date":"2026-10-27","service":" ","time":"19:30"}]""")))
        assertNull(PushMessage.parse(slots("""[{"date":"2026-10-27","service":"Terça","time":"19h30"}]""")))
        assertNull(PushMessage.parse(slots("""[{"date":"2026-10-27","service":"Terça"}]""")))
        assertNull(PushMessage.parse(slots("$oneSlot trailing")))
    }

    @Test
    fun `one bad slot invalidates the whole message`() {
        val mixed = """[{"date":"2026-10-27","service":"Terça de Oração","time":"19:30"},"oops"]"""
        assertNull(PushMessage.parse(slots(mixed)))
    }

    @Test
    fun `unknown fields are ignored`() {
        val extra = """[{"date":"2026-10-27","service":"Terça de Oração","time":"19:30","room":"A"}]"""
        assertEquals(PushMessage.ScheduleWeek(listOf(tuesday)), PushMessage.parse(slots(extra) + ("x" to "y")))
    }

    @Test
    fun `gender F is feminine on every kind`() {
        val eve = mapOf("type" to "schedule_eve", "slots" to oneSlot, "gender" to "F")
        val published = mapOf("type" to "schedule_published", "month" to "2026-11", "slots" to oneSlot) +
            ("gender" to "F")
        assertEquals(
            PushMessage.ScheduleWeek(listOf(tuesday), RecipientGender.FEMALE),
            PushMessage.parse(slots(oneSlot) + ("gender" to "F")),
        )
        assertEquals(PushMessage.ScheduleEve(tuesday, RecipientGender.FEMALE), PushMessage.parse(eve))
        assertEquals(
            PushMessage.SchedulePublished(YearMonth.of(2026, 11), listOf(tuesday), RecipientGender.FEMALE),
            PushMessage.parse(published),
        )
    }

    @Test
    fun `gender M, absent or unknown is masculine and never invalidates`() {
        listOf("M", "x", "").forEach { value ->
            assertEquals(
                PushMessage.ScheduleWeek(listOf(tuesday), RecipientGender.MALE),
                PushMessage.parse(slots(oneSlot) + ("gender" to value)),
            )
        }
        assertEquals(
            PushMessage.ScheduleWeek(listOf(tuesday), RecipientGender.MALE),
            PushMessage.parse(slots(oneSlot)),
        )
    }
}
