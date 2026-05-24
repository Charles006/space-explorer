package com.space_explorer.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class DateUtilsTest {

    @Before
    fun setUp() {
        DateUtils.setFixedClock("2026-05-22")
    }

    @After
    fun tearDown() {
        DateUtils.resetClock()
    }

    @Test
    fun isValidIsoDate_validInput_returnsTrue() {
        assertThat(DateUtils.isValidIsoDate("2026-05-22")).isTrue()
        assertThat(DateUtils.isValidIsoDate("2000-01-01")).isTrue()
    }

    @Test
    fun isValidIsoDate_malformed_returnsFalse() {
        assertThat(DateUtils.isValidIsoDate("22-05-2026")).isFalse()
        assertThat(DateUtils.isValidIsoDate("not-a-date")).isFalse()
        assertThat(DateUtils.isValidIsoDate("")).isFalse()
        assertThat(DateUtils.isValidIsoDate("2026-5-22")).isFalse()
        assertThat(DateUtils.isValidIsoDate("2026/05/22")).isFalse()
    }

    @Test
    fun isValidIsoDate_impossibleCalendarDates_returnsFalse() {
        assertThat(DateUtils.isValidIsoDate("2026-02-30")).isFalse()
        assertThat(DateUtils.isValidIsoDate("2026-13-01")).isFalse()
    }

    @Test
    fun prettyPrint_validIso_formatsInSpanish() {
        val pretty = DateUtils.prettyPrint("2026-05-22")
        assertThat(pretty).contains("2026")
        assertThat(pretty).contains("22")
        assertThat(pretty.lowercase()).contains("mayo")
    }

    @Test
    fun prettyPrint_invalidInput_returnsOriginal() {
        assertThat(DateUtils.prettyPrint("not-a-date")).isEqualTo("not-a-date")
    }

    @Test
    fun today_returnsFixedClockValue() {
        assertThat(DateUtils.today()).isEqualTo("2026-05-22")
    }

    @Test
    fun daysAgo_subtractsCorrectly() {
        assertThat(DateUtils.daysAgo(0)).isEqualTo("2026-05-22")
        assertThat(DateUtils.daysAgo(1)).isEqualTo("2026-05-21")
        assertThat(DateUtils.daysAgo(22)).isEqualTo("2026-04-30")
    }

    @Test
    fun subtractDays_acrossMonthBoundary() {
        assertThat(DateUtils.subtractDays("2026-05-01", 1)).isEqualTo("2026-04-30")
        assertThat(DateUtils.subtractDays("2026-03-01", 1)).isEqualTo("2026-02-28")
    }

    @Test
    fun previousDay_returnsDayBefore() {
        assertThat(DateUtils.previousDay("2026-05-22")).isEqualTo("2026-05-21")
    }
}
