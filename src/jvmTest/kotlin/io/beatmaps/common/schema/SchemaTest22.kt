package io.beatmaps.common.schema

import io.beatmaps.common.beatsaber.CutDirection
import io.beatmaps.common.schema.SchemaCommon.validateFolder
import io.beatmaps.common.schema.SchemaCommon.violation
import io.beatmaps.common.schema.SchemaCommon.violationWrong
import org.junit.Test
import org.valiktor.constraints.Between
import org.valiktor.constraints.In
import org.valiktor.constraints.Matches
import org.valiktor.constraints.NotNull
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SchemaTest22 {
    @Test
    fun basic2_2() {
        val ex = validateFolder("2_2/basic")
        assertNull(ex)
    }

    @Test
    fun schema2_2() {
        val ex = validateFolder("2_2/default")
        assertNull(ex)
    }

    @Test
    fun error2_2() {
        val ex = validateFolder("2_2/error")
        assertNotNull(ex)

        assertEquals(30, ex.constraintViolations.size)
        assertEquals(
            setOf(
                violation("version", "3", Matches(Regex("\\d+\\.\\d+\\.\\d+"))),
                violation("_notes[0]._type", 50, In(setOf(0, 1, 3))),
                violation("_notes[0]._cutDirection", 50, CutDirection),
                violation("_notes[0]._time", 100f, Between(0.0f, 64f)),
                violation("_notes[1]._type", null, In(setOf(0, 1, 3))),
                violation("_notes[1]._cutDirection", null, NotNull),
                violation("_notes[1]._time", null, NotNull),
                violation("_notes[1]._lineIndex", null, NotNull),
                violation("_notes[1]._lineLayer", null, NotNull),
                violation("_notes[2]._type"),
                violation("_notes[2]._cutDirection"),
                violation("_notes[2]._time"),
                violation("_notes[2]._lineIndex"),
                violation("_notes[2]._lineLayer"),
                violation("_obstacles[1]._type", null, NotNull),
                violation("_obstacles[1]._duration", null, NotNull),
                violation("_obstacles[1]._time", null, NotNull),
                violation("_obstacles[1]._lineIndex", null, NotNull),
                violation("_obstacles[1]._width", null, NotNull),
                violation("_obstacles[2]._type"),
                violation("_obstacles[2]._duration"),
                violation("_obstacles[2]._time"),
                violation("_obstacles[2]._lineIndex"),
                violation("_obstacles[2]._width"),
                violation("_events[1]._time", null, NotNull),
                violation("_events[1]._type", null, NotNull),
                violation("_events[1]._value", null, NotNull),
                violation("_events[2]._time"),
                violation("_events[2]._type"),
                violation("_events[2]._value")
            ),
            ex.constraintViolations
        )
    }

    @Test
    fun badtypes2_2() {
        val ex = validateFolder("2_2/badtypes")
        assertNotNull(ex)

        assertEquals(8, ex.constraintViolations.size)
        assertEquals(
            setOf(
                violationWrong("version"),
                violationWrong("_notes"),
                violationWrong("_obstacles"),
                violationWrong("_events"),
                violationWrong("_waypoints"),
                violationWrong("_specialEventsKeywordFilters"),
                violationWrong("_customData"),
                violationWrong("_BPMChanges")
            ),
            ex.constraintViolations
        )
    }

    @Test
    fun missing2_2() {
        val ex = validateFolder("2_2/missing")
        assertNotNull(ex)

        assertEquals(4, ex.constraintViolations.size)
        assertEquals(
            setOf(
                violation("version"),
                violation("_notes"),
                violation("_obstacles"),
                violation("_events")
            ),
            ex.constraintViolations
        )
    }

    @Test
    fun null2_2() {
        val ex = validateFolder("2_2/null")
        assertNotNull(ex)

        assertEquals(8, ex.constraintViolations.size)
        assertEquals(
            setOf(
                violation("version", null, NotNull),
                violation("_notes", null, NotNull),
                violation("_obstacles", null, NotNull),
                violation("_events", null, NotNull),
                violation("_waypoints", null, NotNull),
                violation("_specialEventsKeywordFilters", null, NotNull),
                violation("_customData", null, NotNull),
                violation("_BPMChanges", null, NotNull)
            ),
            ex.constraintViolations
        )
    }
}
