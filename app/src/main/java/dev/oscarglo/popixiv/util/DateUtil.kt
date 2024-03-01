package dev.oscarglo.popixiv.util

import android.icu.text.SimpleDateFormat
import java.util.Locale

val pixivDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
val displayShortDateFormat get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
val displayDateTimeFormat get() = SimpleDateFormat("d MMMM yyyy H:mm", Locale.getDefault())