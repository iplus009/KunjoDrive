package com.novo.drive.domain.model

enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    BENGALI("bn");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code == null) return ENGLISH
            return entries.firstOrNull {
                it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true)
            } ?: ENGLISH
        }
    }
}
