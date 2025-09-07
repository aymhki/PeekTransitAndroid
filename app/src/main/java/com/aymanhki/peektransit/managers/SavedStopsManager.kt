package com.aymanhki.peektransit.managers

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.aymanhki.peektransit.data.cache.VariantsCacheManager
import com.aymanhki.peektransit.data.database.*
import com.aymanhki.peektransit.data.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class SavedStopsManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: SavedStopsManager? = null
        fun getInstance(context: Context): SavedStopsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SavedStopsManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private const val MAIN_VIEW_MODE_KEY = "main_view_mode"
        private const val MAIN_STOP_ORDER_KEY = "main_stop_order"
    }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val appDb = AppDatabase.getInstance(context)
    private val savedStopDao = appDb.savedStopDao()
    private val folderDao = appDb.folderCategoryDao()
    private val settingsDao = appDb.appSettingsDao()

    private val variantsCacheManager = VariantsCacheManager.getInstance(context.applicationContext)

    private val _savedStops = MutableStateFlow<List<SavedStop>>(emptyList())
    val savedStops: StateFlow<List<SavedStop>> = _savedStops.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeSavedStops()
    }

    private fun observeSavedStops() {
        savedStopDao.getAll()
            .onEach { entities ->
                _savedStops.value = entities.map { it.toModel() }
                _isLoading.value = false
            }
            .catch { e ->
                Log.e("SavedStopsManager", "Error observing saved stops", e)
                _isLoading.value = false
            }
            .launchIn(managerScope)
    }

    fun getUncategorizedStops(): Flow<List<SavedStop>> {
        val stopsFlow = savedStopDao.getUncategorized().map { entities -> entities.map { it.toModel() } }
        val orderFlow = settingsDao.getValue(MAIN_STOP_ORDER_KEY).map { json ->
            if (json == null) emptyList()
            else gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
        }

        return combine(stopsFlow, orderFlow) { stops, order ->
            if (order.isEmpty()) return@combine stops
            val stopMap = stops.associateBy { it.id }
            order.mapNotNull { stopMap[it] }
        }
    }

    fun getFolderCategoryStops(folderId: String): Flow<List<SavedStop>> {
        val folderFlow = folderDao.getAll()
            .map { folderList -> folderList.find { it.id == folderId } }

        val stopsFlow = savedStopDao.getAll()

        return combine(folderFlow, stopsFlow) { folder, stops ->
            if (folder == null) {
                return@combine emptyList<SavedStop>()
            }

            val order = gson.fromJson<List<String>>(
                folder.stopOrderJson,
                object : TypeToken<List<String>>() {}.type
            )

            if (order.isEmpty()) {
                return@combine emptyList<SavedStop>()
            }

            val stopMap = stops
                .filter { it.id in order }
                .associateBy { it.id }

            order.mapNotNull { stopId -> stopMap[stopId]?.toModel() }
        }
    }

    fun getAllSavedStopsFolderCategories(): Flow<List<FolderCategory>> {
        return folderDao.getAll().map { entities -> entities.map { it.toModel() } }
    }


    fun createSavedStopsFolderCategory(name: String, icons: List<String>) = managerScope.launch {
        _isLoading.value = true

        try {
            val newFolder = FolderCategory(name = name, icons = icons)
            folderDao.insert(newFolder.toEntity())
        } finally {
            _isLoading.value = false
        }
    }

    fun createSavedStopsFolderCategory(name: String, icons: List<String>, firstStopIds: List<String>) = managerScope.launch {
        _isLoading.value = true

        try {
            val newFolder = FolderCategory(name = name, icons = icons, stopOrder = firstStopIds)
            appDb.withTransaction {
                folderDao.insert(newFolder.toEntity())
                val stops = savedStopDao.getStopsByIds(firstStopIds)
                for (stopEntity in stops) {
                    val model = stopEntity.toModel()
                    val newCategories = (model.folderCategories ?: emptyList()) + newFolder.id
                    val updatedJson = gson.toJson(newCategories.distinct())
                    savedStopDao.updateFolderCategories(stopEntity.id, updatedJson)
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun moveUncategorizedStopsToFolders(stopIds: List<String>, folderIds: List<String>) = managerScope.launch {
        _isLoading .value = true

        try {
            appDb.withTransaction {
                for (folderId in folderIds) {
                    val folder = folderDao.getById(folderId) ?: continue
                    val model = folder.toModel()
                    val newOrder = (model.stopOrder + stopIds).distinct()
                    folderDao.update(folder.copy(stopOrderJson = gson.toJson(newOrder)))
                }

                for (stopId in stopIds) {
                    val stop = savedStopDao.getStopById(stopId) ?: continue
                    val model = stop.toModel()
                    val newCategories = ((model.folderCategories ?: emptyList()) + folderIds).distinct()
                    savedStopDao.updateFolderCategories(stop.id, gson.toJson(newCategories))
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun moveStops(stopIds: List<String>, folderIds: List<String>, makeUncategorized: Boolean = false, fromWhichFolderId: String) = managerScope.launch {
        _isLoading .value = true

        try {
            appDb.withTransaction {
                if (makeUncategorized) {
                    val allFolders = folderDao.getAll().first()
                    allFolders.forEach { folderEntity ->
                        val model = folderEntity.toModel()
                        if (model.stopOrder.any { it in stopIds }) {
                            val newOrder = model.stopOrder.filter { it !in stopIds }
                            folderDao.update(folderEntity.copy(stopOrderJson = gson.toJson(newOrder)))
                        }
                    }
                    val emptyCategoriesJson = gson.toJson(emptyList<String>())
                    stopIds.forEach { stopId ->
                        savedStopDao.updateFolderCategories(stopId, emptyCategoriesJson)
                    }
                } else {
                    folderDao.getById(fromWhichFolderId)?.let { fromFolderEntity ->
                        val model = fromFolderEntity.toModel()
                        val newOrder = model.stopOrder.filter { it !in stopIds }
                        folderDao.update(fromFolderEntity.copy(stopOrderJson = gson.toJson(newOrder)))
                    }

                    for (folderId in folderIds) {
                        val folder = folderDao.getById(folderId) ?: continue
                        val model = folder.toModel()
                        val newOrder = (model.stopOrder + stopIds).distinct()
                        folderDao.update(folder.copy(stopOrderJson = gson.toJson(newOrder)))
                    }

                    for (stopId in stopIds) {
                        val stop = savedStopDao.getStopById(stopId) ?: continue
                        val model = stop.toModel()
                        val newCategories = ((model.folderCategories ?: emptyList()) - fromWhichFolderId + folderIds).distinct()
                        savedStopDao.updateFolderCategories(stop.id, gson.toJson(newCategories))
                    }
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteSelectedFolders(folderIds: List<String>) = managerScope.launch {
        _isLoading.value = true

        try {
            appDb.withTransaction {
                val allStops = savedStopDao.getAll().first()
                val stopIdsToDelete = mutableListOf<String>()

                allStops.forEach { stopEntity ->
                    val model = stopEntity.toModel()
                    val currentCategories = model.folderCategories
                    if (currentCategories != null && currentCategories.any { it in folderIds }) {
                        val newCategories = currentCategories.filter { it !in folderIds }
                        if (newCategories.isEmpty()) {
                            stopIdsToDelete.add(stopEntity.id)
                        } else {
                            savedStopDao.updateFolderCategories(stopEntity.id, gson.toJson(newCategories))
                        }
                    }
                }

                if (stopIdsToDelete.isNotEmpty()) {
                    savedStopDao.deleteByIds(stopIdsToDelete)
                }

                folderDao.deleteByIds(folderIds)
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteSelectedSelectedStops(stopIds: List<String>, folderId: String) = managerScope.launch {
        _isLoading.value = true

        try {
            appDb.withTransaction {
                val folder = folderDao.getById(folderId)
                if (folder != null) {
                    val model = folder.toModel()
                    val newOrder = model.stopOrder.filter { it !in stopIds }
                    folderDao.update(folder.copy(stopOrderJson = gson.toJson(newOrder)))
                }

                stopIds.forEach { stopId ->
                    val stop = savedStopDao.getStopById(stopId)
                    if (stop != null) {
                        val model = stop.toModel()
                        val newCategories = model.folderCategories?.filter { it != folderId }
                        savedStopDao.updateFolderCategories(stopId, gson.toJson(newCategories))
                    }
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteUncategorizedSelectedStops(stopIds: List<String>) = managerScope.launch {
        savedStopDao.deleteByIds(stopIds)
    }

    fun getMainSavedStopsScreenViewMode(): Flow<SavedStopsViewMode> {
        return settingsDao.getValue(MAIN_VIEW_MODE_KEY).map { SavedStopsViewMode.fromString(it) }
    }

    fun getFolderSavedStopsScreenViewMode(folderId: String): Flow<SavedStopsViewMode> {
        return folderDao.getAll()
            .map { list -> list.find { it.id == folderId }?.viewMode }
            .map { SavedStopsViewMode.fromString(it) }
    }

    fun toggleMainSavedStopsScreenViewMode() = managerScope.launch {
        val currentModeString = settingsDao.getValueOnce(MAIN_VIEW_MODE_KEY)
        val currentMode = SavedStopsViewMode.fromString(currentModeString)
        val allModes = SavedStopsViewMode.entries
        val nextMode = allModes[(currentMode.ordinal + 1) % allModes.size]
        settingsDao.upsert(AppSettingsEntity(MAIN_VIEW_MODE_KEY, nextMode.name))
    }

    fun toggleFolderSavedStopsScreenViewMode(folderId: String) = managerScope.launch {
        val folder = folderDao.getById(folderId) ?: return@launch
        val currentMode = SavedStopsViewMode.fromString(folder.viewMode)
        val allModes = SavedStopsViewMode.entries
        val nextMode = allModes[(currentMode.ordinal + 1) % allModes.size]
        folderDao.update(folder.copy(viewMode = nextMode.name))
    }


    fun isStopSavedFlow(stopId: String): Flow<Boolean> {
        return savedStopDao.isSaved(stopId)
    }

    fun toggleSavedStatus(stop: Stop) {
        managerScope.launch {
            _isLoading.value = true
            try {
                val stopId = stop.number.toString()
                val existingStop = savedStopDao.getStopById(stopId)

                if (existingStop != null) {
                    appDb.withTransaction {
                        val model = existingStop.toModel()
                        val folderIds = model.folderCategories ?: emptyList()

                        for (folderId in folderIds) {
                            val folder = folderDao.getById(folderId) ?: continue
                            val folderModel = folder.toModel()
                            val newOrder = folderModel.stopOrder.filter { it != stopId }
                            val updatedFolder = folder.copy(stopOrderJson = gson.toJson(newOrder))
                            folderDao.update(updatedFolder)
                        }

                        savedStopDao.deleteById(stopId)
                    }
                } else {
                    if (stop.variants.isEmpty()) {
                        stop.variants = variantsCacheManager.getCachedVariants(stop.number) ?: emptyList()
                    }
                    val newSavedStop = SavedStop(stopData = stop)
                    savedStopDao.insert(newSavedStop.toEntity())
                }
            } catch (e: Exception) {
                Log.e("SavedStopsManager", "Error toggling saved status: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAllData() {
        managerScope.launch {
            try {
                appDb.clearAllTables()
            } catch (e: Exception) {
                Log.e("SavedStopsManager", "Error clearing all data: ${e.message}", e)
            }
        }
    }
}