package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.model.TouchPoint
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "click_profiles")
data class ClickProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val intervalMs: Long = 1000L,
    val points: List<TouchPoint> = emptyList(),
    val linkedAppPackage: String? = null,
    val autoLaunchEnabled: Boolean = true,
    val isAdvanced: Boolean = false
)

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, TouchPoint::class.java)
    private val adapter = moshi.adapter<List<TouchPoint>>(listType)

    @TypeConverter
    fun fromString(value: String?): List<TouchPoint> {
        if (value == null) return emptyList()
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<TouchPoint>?): String {
        return adapter.toJson(list ?: emptyList())
    }
}

@Dao
interface ClickProfileDao {
    @Query("SELECT * FROM click_profiles ORDER BY id DESC")
    fun getAllProfiles(): Flow<List<ClickProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ClickProfile): Long

    @Update
    suspend fun updateProfile(profile: ClickProfile)

    @Delete
    suspend fun deleteProfile(profile: ClickProfile)

    @Query("SELECT * FROM click_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): ClickProfile?

    @Query("SELECT * FROM click_profiles WHERE linkedAppPackage = :packageName LIMIT 1")
    suspend fun getProfileByPackage(packageName: String): ClickProfile?

    @Query("DELETE FROM click_profiles")
    suspend fun deleteAllProfiles()
}

@Database(entities = [ClickProfile::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clickProfileDao(): ClickProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE click_profiles ADD COLUMN isAdvanced INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoclicker-db"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ClickProfileRepository(private val dao: ClickProfileDao) {
    val allProfiles: Flow<List<ClickProfile>> = dao.getAllProfiles()

    suspend fun insert(profile: ClickProfile): Long = dao.insertProfile(profile)

    suspend fun update(profile: ClickProfile) = dao.updateProfile(profile)

    suspend fun delete(profile: ClickProfile) = dao.deleteProfile(profile)

    suspend fun getById(id: Int): ClickProfile? = dao.getProfileById(id)
}
