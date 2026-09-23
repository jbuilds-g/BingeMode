package com.example.data.backup

import com.example.data.model.Setting
import com.example.data.model.Show
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONException
import org.json.JSONObject

data class BackupPayload(
    val shows: List<Show>,
    val settings: List<Setting>
)

@JsonClass(generateAdapter = true)
internal data class BackupEnvelope(
    val version: Int,
    val shows: List<Show>? = emptyList(),
    val settings: List<Setting>? = emptyList()
)

object BackupParser {
    const val CURRENT_VERSION = 2
    const val TMDB_KEY_SETTING = "tmdb_key"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val showListAdapter: JsonAdapter<List<Show>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, Show::class.java)
    )
    private val envelopeAdapter: JsonAdapter<BackupEnvelope> = moshi.adapter(BackupEnvelope::class.java)

    fun serialize(shows: List<Show>, settings: List<Setting>): String {
        val safeSettings = settings.filter { it.key != TMDB_KEY_SETTING }
        return envelopeAdapter.toJson(
            BackupEnvelope(
                version = CURRENT_VERSION,
                shows = shows,
                settings = safeSettings
            )
        )
    }

    fun parse(json: String): BackupPayload {
        val normalized = json.trim()
        require(normalized.isNotEmpty()) { "Backup file is empty" }

        return try {
            val objectJson = JSONObject(normalized)
            require(objectJson.has("version")) { "Missing backup version" }

            val version = objectJson.getInt("version")
            require(version in 1..CURRENT_VERSION) {
                "Unsupported backup version: $version"
            }

            val envelope = envelopeAdapter.fromJson(normalized)
                ?: throw IllegalArgumentException("Invalid backup envelope")
            val shows = envelope.shows
                ?: throw IllegalArgumentException("Backup is missing its shows list")

            validateShows(shows)
            BackupPayload(shows, envelope.settings.orEmpty().filter { it.key != TMDB_KEY_SETTING })
        } catch (_: JSONException) {
            // Pre-versioned BingeMode backups were raw Show arrays.
            val shows = showListAdapter.fromJson(normalized)
                ?: throw IllegalArgumentException("Invalid backup format")
            validateShows(shows)
            BackupPayload(shows, emptyList())
        }
    }

    private fun validateShows(shows: List<Show>) {
        require(shows.none { it.title.isBlank() }) {
            "Backup contains a show with no title"
        }
    }
}
