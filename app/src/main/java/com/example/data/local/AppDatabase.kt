package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Expense::class, QuickTemplate::class, BudgetConfig::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun quickTemplateDao(): QuickTemplateDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "catatkilat_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val templateDao = database.quickTemplateDao()
                val budgetDao = database.budgetDao()
                val expenseDao = database.expenseDao()

                // Default Quick Templates as requested by user
                val initialTemplates = listOf(
                    QuickTemplate(name = "Makan Siang", category = "Makanan", defaultAmount = 25000.0, iconKey = "food", orderIndex = 0),
                    QuickTemplate(name = "Bensin", category = "Transportasi", defaultAmount = 30000.0, iconKey = "fuel", orderIndex = 1),
                    QuickTemplate(name = "Nongkrong di Kafe", category = "Hiburan", defaultAmount = 35000.0, iconKey = "coffee", orderIndex = 2),
                    QuickTemplate(name = "Parkir", category = "Transportasi", defaultAmount = 5000.0, iconKey = "parking", orderIndex = 3),
                    QuickTemplate(name = "Belanja Mart", category = "Belanja", defaultAmount = 50000.0, iconKey = "groceries", orderIndex = 4),
                    QuickTemplate(name = "Token Listrik", category = "Tagihan", defaultAmount = 100000.0, iconKey = "electric", orderIndex = 5)
                )
                templateDao.insertTemplates(initialTemplates)

                // Default Budget Config: 3.5 Million IDR, 80% alert threshold, 20:00 reminder
                budgetDao.saveBudgetConfig(BudgetConfig())

                // A few starter sample expenses for the current month so charts are visually rich immediately
                val now = System.currentTimeMillis()
                val dayMillis = 24L * 60 * 60 * 1000
                val initialExpenses = listOf(
                    Expense(title = "Makan Siang Nasi Padang", amount = 28000.0, category = "Makanan", timestamp = now - dayMillis * 0, isSynced = true),
                    Expense(title = "Bensin Pertamax", amount = 35000.0, category = "Transportasi", timestamp = now - dayMillis * 1, isSynced = true),
                    Expense(title = "Kopi & Croissant", amount = 42000.0, category = "Hiburan", timestamp = now - dayMillis * 1, isSynced = true),
                    Expense(title = "Supermarket Mingguan", amount = 165000.0, category = "Belanja", timestamp = now - dayMillis * 2, isSynced = true),
                    Expense(title = "Paket Data Internet", amount = 85000.0, category = "Tagihan", timestamp = now - dayMillis * 3, isSynced = true),
                    Expense(title = "Makan Malam Bersama", amount = 55000.0, category = "Makanan", timestamp = now - dayMillis * 4, isSynced = true)
                )
                expenseDao.insertExpenses(initialExpenses)
            }
        }
    }
}
