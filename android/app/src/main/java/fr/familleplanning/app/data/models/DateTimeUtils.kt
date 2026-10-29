package fr.familleplanning.app.data.models

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilitaires temporels stricts pour FamillePlanning.
 * Le fuseau horaire de référence de la famille est "Europe/Paris".
 * Ne jamais interpréter une heure locale comme un horaire UTC.
 */
object DateTimeUtils {
    val ZONE_PARIS: ZoneId = ZoneId.of("Europe/Paris")

    private val FRENCH_LOCALE = Locale.FRENCH
    private val DAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM", FRENCH_LOCALE)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH'h'mm", FRENCH_LOCALE)
    private val TIME_SHORT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", FRENCH_LOCALE)
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", FRENCH_LOCALE)

    /**
     * Convertit un Instant UTC (stocké dans Firestore) en ZonedDateTime à l'heure de Paris.
     */
    fun toParisDateTime(instant: Instant): ZonedDateTime {
        return instant.atZone(ZONE_PARIS)
    }

    /**
     * Convertit une date et une heure locales à Paris en Instant UTC pour Firestore.
     */
    fun toInstantFromParis(date: LocalDate, time: LocalTime): Instant {
        val zonedDateTime = ZonedDateTime.of(date, time, ZONE_PARIS)
        return zonedDateTime.toInstant()
    }

    /**
     * Retourne la date courante à Paris.
     */
    fun todayInParis(): LocalDate {
        return LocalDate.now(ZONE_PARIS)
    }

    /**
     * Formate une date en français, ex: "Lundi 26 octobre"
     */
    fun formatDayMonth(date: LocalDate): String {
        val formatted = date.format(DAY_MONTH_FORMATTER)
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(FRENCH_LOCALE) else it.toString() }
    }

    /**
     * Formate le mois et l'année en français, ex: "Octobre 2026"
     */
    fun formatMonthYear(date: LocalDate): String {
        val formatted = date.format(MONTH_YEAR_FORMATTER)
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(FRENCH_LOCALE) else it.toString() }
    }

    /**
     * Formate un horaire en français, ex: "14h30"
     */
    fun formatTime(time: LocalTime): String {
        return time.format(TIME_FORMATTER)
    }

    /**
     * Formate un horaire court, ex: "14:30"
     */
    fun formatTimeShort(time: LocalTime): String {
        return time.format(TIME_SHORT_FORMATTER)
    }

    /**
     * Calcule le lundi de la semaine contenant la date fournie.
     */
    fun getMondayOfWeek(date: LocalDate): LocalDate {
        val dayOfWeekValue = date.dayOfWeek.value // 1 = Lundi, 7 = Dimanche
        return date.minusDays((dayOfWeekValue - 1).toLong())
    }

    /**
     * Retourne les 7 jours de la semaine (du lundi au dimanche).
     */
    fun getDaysOfWeek(referenceDate: LocalDate): List<LocalDate> {
        val monday = getMondayOfWeek(referenceDate)
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    /**
     * Calcule si un événement s'étalant sur un intervalle [startInstant, endInstant]
     * couvre tout ou partie de la journée spécifiée à l'heure de Paris.
     */
    fun isSpanningDay(startInstant: Instant, endInstant: Instant, day: LocalDate): Boolean {
        val dayStart = day.atStartOfDay(ZONE_PARIS).toInstant()
        val dayEnd = day.plusDays(1).atStartOfDay(ZONE_PARIS).toInstant()

        // Intervalle [début, fin) : un rendez-vous se terminant à 00:00 ne
        // doit pas être répété sur le jour suivant. Un événement ponctuel
        // sans durée reste visible sur sa date de début.
        if (!endInstant.isAfter(startInstant)) {
            return !startInstant.isBefore(dayStart) && startInstant.isBefore(dayEnd)
        }
        return startInstant.isBefore(dayEnd) && endInstant.isAfter(dayStart)
    }
}
