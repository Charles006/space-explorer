package com.space_explorer.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class DateUtilsTest {

    @Before
    fun setUp() {
        // Pin "today" so date arithmetic tests are deterministic.
        DateUtils.setFixedClock("2026-05-22")
    }

    @After
    fun tearDown() {
        DateUtils.resetClock()
    }

    @Test
    fun `isValidIsoDate accepts proper format`() {
        assertThat(DateUtils.isValidIsoDate("2026-05-22")).isTrue()
        assertThat(DateUtils.isValidIsoDate("2000-01-01")).isTrue()
    }

    @Test
    fun `isValidIsoDate rejects malformed strings`() {
        assertThat(DateUtils.isValidIsoDate("22-05-2026")).isFalse()
        assertThat(DateUtils.isValidIsoDate("not-a-date")).isFalse()
        assertThat(DateUtils.isValidIsoDate("")).isFalse()
        assertThat(DateUtils.isValidIsoDate("2026-5-22")).isFalse()
        assertThat(DateUtils.isValidIsoDate("2026/05/22")).isFalse()
    }

    @Test
    fun `isValidIsoDate rejects impossible calendar dates`() {
        // java.time refuses to roll over invalid days; SimpleDateFormat would accept these.
        assertThat(DateUtils.isValidIsoDate("2026-02-30")).isFalse()
        assertThat(DateUtils.isValidIsoDate("2026-13-01")).isFalse()
    }

    @Test
    fun `prettyPrint formats iso date in spanish locale`() {
        val pretty = DateUtils.prettyPrint("2026-05-22")
        assertThat(pretty).contains("2026")
        assertThat(pretty).contains("22")
        // "mayo" in Spanish month name
        assertThat(pretty.lowercase()).contains("mayo")
    }

    @Test
    fun `prettyPrint returns original on parse failure`() {
        val invalid = "not-a-date"
        assertThat(DateUtils.prettyPrint(invalid)).isEqualTo(invalid)
    }

    @Test
    fun `today returns fixed date when clock is pinned`() {
        assertThat(DateUtils.today()).isEqualTo("2026-05-22")
    }

    @Test
    fun `daysAgo subtracts days from today`() {
        assertThat(DateUtils.daysAgo(0)).isEqualTo("2026-05-22")
        assertThat(DateUtils.daysAgo(1)).isEqualTo("2026-05-21")
        assertThat(DateUtils.daysAgo(22)).isEqualTo("2026-04-30")
    }

    @Test
    fun `subtractDays handles month boundaries`() {
        assertThat(DateUtils.subtractDays("2026-05-01", 1)).isEqualTo("2026-04-30")
        assertThat(DateUtils.subtractDays("2026-03-01", 1)).isEqualTo("2026-02-28")
    }

    @Test
    fun `previousDay returns day before given date`() {
        assertThat(DateUtils.previousDay("2026-05-22")).isEqualTo("2026-05-21")
    }
}
