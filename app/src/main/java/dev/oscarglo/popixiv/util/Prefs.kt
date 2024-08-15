package dev.oscarglo.popixiv.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

enum class Prefs(private val default: String = "") {
    PIXIV_ACCESS_TOKEN,
    PIXIV_REFRESH_TOKEN,
    SAUCENAO_TOKEN,
    APPEARANCE_THEME("system"),
    APPEARANCE_BLUR_R18("true"),
    APPEARANCE_GRID_STAGGER("false"),
    APPEARANCE_GRID_GAP("2"),
    APPEARANCE_CARD_SIZE("128"),
    APPEARANCE_CARD_MULTI("false");

    fun exists() = prefs.contains(name)

    fun get() = prefs.getString(name, default)!!

    fun set(value: String) = prefs.edit().putString(name, value).apply()

    @Composable
    fun state(): MutableState<String> {
        val property = rememberSaveable { mutableStateOf(get()) }
        var value by property

        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == name) value = get()
        }

        LaunchedEffect(value) { set(value) }

        return property
    }

    @Composable
    fun <T> parsedState(serialize: (T) -> String = { it.toString() }, parse: (String) -> T): MutableState<T> {
        var value by state()
        val property = rememberSaveable { mutableStateOf(parse(value)) }
        var typedValue by property

        LaunchedEffect(value) { typedValue = parse(value) }
        LaunchedEffect(typedValue) { value = serialize(typedValue) }

        return property
    }

    @Composable
    fun booleanState() = parsedState { it == "true" }

    @Composable
    fun intState() = parsedState { it.toInt() }

    companion object {
        lateinit var prefs: SharedPreferences

        fun init(activity: Activity) {
            prefs = activity.getSharedPreferences("popixiv", Context.MODE_PRIVATE)
        }
    }
}