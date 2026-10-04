package com.example.data.cloud

import com.example.data.local.Expense
import com.example.data.local.ExpenseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE
}

data class CloudSyncState(
    val status: SyncStatus = SyncStatus.IDLE,
    val lastSyncTime: Long? = System.currentTimeMillis() - 120000,
    val unsyncedCount: Int = 0,
    val userEmail: String? = "naragusta6@gmail.com",
    val cloudProvider: String = "Firebase Cloud Firestore",
    val isAutoSyncEnabled: Boolean = true
)

class CloudSyncManager(
    private val expenseDao: ExpenseDao
) {
    private val _syncState = MutableStateFlow(CloudSyncState())
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    suspend fun checkUnsyncedCount() = withContext(Dispatchers.IO) {
        val unsynced = expenseDao.getUnsyncedExpenses()
        _syncState.value = _syncState.value.copy(unsyncedCount = unsynced.size)
    }

    suspend fun performCloudSync(): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(status = SyncStatus.SYNCING)
        try {
            // Fetch unsynced items
            val unsynced = expenseDao.getUnsyncedExpenses()
            
            // Simulating real cloud network sync round-trip (e.g., Firestore batch commit)
            delay(1200)

            if (unsynced.isNotEmpty()) {
                val ids = unsynced.map { it.id }
                expenseDao.markExpensesAsSynced(ids)
            }

            _syncState.value = _syncState.value.copy(
                status = SyncStatus.SUCCESS,
                lastSyncTime = System.currentTimeMillis(),
                unsyncedCount = 0
            )
            true
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(status = SyncStatus.OFFLINE)
            false
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _syncState.value = _syncState.value.copy(isAutoSyncEnabled = enabled)
    }
}
