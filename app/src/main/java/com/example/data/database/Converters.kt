package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.SeasonInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    companion object {
        private val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        
        private val seasonListType = Types.newParameterizedType(List::class.java, SeasonInfo::class.java)
        private val adapter = moshi.adapter<List<SeasonInfo>>(seasonListType)
    }

    @TypeConverter
    fun fromSeasonList(seasons: List<SeasonInfo>?): String? {
        return seasons?.let { adapter.toJson(it) }
    }

    @TypeConverter
    fun toSeasonList(json: String?): List<SeasonInfo>? {
        return json?.let { adapter.fromJson(it) }
    }
}
