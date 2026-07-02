package com.example.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object EthiopianCalendarHelper {

    val ETHIOPIAN_MONTHS = listOf(
        "Meskerem", "Tekemt", "Hadar", "Tahsas", "Tir", "Yakatit",
        "Meggabit", "Miyazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume"
    )

    data class EthiopianDate(val year: Int, val month: Int, val day: Int) {
        val monthName: String
            get() = if (month in 1..13) ETHIOPIAN_MONTHS[month - 1] else "Unknown"

        fun format(): String {
            return "$monthName $day, $year"
        }
    }

    /**
     * Converts Gregorian LocalDate to EthiopianDate.
     */
    fun gregorianToEthiopian(date: LocalDate): EthiopianDate {
        var ethYear = date.year - 8
        var meskerem1 = getMeskerem1(ethYear)

        if (date.isBefore(meskerem1)) {
            ethYear -= 1
            meskerem1 = getMeskerem1(ethYear)
        }

        val daysDiff = date.toEpochDay() - meskerem1.toEpochDay()
        val ethMonth = (daysDiff / 30).toInt() + 1
        val ethDay = (daysDiff % 30).toInt() + 1

        return EthiopianDate(ethYear, ethMonth, ethDay)
    }

    /**
     * Converts Ethiopian date to Gregorian LocalDate.
     */
    fun ethiopianToGregorian(ethYear: Int, ethMonth: Int, ethDay: Int): LocalDate {
        val meskerem1 = getMeskerem1(ethYear)
        val daysToAdd = (ethMonth - 1) * 30 + (ethDay - 1)
        return meskerem1.plusDays(daysToAdd.toLong())
    }

    /**
     * Helper to get the Gregorian date of Meskerem 1 of the given Ethiopian Year.
     */
    private fun getMeskerem1(ethYear: Int): LocalDate {
        val gregYear = ethYear + 7
        // In the Ethiopian calendar, Pagume has 6 days in a leap year (when ethYear % 4 == 3).
        // This causes the next Ethiopian Year (which starts in gregYear) to start on September 12 instead of September 11.
        val isLeap = (ethYear - 1) % 4 == 3
        return if (isLeap) {
            LocalDate.of(gregYear, 9, 12)
        } else {
            LocalDate.of(gregYear, 9, 11)
        }
    }

    /**
     * Formats a given LocalDate using the current calendar preference.
     */
    fun formatDate(date: LocalDate, useEthiopian: Boolean): String {
        return if (useEthiopian) {
            gregorianToEthiopian(date).format()
        } else {
            date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.US))
        }
    }
}
