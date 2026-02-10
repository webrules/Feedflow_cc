package com.feedflow.data.repository

import com.feedflow.data.local.db.dao.SettingsDao
import com.feedflow.data.local.db.entity.SettingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao
) {
    fun getSettingFlow(key: String): Flow<String?> {
        return settingsDao.getSettingFlow(key)
    }

    suspend fun getSetting(key: String): String? {
        return settingsDao.getSetting(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        settingsDao.saveSetting(SettingEntity(key = key, value = value))
    }

    suspend fun deleteSetting(key: String) {
        settingsDao.deleteSetting(key)
    }

    suspend fun deleteAll() {
        settingsDao.deleteAll()
    }
}
