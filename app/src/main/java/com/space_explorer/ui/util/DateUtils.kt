package com.space_explorer.ui.util

import com.space_explorer.core.Constants
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

object DateUtils {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd", Locale.US)
        .withResolverStyle(ResolverStyle.STRICT)

    private val prettyFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd 'de' MMMM, uuuu", Locale("es"))
        .withResolverStyle(ResolverStyle.STRICT)

    @Volatile
    internal var clock: Clock = Clock.systemUTC()

    fun today(): String = LocalDate.now(clock).format(isoFormatter)

    fun daysAgo(days: Int): String =
        LocalDate.now(clock).minusDays(days.toLong()).format(isoFormatter)

    fun subtractDays(date: String, days: Int): String =
        LocalDate.parse(date, isoFormatter).minusDays(days.toLong()).format(isoFormatter)

    fun previousDay(date: String): String = subtractDays(date, 1)

    fun isValidIsoDate(value: String): Boolean {
        if (value.length != Constants.ISO_DATE_LENGTH) return false
        return try {
            LocalDate.parse(value, isoFormatter)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    fun prettyPrint(isoDate: String): String = try {
        LocalDate.parse(isoDate, isoFormatter).format(prettyFormatter)
    } catch (_: DateTimeParseException) {
        isoDate
    }

    internal fun resetClock() {
        clock = Clock.systemUTC()
    }

    internal fun setFixedClock(isoDate: String) {
        val date = LocalDate.parse(isoDate, isoFormatter)
        clock = Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    }
}
