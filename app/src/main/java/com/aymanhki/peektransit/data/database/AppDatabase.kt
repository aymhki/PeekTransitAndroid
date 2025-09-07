package com.aymanhki.peektransit.data.database


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aymanhki.peektransit.data.models.SavedStop
import com.aymanhki.peektransit.data.models.Stop
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.aymanhki.peektransit.data.models.FolderCategory
import com.aymanhki.peektransit.data.models.SavedStopsViewMode
import androidx.room.*
import androidx.room.migration.Migration


@Entity(tableName = "saved_stops")
data class SavedStopEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "stop_data_json")
    val stopDataJson: String,

    @ColumnInfo(name = "folder_categories_json")
    val folderCategoriesJson: String?
)



private val gson: Gson = GsonBuilder()
    .serializeSpecialFloatingPointValues()
    .create()

fun SavedStop.toEntity(): SavedStopEntity {
    val stopDataJson = gson.toJson(this.stopData)
    val folderCategoriesJson = this.folderCategories?.let { gson.toJson(it) }
    return SavedStopEntity(
        id = this.id,
        stopDataJson = stopDataJson,
        folderCategoriesJson = folderCategoriesJson
    )
}

fun SavedStopEntity.toModel(): SavedStop {
    val stopDataType = object : TypeToken<Stop>() {}.type
    val stopData: Stop = gson.fromJson(this.stopDataJson, stopDataType)

    val folderCategories: List<String>? = this.folderCategoriesJson?.let {
        val listType = object : TypeToken<List<String>>() {}.type
        gson.fromJson(it, listType)
    }

    return SavedStop(
        id = this.id,
        stopData = stopData,
        folderCategories = folderCategories
    )
}

@Entity(tableName = "folder_categories")
data class FolderCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "icons_json")
    val iconsJson: String,
    @ColumnInfo(name = "stop_order_json")
    val stopOrderJson: String,
    @ColumnInfo(name = "view_mode")
    val viewMode: String
)


fun FolderCategory.toEntity(): FolderCategoryEntity {
    return FolderCategoryEntity(
        id = this.id,
        name = this.name,
        iconsJson = gson.toJson(this.icons),
        stopOrderJson = gson.toJson(this.stopOrder),
        viewMode = SavedStopsViewMode.DEFAULT.name
    )
}

fun FolderCategoryEntity.toModel(): FolderCategory {
    val listStringType = object : TypeToken<List<String>>() {}.type
    return FolderCategory(
        id = this.id,
        name = this.name,
        icons = gson.fromJson(this.iconsJson, listStringType),
        stopOrder = gson.fromJson(this.stopOrderJson, listStringType)
    )
}



@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)



@Dao
interface SavedStopDao {
    @Query("SELECT * FROM saved_stops")
    fun getAll(): Flow<List<SavedStopEntity>>

    @Query("SELECT * FROM saved_stops WHERE folder_categories_json IS NULL OR folder_categories_json = '[]'")
    fun getUncategorized(): Flow<List<SavedStopEntity>>

    @Query("SELECT * FROM saved_stops WHERE id IN (:stopIds)")
    suspend fun getStopsByIds(stopIds: List<String>): List<SavedStopEntity>

    @Query("SELECT * FROM saved_stops WHERE id = :stopId")
    suspend fun getStopById(stopId: String): SavedStopEntity?

    @Query("UPDATE saved_stops SET folder_categories_json = :folderIdsJson WHERE id = :stopId")
    suspend fun updateFolderCategories(stopId: String, folderIdsJson: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_stops WHERE id = :stopId LIMIT 1)")
    fun isSaved(stopId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stop: SavedStopEntity)

    @Query("DELETE FROM saved_stops WHERE id = :stopId")
    suspend fun deleteById(stopId: String)

    @Query("DELETE FROM saved_stops WHERE id IN (:stopIds)")
    suspend fun deleteByIds(stopIds: List<String>)

    @Query("DELETE FROM saved_stops")
    suspend fun clearAll()

    @Query("SELECT EXISTS(SELECT 1 FROM saved_stops WHERE id = :stopId LIMIT 1)")
    suspend fun isStopSaved(stopId: String): Boolean
}


@Dao
interface FolderCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderCategoryEntity)

    @Update
    suspend fun update(folder: FolderCategoryEntity)

    @Query("SELECT * FROM folder_categories")
    fun getAll(): Flow<List<FolderCategoryEntity>>

    @Query("SELECT * FROM folder_categories WHERE id = :folderId")
    suspend fun getById(folderId: String): FolderCategoryEntity?

    @Query("DELETE FROM folder_categories WHERE id IN (:folderIds)")
    suspend fun deleteByIds(folderIds: List<String>)
}


@Dao
interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: AppSettingsEntity)

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    fun getValue(key: String): Flow<String?>

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getValueOnce(key: String): String?
}

@Database(
    entities = [SavedStopEntity::class, FolderCategoryEntity::class, AppSettingsEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savedStopDao(): SavedStopDao
    abstract fun folderCategoryDao(): FolderCategoryDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val MIGRATION_PREFS = "peek_transit_migration_prefs"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "peek_transit_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()


                runMigrationIfNeeded(context, instance)

                INSTANCE = instance
                instance
            }
        }

        private fun runMigrationIfNeeded(context: Context, database: AppDatabase) {
            val migrationPrefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)


            CoroutineScope(Dispatchers.IO).launch {
                migrateFromSharedPreferences(context, database.savedStopDao())
            }

        }

        private suspend fun migrateFromSharedPreferences(context: Context, dao: SavedStopDao) {
            val oldPreferences = context.getSharedPreferences("savedStops", Context.MODE_PRIVATE)
            val json = oldPreferences.getString("savedStops", null) ?: return

            if (json.isBlank()) {
                return
            }

            try {
                val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
                val type = object : TypeToken<List<SavedStop>>() {}.type
                val oldSavedStops: List<SavedStop> = gson.fromJson(json, type) ?: emptyList()

                if (oldSavedStops.isNotEmpty()) {
                    val entities = oldSavedStops.map { it.toEntity() }
                    entities.forEach { dao.insert(it) }
                    oldPreferences.edit { clear() }
                }
            } catch (e: Exception) {
                Log.e("DatabaseMigration", "Failed to migrate data from SharedPreferences", e)
            }
        }
    }
}
