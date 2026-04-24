package com.helpteach.offline.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// -----------------------------
// ENTITIES (Jadvallar)
// -----------------------------

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1, // Only one profile needed
    val fullName: String,
    val role: String, // "teacher", "student", "other"
    val organization: String
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1, // Only one settings needed
    val morningTime: String = "07:00",
    val eveningTime: String = "21:00",
    val notifyBefore30: Boolean = true,
    val notifyBefore10: Boolean = true,
    val notifyOnTime: Boolean = true,
    val doNotDisturb: Boolean = false,
    val weatherCity: String = "Qarshi",
    val baseWeekIsOdd: Boolean = true,
    val baseWeekNumber: Int = -1
) {
    fun isOddWeek(currentWeekOfYear: Int): Boolean {
        return if (baseWeekNumber != -1) {
            val diff = currentWeekOfYear - baseWeekNumber
            if (diff % 2 == 0) baseWeekIsOdd else !baseWeekIsOdd
        } else {
            currentWeekOfYear % 2 != 0
        }
    }
}

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val dayOfWeek: Int, // 0 = Monday, 6 = Sunday (matching bot)
    val startTime: String, // format: "HH:mm"
    val endTime: String,
    val room: String,
    val groupName: String,
    val lessonType: String, // "lecture", "practical", "lab", "course", "seminar", "other"
    val weekType: String, // "every", "odd", "even"
    val customAudioUri: String? = null
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String?,
    val dueDate: String?, // "YYYY-MM-DD" or null
    val isDone: Boolean = false,
    val customAudioUri: String? = null
)

@Entity(tableName = "reminders")
data class CustomReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val remindAt: String, // ISO date string or timestamp
    val repeatType: String, // "none", "daily", "weekly"
    val isSent: Boolean = false
)

// -----------------------------
// DAOs (Ma'lumotlar bilan ishlash)
// -----------------------------

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun getProfile(): Flow<Profile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: Profile)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<Settings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: Settings)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY dayOfWeek, startTime")
    fun getAllLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE dayOfWeek = :day ORDER BY startTime")
    fun getLessonsByDay(day: Int): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonById(id: Int): Lesson?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson): Long

    @Delete
    suspend fun deleteLesson(lesson: Lesson)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY dueDate ASC")
    fun getPendingTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDone = 1")
    fun getCompletedTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE title = :title LIMIT 1")
    suspend fun getTaskByTitle(title: String): Task?
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isSent = 0 ORDER BY remindAt ASC")
    fun getPendingReminders(): Flow<List<CustomReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: CustomReminder)

    @Delete
    suspend fun deleteReminder(reminder: CustomReminder)
}

// -----------------------------
// DATABASE (Asosiy Baza)
// -----------------------------

@Database(
    entities = [Profile::class, Settings::class, Lesson::class, Task::class, CustomReminder::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun settingsDao(): SettingsDao
    abstract fun lessonDao(): LessonDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "helpteach_database"
                )
                .fallbackToDestructiveMigration() // Eslab qoling, bu eski ma'lumotlarni o'chirib yangidan yaratadi (versiya o'zgarganda)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
