package com.perol.asdpl.pixivez.objects

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.IntDef
import java.util.*

object LanguageUtil {

    @SuppressLint("AppBundleLocaleChanges")
    fun setLanguage(context: Context, @Language language: Int) {
        val resources = context.resources
        val configuration = resources.configuration
        val displayMetrics = resources.displayMetrics
        val newLocale = langToLocale(localeToLang(langToLocale(language)))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(newLocale)
            val localeList = LocaleList(newLocale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
            context.createConfigurationContext(configuration)
        }
        else {
            configuration.setLocale(newLocale)
            context.createConfigurationContext(configuration)
        }
        // https://developer.android.com/reference/android/content/res/Resources.html#updateConfiguration(android.content.res.Configuration,%20android.util.DisplayMetrics).
        resources.updateConfiguration(
            configuration,
            displayMetrics
        ) // This method was deprecated in API level 25.
    }

    fun localeToLang(local: Locale): Int {
        return when (local.language) {
            Locale.ENGLISH.language -> Language.ENGLISH
            Locale.JAPANESE.language -> Language.JAPANESE
            Locale.SIMPLIFIED_CHINESE.language -> Language.SIMPLIFIED_CHINESE
            Locale.TRADITIONAL_CHINESE.language -> Language.TRADITIONAL_CHINESE
            else -> Language.ENGLISH
        }
    }

    fun getLocale(): Locale {
        return langToLocale(Language.SYSTEM)
    }

    fun getLang(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales.get(0).language
        }
        else {
            Resources.getSystem().configuration.locale.language
        }
    }

    fun langToLocale(@Language language: Int): Locale {
        return when (language) {
            Language.SYSTEM -> {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Resources.getSystem().configuration.locales[0]
                }
                else {
                    Resources.getSystem().configuration.locale
                }
            }
            Language.ENGLISH -> Locale.ENGLISH
            Language.JAPANESE -> Locale.JAPANESE
            Language.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
            Language.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
            else -> Locale.SIMPLIFIED_CHINESE
        }
    }

    @IntDef(
        Language.SYSTEM,
        Language.ENGLISH,
        Language.TRADITIONAL_CHINESE,
        Language.SIMPLIFIED_CHINESE,
        Language.JAPANESE
    )
    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Language {

        companion object {
            const val SYSTEM = -1 // 跟随系统
            const val ENGLISH = 1 // 英语
            const val SIMPLIFIED_CHINESE = 0 // 简体中文
            const val TRADITIONAL_CHINESE = 2 // 繁体中文
            const val JAPANESE = 3 // 日语
        }
    }
}
