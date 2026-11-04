package com.lockdown.android

import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONException
import org.json.JSONObject

internal val approvedActions = mapOf(
    "official_account_recovery" to "Open the account provider’s official recovery page.",
    "review_account_sessions" to "Review active sessions in the provider’s security settings.",
    "verify_suspicious_message" to "Verify the message through an official channel.",
    "contact_bank" to "Contact your bank through its official app or the number on your card.",
    "lock_lost_device" to "Use the provider’s official device-finding service to lock the device.",
)

internal data class Assessment(val risk: String, val actions: List<String>)

internal fun requestAssessment(description: String): Assessment? {
    return try {
        val url = URL(BuildConfig.ASSESSMENT_URL)
        if (url.protocol != "https" || url.host.isBlank() || url.userInfo != null) return null
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject().put("description", description).put("consent", true)
            connection.outputStream.use { it.write(payload.toString().encodeToByteArray()) }
            if (connection.responseCode != 200) return null
            val bytes = connection.inputStream.use { input ->
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(1024)
                while (buffer.size() <= 8192) {
                    val count = input.read(chunk)
                    if (count < 0) break
                    buffer.write(chunk, 0, count)
                }
                buffer.toByteArray()
            }
            if (bytes.size > 8192) return null
            val result = JSONObject(bytes.decodeToString())
            val risk = result.optString("risk")
            if (result.optString("mode") != "meta" || risk !in setOf("low", "medium", "high")) return null
            val ids = result.getJSONArray("action_ids")
            if (ids.length() !in 1..3) return null
            val actions = (0 until ids.length()).map { approvedActions[ids.getString(it)] ?: return null }
            if (actions.distinct().size != actions.size) return null
            Assessment(risk, actions)
        } finally {
            connection.disconnect()
        }
    } catch (_: IOException) {
        null
    } catch (_: JSONException) {
        null
    }
}
