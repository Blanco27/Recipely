package com.nwe.recipely

import com.nwe.recipely.ui.cook.parseTimerSeconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StepTimerParserTest {

    @Test fun germanMinutes() = assertEquals(1200, parseTimerSeconds("Tomaten 20 Minuten köcheln lassen"))
    @Test fun germanMinAbbrev() = assertEquals(1200, parseTimerSeconds("20 min ziehen lassen"))
    @Test fun noSpace() = assertEquals(1200, parseTimerSeconds("20Min warten"))
    @Test fun englishMinutesPlural() = assertEquals(1200, parseTimerSeconds("Simmer for 20 minutes"))
    @Test fun germanHours() = assertEquals(3600, parseTimerSeconds("1 Std backen"))
    @Test fun hAbbrev() = assertEquals(3600, parseTimerSeconds("Ruhen lassen: 1 h"))
    @Test fun englishHour() = assertEquals(3600, parseTimerSeconds("Bake for 1 hour"))
    @Test fun caseInsensitive() = assertEquals(1200, parseTimerSeconds("20 MIN"))
    @Test fun firstOfSeveral() = assertEquals(600, parseTimerSeconds("Rest 10 min, then bake 30 minutes"))
    @Test fun zeroIsIgnored() = assertNull(parseTimerSeconds("0 min"))
    @Test fun noDuration() = assertNull(parseTimerSeconds("Mix everything well"))
    @Test fun numberWithoutUnit() = assertNull(parseTimerSeconds("Add 2 eggs"))
    @Test fun blank() = assertNull(parseTimerSeconds(""))
}
