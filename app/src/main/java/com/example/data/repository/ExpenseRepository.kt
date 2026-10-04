package com.example.data.repository

import android.content.Context
import com.example.data.cloud.CloudSyncManager
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetConfig
import com.example.data.local.Expense
import com.example.data.local.QuickTemplate
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseRepository(
    private val database: AppDatabase,
    val cloudSyncManager: CloudSyncManager
) {
    private val expenseDao = database.expenseDao()
    private val quickTemplateDao = database.quickTemplateDao()
    private val budgetDao = database.budgetDao()

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allTemplates: Flow<List<QuickTemplate>> = quickTemplateDao.getAllTemplates()
    val budgetConfig: Flow<BudgetConfig?> = budgetDao.getBudgetConfig()

    suspend fun insertExpense(
        title: String,
        amount: Double,
        category: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val expense = Expense(
            title = title.trim(),
            amount = amount,
            category = category,
            note = note.trim(),
            timestamp = timestamp,
            isSynced = false
        )
        val id = expenseDao.insertExpense(expense)
        cloudSyncManager.checkUnsyncedCount()
        return id
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense.copy(isSynced = false))
        cloudSyncManager.checkUnsyncedCount()
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
        cloudSyncManager.checkUnsyncedCount()
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteById(id)
        cloudSyncManager.checkUnsyncedCount()
    }

    // Quick Templates Management
    suspend fun insertTemplate(template: QuickTemplate): Long {
        return quickTemplateDao.insertTemplate(template)
    }

    suspend fun updateTemplate(template: QuickTemplate) {
        quickTemplateDao.updateTemplate(template)
    }

    suspend fun deleteTemplate(template: QuickTemplate) {
        quickTemplateDao.deleteTemplate(template)
    }

    // Budget Management
    suspend fun saveBudgetConfig(config: BudgetConfig) {
        budgetDao.saveBudgetConfig(config)
    }

    // CSV Generation
    fun generateCsvContent(expenses: List<Expense>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,Tanggal,Kategori,Nominal,Deskripsi,Catatan,Status Sinkron\n")
        expenses.forEach { exp ->
            val dateStr = dateFormat.format(Date(exp.timestamp))
            val safeTitle = exp.title.replace("\"", "\"\"")
            val safeNote = exp.note.replace("\"", "\"\"")
            val syncText = if (exp.isSynced) "Tersinkron" else "Lokal"
            sb.append("${exp.id},\"$dateStr\",\"${exp.category}\",${exp.amount.toLong()},\"$safeTitle\",\"$safeNote\",$syncText\n")
        }
        return sb.toString()
    }

    fun exportCsvToFile(context: Context, expenses: List<Expense>): File {
        val content = generateCsvContent(expenses)
        val file = File(context.cacheDir, "CatatKilat_Laporan_Pengeluaran.csv")
        file.writeText(content)
        return file
    }

    // Dynamic Engaging Reminder Messages
    val dynamicReminderMessages = listOf(
        "Kopi senja tadi udah dicatat belum? ☕ Cuma butuh 5 detik!",
        "Jangan sampai dompet kaget di akhir bulan! Catat pengeluaran hari ini ⚡",
        "Rutinitas orang bijak: sebelum tidur, tutup hari dengan catat pengeluaran 🌙",
        "Ada struk belanjaan nyelip di saku celana? Yuk rekap kilat sekarang 📝",
        "Tahu ke mana uang pergi = langkah awal bebas finansial. Catat kilat! 🚀",
        "Beli bensin atau jajan sore tadi? Satu tap di CatatKilat beres! ⛽"
    )

    fun getRandomReminderMessage(): String {
        return dynamicReminderMessages.random()
    }

    fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis

        return Pair(startOfMonth, endOfMonth)
    }
}
