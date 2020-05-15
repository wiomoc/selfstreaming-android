package de.selfnet.tv.service

import java.text.SimpleDateFormat
import java.util.*

fun formatTime(date: Date) = String.format("%02d:%02d", date.hours, date.minutes)

var dayFormatter = SimpleDateFormat("EEEE")
fun getFriendlyDay(date: Date) =
    if (date.isToday) "Heute" else dayFormatter.format(date)

val Date.isToday
    get() = System.currentTimeMillis().let {
        (time + TimeZone.getDefault().getOffset(it)) / 86400000 == it / 86400000
    }

