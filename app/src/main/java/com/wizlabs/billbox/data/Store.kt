package com.wizlabs.billbox.data

import com.wizlabs.billbox.model.AppData
import java.io.File

/**
 * The phone's copy of the bill list, one JSON file in private storage. Google Drive holds the
 * master copy; this file lets the app open instantly and work without a connection.
 * Writes go to a temp file and are renamed, so a crash never leaves a half-written file.
 */
class Store(private val file: File) {

    fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            Codec.decode(file.readText())
        } catch (e: Exception) {
            file.copyTo(File(file.parentFile, "bills.unreadable-${System.currentTimeMillis()}.json"), overwrite = true)
            AppData()
        }
    }

    @Synchronized
    fun save(data: AppData) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(Codec.encode(data))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    fun clear() {
        file.delete()
    }
}
