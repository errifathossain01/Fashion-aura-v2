package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.OrderItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class OrderTypeConverters {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val orderItemListType = Types.newParameterizedType(List::class.java, OrderItem::class.java)
    private val orderItemAdapter = moshi.adapter<List<OrderItem>>(orderItemListType)

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private val stringMapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val stringMapAdapter = moshi.adapter<Map<String, String>>(stringMapType)

    @TypeConverter
    fun fromOrderItemList(items: List<OrderItem>?): String {
        return orderItemAdapter.toJson(items ?: emptyList())
    }

    @TypeConverter
    fun toOrderItemList(json: String?): List<OrderItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            orderItemAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return stringListAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        return stringMapAdapter.toJson(map ?: emptyMap())
    }

    @TypeConverter
    fun toStringMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            stringMapAdapter.fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
