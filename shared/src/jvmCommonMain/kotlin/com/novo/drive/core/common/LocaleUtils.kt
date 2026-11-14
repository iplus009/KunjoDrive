package com.novo.drive.core.common

import com.novo.drive.domain.model.AppLanguage
import java.util.Locale

fun isBengaliRuntime(language: AppLanguage? = null): Boolean = when (language) {
    AppLanguage.BENGALI -> true
    AppLanguage.ENGLISH -> false
    AppLanguage.SYSTEM, null -> Locale.getDefault().language.startsWith("bn", ignoreCase = true)
}

fun bi(en: String, bn: String, language: AppLanguage? = null): String =
    if (isBengaliRuntime(language)) bn else en
