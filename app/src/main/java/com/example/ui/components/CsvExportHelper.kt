package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {
    fun createCsvFile(context: Context, expenses: List<Expense>): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvHeader = "ID,Tanggal,Kategori,Nominal(Rp),Deskripsi,Catatan,Status\n"
        val sb = StringBuilder(csvHeader)

        expenses.forEach { exp ->
            val dateStr = dateFormat.format(Date(exp.timestamp))
            val cleanTitle = exp.title.replace("\"", "\"\"")
            val cleanNote = exp.note.replace("\"", "\"\"")
            val syncStatus = if (exp.isSynced) "Tersinkron Cloud" else "Penyimpanan Lokal"
            sb.append("${exp.id},\"$dateStr\",\"${exp.category}\",${exp.amount.toLong()},\"$cleanTitle\",\"$cleanNote\",\"$syncStatus\"\n")
        }

        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "CatatKilat_Rekap_Pengeluaran.csv")
        file.writeText(sb.toString())
        return file
    }

    fun shareCsv(context: Context, expenses: List<Expense>) {
        if (expenses.isEmpty()) {
            Toast.makeText(context, "Tidak ada data pengeluaran untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = createCsvFile(context, expenses)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Rekap Pengeluaran CatatKilat")
                putExtra(Intent.EXTRA_TEXT, "Berikut adalah file CSV rekap pengeluaran harian dari aplikasi CatatKilat.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Rekap CSV"))
        } catch (e: Exception) {
            // Fallback: Copy content to clipboard
            copyCsvToClipboard(context, expenses)
        }
    }

    fun copyCsvToClipboard(context: Context, expenses: List<Expense>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder("ID,Tanggal,Kategori,Nominal(Rp),Deskripsi\n")
        expenses.forEach { exp ->
            sb.append("${exp.id},${dateFormat.format(Date(exp.timestamp))},${exp.category},${exp.amount.toLong()},${exp.title}\n")
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Rekap Pengeluaran CatatKilat", sb.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Rekap CSV berhasil disalin ke Clipboard!", Toast.LENGTH_LONG).show()
    }
}
