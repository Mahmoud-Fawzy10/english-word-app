package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.WordLocation
import org.json.JSONArray
import org.json.JSONObject

class WordTypeConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        val jsonArray = JSONArray()
        value?.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromLocationList(value: List<WordLocation>?): String {
        val jsonArray = JSONArray()
        value?.forEach { loc ->
            val obj = JSONObject().apply {
                put("book", loc.book)
                put("unit", loc.unit)
                put("lesson", loc.lesson)
                put("page", loc.page)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toLocationList(value: String?): List<WordLocation> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        WordLocation(
                            book = obj.optString("book", ""),
                            unit = obj.optString("unit", ""),
                            lesson = obj.optString("lesson", ""),
                            page = obj.optString("page", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
