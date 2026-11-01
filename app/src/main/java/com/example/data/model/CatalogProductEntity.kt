package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
@Entity(tableName = "catalog_products")
data class CatalogProductEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "General",
    val basePrice: Double = 0.0,
    val availableSizes: List<String> = emptyList(),
    val availableColors: List<String> = emptyList(),
    val variantPhotos: Map<String, String> = emptyMap(),
    val stockStatus: String = "IN_STOCK", // "IN_STOCK", "PRE_ORDER"
    val photoUri: String = ""
)
