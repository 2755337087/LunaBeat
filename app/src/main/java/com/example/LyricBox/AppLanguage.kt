package com.example.LyricBox

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

const val APP_SETTINGS_PREFS_NAME = "AppSettings"
const val APP_LANGUAGE_TAG_KEY = "appLanguageTag"
const val APP_LANGUAGE_SYSTEM = "system"

object AppLanguage {
    fun getSavedLanguageTag(context: Context): String {
        return context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(APP_LANGUAGE_TAG_KEY, APP_LANGUAGE_SYSTEM)
            ?: APP_LANGUAGE_SYSTEM
    }

    fun saveLanguageTag(context: Context, languageTag: String) {
        context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(APP_LANGUAGE_TAG_KEY, languageTag)
            .apply()
    }

    fun wrapContext(base: Context): Context {
        val languageTag = getSavedLanguageTag(base)
        if (languageTag == APP_LANGUAGE_SYSTEM) return base

        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
