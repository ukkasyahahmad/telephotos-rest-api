package com.tes.telephotos.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

object DateUtils {
    fun formatMediaDate(timestampSeconds: Long): String {
        val calendar = Calendar.getInstance()
        val currentCalendar = Calendar.getInstance()

        calendar.timeInMillis = timestampSeconds * 1000

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val isSameYear = calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)

        if (isSameYear) {
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val currentDayOfYear = currentCalendar.get(Calendar.DAY_OF_YEAR)

            return when {
                dayOfYear == currentDayOfYear -> "Today"
                dayOfYear == currentDayOfYear - 1 -> "Yesterday"
                else -> {
                    val dayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                    dayFormat.format(calendar.time)
                }
            }
        }

        return dateFormat.format(calendar.time)
    }
}