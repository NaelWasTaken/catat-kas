package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiFinancialService
import com.example.data.cloud.CloudSyncManager
import com.example.data.cloud.CloudSyncState
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetConfig
import com.example.data.local.Expense
import com.example.data.local.QuickTemplate
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : UiEvent()
    data class QuickTemplateTriggered(val template: QuickTemplate) : UiEvent()
}

data class DashboardUiState(
    val monthlyBudget: Double = 3500000.0,
    val totalSpentThisMonth: Double = 0.0,
    val monthlyExpenses: List<Expense> = emptyList(),
    val allExpenses: List<Expense> = emptyList(),
    val quickTemplates: List<QuickTemplate> = emptyList(),
    val budgetConfig: BudgetConfig = BudgetConfig(),
    val cloudSyncState: CloudSyncState = CloudSyncState(),
    val isAiLoading: Boolean = false,
    val aiAnalysisResult: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val syncManager = CloudSyncManager(database.expenseDao())
    val repository = ExpenseRepository(database, syncManager)
    private val geminiService = GeminiFinancialService()

    val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiAnalysisResult = MutableStateFlow("")
    val aiAnalysisResult: StateFlow<String> = _aiAnalysisResult.asStateFlow()

    private var lastDeletedExpense: Expense? = null

    // Combined Dashboard state
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allExpenses,
        repository.allTemplates,
        repository.budgetConfig,
        syncManager.syncState
    ) { allExpenses: List<Expense>, templates: List<QuickTemplate>, budgetConfig: BudgetConfig?, syncState: CloudSyncState ->
        val range = repository.getCurrentMonthRange()
        val monthlyList = allExpenses.filter { it.timestamp in range.first..range.second }
        val totalSpent = monthlyList.sumOf { it.amount }
        val config = budgetConfig ?: BudgetConfig()

        DashboardUiState(
            monthlyBudget = config.monthlyBudget,
            totalSpentThisMonth = totalSpent,
            monthlyExpenses = monthlyList,
            allExpenses = allExpenses,
            quickTemplates = templates,
            budgetConfig = config,
            cloudSyncState = syncState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val id = repository.insertExpense(title, amount, category, note, timestamp)
            _eventFlow.emit(
                UiEvent.ShowSnackbar(
                    message = "Tercatat: ${title} (${currencyFormat.format(amount)})",
                    actionLabel = "Urungkan",
                    onAction = {
                        viewModelScope.launch {
                            repository.deleteExpenseById(id)
                        }
                    }
                )
            )
            // Trigger auto cloud sync if enabled
            if (syncManager.syncState.value.isAutoSyncEnabled) {
                syncManager.performCloudSync()
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            lastDeletedExpense = expense
            repository.deleteExpense(expense)
            _eventFlow.emit(
                UiEvent.ShowSnackbar(
                    message = "Pengeluaran dihapus",
                    actionLabel = "Urungkan",
                    onAction = {
                        viewModelScope.launch {
                            lastDeletedExpense?.let {
                                repository.insertExpense(it.title, it.amount, it.category, it.note, it.timestamp)
                            }
                        }
                    }
                )
            )
        }
    }

    fun quickLogTemplate(template: QuickTemplate) {
        addExpense(
            title = template.name,
            amount = template.defaultAmount,
            category = template.category,
            note = "Dicatat via Quick Shortcut"
        )
    }

    fun addOrUpdateTemplate(template: QuickTemplate) {
        viewModelScope.launch {
            if (template.id == 0L) {
                repository.insertTemplate(template)
            } else {
                repository.updateTemplate(template)
            }
        }
    }

    fun deleteTemplate(template: QuickTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun updateBudget(newBudget: Double, reminderHour: Int = 20, reminderMinute: Int = 0) {
        viewModelScope.launch {
            val current = uiState.value.budgetConfig
            repository.saveBudgetConfig(
                current.copy(
                    monthlyBudget = newBudget,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute
                )
            )
        }
    }

    fun requestAiInsight() {
        viewModelScope.launch {
            _isAiLoading.value = true
            val currentExpenses = uiState.value.monthlyExpenses
            val budget = uiState.value.monthlyBudget
            val spent = uiState.value.totalSpentThisMonth

            val insight = geminiService.analyzeSpendingWithThinking(
                expenses = currentExpenses,
                monthlyBudget = budget,
                totalSpent = spent,
                currencyFormat = currencyFormat
            )
            _aiAnalysisResult.value = insight
            _isAiLoading.value = false
        }
    }

    fun performManualSync() {
        viewModelScope.launch {
            val success = syncManager.performCloudSync()
            _eventFlow.emit(
                UiEvent.ShowSnackbar(
                    message = if (success) "Sinkronisasi Cloud Berhasil! ☁️" else "Gagal menyinkronkan (Offline Mode)"
                )
            )
        }
    }

    fun handleShortcutIntent(title: String?, amountStr: String?, category: String?) {
        if (title.isNullOrBlank()) return
        val amount = amountStr?.toDoubleOrNull() ?: 25000.0
        val cat = category ?: "Lainnya"

        addExpense(
            title = title,
            amount = amount,
            category = cat,
            note = "OS Shortcut Action"
        )
    }
}
