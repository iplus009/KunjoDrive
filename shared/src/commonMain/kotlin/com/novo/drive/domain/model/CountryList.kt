package com.novo.drive.domain.model

data class CountryList(
    val countries: List<Country>,
    val detected: Country?
)
