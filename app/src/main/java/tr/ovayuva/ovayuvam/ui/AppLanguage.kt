package tr.ovayuva.ovayuvam.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLanguage {
    val choices = linkedMapOf("en" to "English", "de" to "Deutsch", "tr" to "Türkçe",
        "ru" to "Русский", "es" to "Español", "fr" to "Français")

    fun selected(context: Context): String = if (Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags().substringBefore(',')
    } else context.getSharedPreferences("language", Context.MODE_PRIVATE).getString("code", "").orEmpty()

    fun wrap(context: Context): Context {
        val code = selected(context)
        if (code.isEmpty()) return context
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(code)))
        })
    }

    fun choose(activity: Activity, code: String) {
        require(code.isEmpty() || code in choices)
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(code)
        } else {
            activity.getSharedPreferences("language", Context.MODE_PRIVATE).edit().putString("code", code).commit()
            activity.recreate()
        }
    }
}
