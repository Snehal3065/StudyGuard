package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BrainDumpNote(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BrainDumpManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_braindump_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTES_JSON = "braindump_notes"

        @Volatile
        private var instance: BrainDumpManager? = null

        fun getInstance(context: Context): BrainDumpManager {
            return instance ?: synchronized(this) {
                instance ?: BrainDumpManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getNotes(): List<BrainDumpNote> {
        val json = prefs.getString(KEY_NOTES_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<BrainDumpNote>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BrainDumpNote(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveNotes(notes: List<BrainDumpNote>) {
        val arr = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject().apply {
                put("id", n.id)
                put("text", n.text)
                put("timestamp", n.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_NOTES_JSON, arr.toString()).apply()
    }

    fun addNote(text: String): BrainDumpNote {
        val note = BrainDumpNote(text = text.trim())
        val current = getNotes().toMutableList().apply { add(0, note) }
        saveNotes(current)
        return note
    }

    fun deleteNote(id: String) {
        val current = getNotes().filterNot { it.id == id }
        saveNotes(current)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_NOTES_JSON).apply()
    }
}
