package com.helpteach.offline.network

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiTTS {
    private const val API_KEY = "AIzaSyCRjzopXGS0PVMpJK7rpHxp-GtIXb6KuJc"
    private const val MODEL = "gemini-2.5-flash-preview-tts"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * Matnni ovozga aylantiradi va faylga saqlaydi.
     * @return true agar muvaffaqiyatli bo'lsa
     */
    fun generateAndSave(context: Context, text: String, filename: String): Boolean {
        try {
            val url = URL("$BASE_URL/$MODEL:generateContent?key=$API_KEY")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", text)
                    ))
                ))
                put("generationConfig", JSONObject().apply {
                    put("response_modalities", JSONArray().put("AUDIO"))
                    put("speech_config", JSONObject().apply {
                        put("voice_config", JSONObject().apply {
                            put("prebuilt_voice_config", JSONObject().apply {
                                put("voice_name", "Kore")
                            })
                        })
                    })
                })
            }

            // So'rov yuborish
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(requestBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return false
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            connection.disconnect()

            // Audio ma'lumotni ajratib olish
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates") ?: return false
            val firstCandidate = candidates.optJSONObject(0) ?: return false
            val content = firstCandidate.optJSONObject("content") ?: return false
            val parts = content.optJSONArray("parts") ?: return false
            val firstPart = parts.optJSONObject(0) ?: return false
            val inlineData = firstPart.optJSONObject("inlineData") ?: return false
            val audioBase64 = inlineData.optString("data", "") 
            
            if (audioBase64.isEmpty()) return false

            // Base64 dan audio ma'lumotni dekod qilish
            val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)

            // Faylga saqlash
            val ttsDir = File(context.filesDir, "tts_audio")
            if (!ttsDir.exists()) ttsDir.mkdirs()

            val audioFile = File(ttsDir, filename)
            audioFile.writeBytes(audioBytes)

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Saqlangan audio faylni olish
     */
    fun getAudioFile(context: Context, filename: String): File? {
        val file = File(File(context.filesDir, "tts_audio"), filename)
        return if (file.exists()) file else null
    }

    /**
     * Dars uchun ovozli matn tuzish
     */
    fun buildLessonText(subject: String, lessonType: String, startTime: String, room: String): String {
        val typeUz = when (lessonType.lowercase()) {
            "lecture" -> "ma'ruza"
            "practical" -> "amaliyot"
            "lab" -> "laboratoriya"
            "seminar" -> "seminar"
            "course" -> "kurs ishi"
            else -> ""
        }
        return "Hurmatli foydalanuvchi, sizning $subject fanidan $typeUz mashg'ulotingiz soat $startTime da $room xonada boshlanmoqda."
    }

    /**
     * Dars uchun audio fayl nomini generatsiya qilish
     */
    fun lessonAudioFilename(dayOfWeek: Int, startTime: String, subject: String): String {
        val safeSubject = subject.replace(Regex("[^a-zA-Z0-9]"), "_").take(20)
        val safeTime = startTime.replace(":", "")
        return "lesson_${dayOfWeek}_${safeTime}_${safeSubject}.wav"
    }
}
