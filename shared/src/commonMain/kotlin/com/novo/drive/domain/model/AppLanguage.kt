package com.novo.drive.domain.model

enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    BENGALI("bn");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code == null) return SYSTEM
            return entries.firstOrNull {
                it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true)
            } ?: SYSTEM
        }
    }
}
