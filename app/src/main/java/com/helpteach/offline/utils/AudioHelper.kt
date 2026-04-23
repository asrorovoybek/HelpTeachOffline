package com.helpteach.offline.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object AudioHelper {
    /**
     * Foydalanuvchi tanlagan audio faylni (Uri) ilovaning xavfsiz papkasiga nusxalaydi.
     * @return nusxalangan faylning to'liq yuli (absolute path) yoki null
     */
    fun copyAudioToInternal(context: Context, uri: Uri): String? {
        try {
            val audioDir = File(context.filesDir, "custom_audio")
            if (!audioDir.exists()) audioDir.mkdirs()

            // Original fayl nomini olishga urinib ko'ramiz
            var originalName = "audio_${UUID.randomUUID().toString().take(8)}.mp3"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    originalName = cursor.getString(nameIndex)
                }
            }
            
            // Xavfsiz nom yaratish (bir xil nomlar ustma-ust tushmasligi uchun)
            val safeName = "${System.currentTimeMillis()}_${originalName.replace(" ", "_")}"
            val destinationFile = File(audioDir, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            return destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Berilgan fayl yo'li orqali faylni o'chiradi (Dars o'chirilganda ishlaydi).
     */
    fun deleteAudioFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Fayl nomini chiroyli ko'rsatish uchun (masalan: ".../123_qoshiq.mp3" -> "qoshiq.mp3")
     */
    fun extractFilename(path: String?): String {
        if (path.isNullOrBlank()) return ""
        return File(path).name.substringAfter("_", File(path).name)
    }
}
