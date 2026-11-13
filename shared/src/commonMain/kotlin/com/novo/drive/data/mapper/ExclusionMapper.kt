package com.novo.drive.data.mapper

import com.novo.drive.data.local.entity.ExclusionEntity
import com.novo.drive.domain.model.Exclusion

fun ExclusionEntity.toDomain(): Exclusion = Exclusion(
    id = id,
    type = type,
    value = value,
    enabled = enabled,
    createdAt = createdAt
)
