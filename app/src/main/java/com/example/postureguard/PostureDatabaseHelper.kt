package com.example.postureguard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostureDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val DATABASE_NAME = "posture.db"
        private const val DATABASE_VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE posture_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                user_id TEXT NOT NULL,
                session_id TEXT,
                start_time TEXT,
                end_time TEXT,
                bad_posture_count INTEGER,
                forwardhead_count INTEGER,
                crossleg_count INTEGER,
                standard_count INTEGER,
                last_modified TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE posture_data ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE posture_data ADD COLUMN last_modified TEXT NOT NULL DEFAULT ''")
        }
    }

    fun insertPostureData(
        userId: String,
        sessionId: String,
        startTime: String,
        endTime: String,
        badPostureCount: Int,
        forwardheadCount: Int,
        crosslegCount: Int,
        standardCount: Int
    ) {
        val db = writableDatabase
        val uuid = UUID.randomUUID().toString()
        val lastModified = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put("uuid", uuid)
            put("user_id", userId)
            put("session_id", sessionId)
            put("start_time", startTime)
            put("end_time", endTime)
            put("bad_posture_count", badPostureCount)
            put("forwardhead_count", forwardheadCount)
            put("crossleg_count", crosslegCount)
            put("standard_count", standardCount)
            put("last_modified", lastModified)
        }
        db.insert("posture_data", null, values)
        insertPostureDataToFirestore(uuid, userId, sessionId, startTime, endTime, badPostureCount, forwardheadCount, crosslegCount, standardCount, lastModified)
    }

    fun insertPostureDataToFirestore(
        uuid: String,
        userId: String,
        sessionId: String,
        startTime: String,
        endTime: String,
        badPostureCount: Int,
        forwardheadCount: Int,
        crosslegCount: Int,
        standardCount: Int,
        lastModified: String
    ) {
        val postureData = hashMapOf(
            "uuid" to uuid,
            "user_id" to userId,
            "session_id" to sessionId,
            "start_time" to startTime,
            "end_time" to endTime,
            "bad_posture_count" to badPostureCount,
            "forwardhead_count" to forwardheadCount,
            "crossleg_count" to crosslegCount,
            "standard_count" to standardCount,
            "last_modified" to lastModified
        )

        firestore.collection("posture_data")
            .document(uuid)
            .set(postureData)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }
    }

    suspend fun getPostureDataByDate(userId: String, date: String): List<PostureData> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(
            "posture_data",
            null,
            "user_id = ? AND DATE(start_time) = ?",
            arrayOf(userId, date),
            null,
            null,
            null
        )
        val postureDataList = mutableListOf<PostureData>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        with(cursor) {
            while (moveToNext()) {
                val uuid = getString(getColumnIndexOrThrow("uuid"))
                val sessionId = getString(getColumnIndexOrThrow("session_id"))
                val startTime = getString(getColumnIndexOrThrow("start_time"))
                val endTime = getString(getColumnIndexOrThrow("end_time"))
                val badPostureCount = getInt(getColumnIndexOrThrow("bad_posture_count"))
                val forwardheadCount = getInt(getColumnIndexOrThrow("forwardhead_count"))
                val crosslegCount = getInt(getColumnIndexOrThrow("crossleg_count"))
                val standardCount = getInt(getColumnIndexOrThrow("standard_count"))
                val lastModified = getString(getColumnIndexOrThrow("last_modified"))
                val totalMonitoringTime = (dateFormat.parse(endTime)?.time ?: 0L) - (dateFormat.parse(startTime)?.time ?: 0L)
                val postureScore = calculatePostureScore(standardCount, forwardheadCount, crosslegCount)
                postureDataList.add(PostureData(uuid, sessionId, startTime, endTime, badPostureCount, forwardheadCount, crosslegCount, standardCount, totalMonitoringTime, postureScore, lastModified))
            }
        }
        cursor.close()
        return@withContext postureDataList
    }

    fun getPostureDataByDateRange(userId: String, fromDate: String, toDate: String): List<PostureData> {
        val db = readableDatabase
        val cursor = db.query(
            "posture_data",
            null,
            "user_id = ? AND DATE(start_time) BETWEEN ? AND ?",
            arrayOf(userId, fromDate, toDate),
            null,
            null,
            null
        )
        val postureDataList = mutableListOf<PostureData>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        with(cursor) {
            while (moveToNext()) {
                val uuid = getString(getColumnIndexOrThrow("uuid"))
                val sessionId = getString(getColumnIndexOrThrow("session_id"))
                val startTime = getString(getColumnIndexOrThrow("start_time"))
                val endTime = getString(getColumnIndexOrThrow("end_time"))
                val badPostureCount = getInt(getColumnIndexOrThrow("bad_posture_count"))
                val forwardheadCount = getInt(getColumnIndexOrThrow("forwardhead_count"))
                val crosslegCount = getInt(getColumnIndexOrThrow("crossleg_count"))
                val standardCount = getInt(getColumnIndexOrThrow("standard_count"))
                val lastModified = getString(getColumnIndexOrThrow("last_modified"))
                val totalMonitoringTime = (dateFormat.parse(endTime)?.time ?: 0L) - (dateFormat.parse(startTime)?.time ?: 0L)
                val postureScore = calculatePostureScore(standardCount, forwardheadCount, crosslegCount)
                postureDataList.add(PostureData(uuid, sessionId, startTime, endTime, badPostureCount, forwardheadCount, crosslegCount, standardCount, totalMonitoringTime, postureScore, lastModified))
            }
        }
        cursor.close()
        return postureDataList
    }

    private fun calculatePostureScore(standardCount: Int, forwardheadCount: Int, crosslegCount: Int): Int {
        val totalPostureCount = standardCount + forwardheadCount + crosslegCount
        return if (totalPostureCount > 0) {
            ((standardCount.toDouble() / totalPostureCount) * 100).toInt()
        } else {
            0
        }
    }

    fun syncDataWithFirestore() {
        val db = readableDatabase
        val cursor = db.query("posture_data", null, null, null, null, null, null)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        with(cursor) {
            while (moveToNext()) {
                val uuid = getString(getColumnIndexOrThrow("uuid"))
                val userId = getString(getColumnIndexOrThrow("user_id"))
                val sessionId = getString(getColumnIndexOrThrow("session_id"))
                val startTime = getString(getColumnIndexOrThrow("start_time"))
                val endTime = getString(getColumnIndexOrThrow("end_time"))
                val badPostureCount = getInt(getColumnIndexOrThrow("bad_posture_count"))
                val forwardheadCount = getInt(getColumnIndexOrThrow("forwardhead_count"))
                val crosslegCount = getInt(getColumnIndexOrThrow("crossleg_count"))
                val standardCount = getInt(getColumnIndexOrThrow("standard_count"))
                val lastModified = getString(getColumnIndexOrThrow("last_modified"))

                firestore.collection("posture_data")
                    .document(uuid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val firestoreLastModified = document.getString("last_modified")
                            if (firestoreLastModified != null && dateFormat.parse(lastModified)?.let { localDate ->
                                    dateFormat.parse(firestoreLastModified)?.let { firestoreDate ->
                                        localDate > firestoreDate
                                    }
                                } == true) {
                                insertPostureDataToFirestore(uuid, userId, sessionId, startTime, endTime, badPostureCount, forwardheadCount, crosslegCount, standardCount, lastModified)
                            }
                        } else {
                            insertPostureDataToFirestore(uuid, userId, sessionId, startTime, endTime, badPostureCount, forwardheadCount, crosslegCount, standardCount, lastModified)
                        }
                    }
            }
        }
        cursor.close()
    }

}

data class PostureData(
    val uuid: String,
    val sessionId: String,
    val startTime: String,
    val endTime: String,
    val badPostureCount: Int,
    val forwardheadCount: Int,
    val crosslegCount: Int,
    val standardCount: Int,
    var totalMonitoringTime: Long,
    var postureScore: Int,
    val lastModified: String
)