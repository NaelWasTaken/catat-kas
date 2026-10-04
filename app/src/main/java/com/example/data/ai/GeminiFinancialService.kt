package com.example.data.ai

import com.example.BuildConfig
import com.example.data.local.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiFinancialService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeSpendingWithThinking(
        expenses: List<Expense>,
        monthlyBudget: Double,
        totalSpent: Double,
        currencyFormat: NumberFormat
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalFinancialInsight(expenses, monthlyBudget, totalSpent, currencyFormat)
        }

        val prompt = buildPrompt(expenses, monthlyBudget, totalSpent, currencyFormat)

        try {
            // Using gemini-3.1-pro-preview with thinkingLevel HIGH as required for complex reasoning
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"

            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    val thinkingConfig = JSONObject().apply {
                        put("thinkingLevel", "high")
                    }
                    put("thinkingConfig", thinkingConfig)
                }
                put("generationConfig", generationConfig)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) {
                            return@withContext text
                        }
                    }
                }
            }

            // Fallback to gemini-3.5-flash if 3.1-pro-preview endpoint had temporary capacity issue
            callFlashFallback(apiKey, prompt) ?: generateLocalFinancialInsight(expenses, monthlyBudget, totalSpent, currencyFormat)
        } catch (e: Exception) {
            generateLocalFinancialInsight(expenses, monthlyBudget, totalSpent, currencyFormat)
        }
    }

    private fun callFlashFallback(apiKey: String, prompt: String): String? {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) return text
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return null
    }

    private fun buildPrompt(
        expenses: List<Expense>,
        monthlyBudget: Double,
        totalSpent: Double,
        currencyFormat: NumberFormat
    ): String {
        val categoryBreakdown = expenses.groupBy { it.category }
            .map { (cat, list) -> "- $cat: ${currencyFormat.format(list.sumOf { it.amount })} (${list.size} kali transaksi)" }
            .joinToString("\n")

        val percentageUsed = if (monthlyBudget > 0) (totalSpent / monthlyBudget) * 100 else 0.0

        return """
            Bertindaklah sebagai Senior Financial Advisor & Planner cerdas.
            Analisis data pengeluaran user berikut:
            - Budget Bulanan: ${currencyFormat.format(monthlyBudget)}
            - Total Pengeluaran Saat Ini: ${currencyFormat.format(totalSpent)} (${String.format(Locale.US, "%.1f", percentageUsed)}% dari budget)
            
            Rincian Pengeluaran per Kategori:
            $categoryBreakdown
            
            Jumlah Total Transaksi: ${expenses.size}

            Berikan analisis mendalam, tajam, dan bersahabat dalam bahasa Indonesia:
            1. 💡 Evaluasi Kesehatan Budget: Apakah pengeluaran masih dalam batas aman atau berisiko over-budget?
            2. 🔍 Kategori yang Perlu Diwaspadai: Kategori mana yang paling banyak menyedot pengeluaran dan bagaimana cara memangkasnya.
            3. ⚡ Rekomendasi Shortcut Penghematan: 2-3 aksi praktis cepat hari ini.
            Gunakan format markdown yang rapi, ringkas, dan memotivasi!
        """.trimIndent()
    }

    private fun generateLocalFinancialInsight(
        expenses: List<Expense>,
        monthlyBudget: Double,
        totalSpent: Double,
        currencyFormat: NumberFormat
    ): String {
        val percentage = if (monthlyBudget > 0) (totalSpent / monthlyBudget) * 100 else 0.0
        val topCategory = expenses.groupBy { it.category }
            .maxByOrNull { it.value.sumOf { exp -> exp.amount } }

        val statusBadge = when {
            percentage > 100 -> "⚠️ OVER BUDGET: Pengeluaran melebihi batas bulanan sebesar ${String.format(Locale.US, "%.1f", percentage - 100)}%!"
            percentage > 80 -> "⚡ WASPADA: Sudah menggunakan ${String.format(Locale.US, "%.1f", percentage)}% dari kuota budget bulanan."
            percentage > 50 -> "✅ STABIL: Pengeluaran telah mencapai separuh budget (${String.format(Locale.US, "%.1f", percentage)}%)."
            else -> "🌟 SANGAT SEHAT: Baru terpakai ${String.format(Locale.US, "%.1f", percentage)}% dari target budget bulanan."
        }

        val topCategoryStr = if (topCategory != null) {
            "${topCategory.key} (${currencyFormat.format(topCategory.value.sumOf { it.amount })})"
        } else {
            "Belum ada data"
        }

        return """
            ### 💡 Ringkasan Analisis Finansial Cepat
            $statusBadge
            
            **Sorotan Utama:**
            - **Total Terpakai:** ${currencyFormat.format(totalSpent)} dari ${currencyFormat.format(monthlyBudget)}
            - **Pos Pengeluaran Terbesar:** $topCategoryStr
            - **Sisa Budget:** ${currencyFormat.format((monthlyBudget - totalSpent).coerceAtLeast(0.0))}
            
            **Saran Aksi Cepat:**
            1. Periksa pengeluaran rutin di kategori $topCategoryStr dan manfaatkan shortcut cepat untuk membatasi belanja impulsif.
            2. Pertahankan pencatatan segera setelah transaksi (kurang dari 10 detik dengan Quick Input) agar evaluasi harian akurat.
        """.trimIndent()
    }
}
