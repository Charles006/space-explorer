package com.space_explorer.ui.util

import com.space_explorer.core.Constants
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

/**
 * Date helpers used by the UI / ViewModel layer.
 *
 * Why java.time (not SimpleDateFormat):
 *   * [DateTimeFormatter] is thread-safe and immutable, so we can keep a
 *     single static instance per pattern without worrying about race
 *     conditions in multi-threaded code (the previous implementation shared
 *     a `SimpleDateFormat`, which is documented as non thread-safe).
 *   * Java time API is the canonical way to handle dates since Java 8 and
 *     is available on minSdk 24+ through core library desugaring (see
 *     `app/build.gradle.kts`).
 *
 * The [clock] dependency is injectable to make time-sensitive logic
 * deterministic in tests.
 */
object DateUtils {

    /**
     * Strict ISO-8601 formatter.
     *
     * `uuuu` (proleptic year) + STRICT [ResolverStyle] is what gives us
     * unambiguous rejection of impossible calendar dates such as `2026-02-30`
     * or `2026-13-01`. With the more lenient SMART resolver these would be
     * silently rolled to the next valid day, masking input bugs.
     */
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd", Locale.US)
        .withResolverStyle(ResolverStyle.STRICT)

    private val prettyFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd 'de' MMMM, uuuu", Locale("es"))
        .withResolverStyle(ResolverStyle.STRICT)

    /**
     * Clock used to read "now". `UTC` keeps results stable regardless of
     * device timezone (NASA APOD is a UTC-keyed dataset).
     *
     * Exposed as a `var` so tests can substitute a `Clock.fixed(...)`.
     */
    @Volatile
    internal var clock: Clock = Clock.systemUTC()

    /** Today in `yyyy-MM-dd` (UTC). */
    fun today(): String = LocalDate.now(clock).format(isoFormatter)

    /** [days] days before today, in `yyyy-MM-dd` (UTC). Negative values move forward. */
    fun daysAgo(days: Int): String =
        LocalDate.now(clock).minusDays(days.toLong()).format(isoFormatter)

    /** Returns the date that is [days] before [date]. Pure function. */
    fun subtractDays(date: String, days: Int): String =
        LocalDate.parse(date, isoFormatter).minusDays(days.toLong()).format(isoFormatter)

    /** Returns the day before [date]. Pure function. */
    fun previousDay(date: String): String = subtractDays(date, 1)

    /** Strict ISO-8601 validation. Rejects malformed strings and impossible dates (e.g. 2024-02-30). */
    fun isValidIsoDate(value: String): Boolean {
        if (value.length != Constants.ISO_DATE_LENGTH) return false
        return try {
            LocalDate.parse(value, isoFormatter)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    /**
     * Formats an ISO date for display, e.g. `2026-05-22` -> `22 de mayo, 2026`.
     * Returns the input unchanged when parsing fails — defensive fallback so
     * the UI never shows a placeholder for an obviously usable string.
     */
    fun prettyPrint(isoDate: String): String = try {
        LocalDate.parse(isoDate, isoFormatter).format(prettyFormatter)
    } catch (_: DateTimeParseException) {
        isoDate
    }

    /** Test helper. Restores production clock. Never call from app code. */
    internal fun resetClock() {
        clock = Clock.systemUTC()
    }

    /** Test helper. Pins "today" to a known instant. */
    internal fun setFixedClock(isoDate: String) {
        val date = LocalDate.parse(isoDate, isoFormatter)
        clock = Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    }
}
