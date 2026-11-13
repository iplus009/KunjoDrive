package com.novo.drive.domain.model

data class Exclusion(
    val id: String,
    val type: ExclusionType,
    val value: String,
    val enabled: Boolean,
    val createdAt: Long
)
