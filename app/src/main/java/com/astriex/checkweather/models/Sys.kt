package com.astriex.checkweather.models

import java.io.Serializable

data class Sys(
    val type: Int,
    val id: Int,
    val message: Double,
    val country: String,
    val sunrise: Int,
    val sunset: Int
): Serializable