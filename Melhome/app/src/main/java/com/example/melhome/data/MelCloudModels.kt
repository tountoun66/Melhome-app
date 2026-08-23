package com.example.melhome.data

import com.google.gson.annotations.SerializedName

data class MelHomeContext(
    @SerializedName("buildings") val buildings: List<MelHomeBuilding>?
)

data class MelHomeBuilding(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("units") val units: List<MelHomeUnit>?,
    @SerializedName("airToAirUnits") val airToAirUnits: List<MelHomeUnit>?
)

data class MelHomeUnit(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("state") val state: MelHomeState?
)

data class MelHomeState(
    @SerializedName("power") val power: String?,
    @SerializedName("targetTemperature") val targetTemperature: Double?,
    @SerializedName("targetTemp") val targetTemp: Double?,
    @SerializedName("actualTemperature") val actualTemperature: Double?,
    @SerializedName("actualTemp") val actualTemp: Double?
)
