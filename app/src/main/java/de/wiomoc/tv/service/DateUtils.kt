package de.wiomoc.tv.service

import java.text.SimpleDateFormat
import java.util.*

fun formatTime(date: Date) = String.format("%02d:%02d", date.hours, date.minutes)

var dayFormatter = SimpleDateFormat("EEEE")
fun getFriendlyDay(date: Date) = if (date.time / 86400000 == System.currentTimeMillis() / 86400000) "Today" else dayFormatter.format(date)
