package io.otrade.data

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String,
    val name: String,
    val description: String,
    val performance: StockPerformance,
) {

    @Serializable
    data class StockPerformance(
        val currentValue: Float,
        val lastValues: List<Float>,
    ) {
        fun toList(): List<Float> {
            return lastValues + currentValue
        }
    }

}
