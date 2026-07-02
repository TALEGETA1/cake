package com.example.utils

import androidx.compose.runtime.*
import java.util.Calendar

object CountdownHelper {

    fun getDeliveryDateTimeMillis(deliveryDateMillis: Long, deliveryTimeStr: String): Long {
        return try {
            val clean = deliveryTimeStr.trim().uppercase()
            val parts = clean.split(":")
            if (parts.size >= 2) {
                var hour = parts[0].toIntOrNull() ?: 0
                val minPart = parts[1].split(" ")
                val minute = minPart[0].toIntOrNull() ?: 0
                val ampm = if (minPart.size > 1) minPart[1] else ""
                
                if (ampm == "PM" && hour < 12) hour += 12
                if (ampm == "AM" && hour == 12) hour = 0
                
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = deliveryDateMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            } else {
                deliveryDateMillis
            }
        } catch (e: Exception) {
            deliveryDateMillis
        }
    }

    fun getCountdownText(deliveryDateMillis: Long, deliveryTimeStr: String, status: String): String {
        if (status == "Completed" || status == "Cancelled") return ""
        val targetMillis = getDeliveryDateTimeMillis(deliveryDateMillis, deliveryTimeStr)
        val diff = targetMillis - System.currentTimeMillis()
        val isPast = diff < 0
        val absDiff = kotlin.math.abs(diff)
        
        val days = absDiff / (24 * 60 * 60 * 1000)
        val hours = (absDiff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutes = (absDiff % (60 * 60 * 1000)) / (60 * 1000)
        
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0 || (days == 0L && hours == 0L)) parts.add("${minutes}m")
        
        val timeStr = parts.joinToString(" ")
        return if (isPast) {
            "($timeStr overdue)"
        } else {
            "($timeStr left)"
        }
    }
}

@Composable
fun rememberCountdownText(deliveryDateMillis: Long, deliveryTimeStr: String, status: String): String {
    var countdown by remember { mutableStateOf("") }
    LaunchedEffect(deliveryDateMillis, deliveryTimeStr, status) {
        while (true) {
            countdown = CountdownHelper.getCountdownText(deliveryDateMillis, deliveryTimeStr, status)
            kotlinx.coroutines.delay(10000) // update every 10 seconds
        }
    }
    return countdown
}
