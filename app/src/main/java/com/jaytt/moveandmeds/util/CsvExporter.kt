package com.jaytt.moveandmeds.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.jaytt.moveandmeds.data.model.ReminderHistory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun exportHistory(context: Context, history: List<ReminderHistory>): Uri {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val csvFile = File(context.cacheDir, "export.csv")
        csvFile.bufferedWriter().use { writer ->
            writer.write("Date,Time,Type,Name,Status\n")
            for (record in history) {
                val date = dateFormat.format(Date(record.scheduledTime))
                val time = timeFormat.format(Date(record.scheduledTime))
                val type = record.itemType.replaceFirstChar { it.uppercaseChar() }
                val name = record.itemName.replace(",", ";")
                writer.write("$date,$time,$type,$name,${record.status}\n")
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )
    }
}
