package com.wpcodify.otprelay

import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Posts a parsed OTP to submit.php. Runs on a background thread. */
object Uploader {

    fun submit(prefs: Prefs, otp: String, source: String, raw: String, smsTs: Long = System.currentTimeMillis()): Pair<Boolean, String> {
        val server = prefs.server
        val pair = prefs.pair
        val key = prefs.appKey
        if (server.isBlank() || pair.isBlank()) return false to "server/pair খালি"

        return try {
            val url = URL("$server/submit.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            val form = buildString {
                append("key=").append(enc(key))
                append("&pair=").append(enc(pair))
                append("&otp=").append(enc(otp))
                append("&source=").append(enc(source))
                append("&smsTs=").append(smsTs)
                append("&raw=").append(enc(raw.take(300)))
            }
            conn.outputStream.use { os: OutputStream -> os.write(form.toByteArray()) }
            val code = conn.responseCode
            val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: ""
            conn.disconnect()
            if (code in 200..299) true to "পাঠানো হয়েছে ($source $otp)"
            else false to "HTTP $code: ${resp.take(80)}"
        } catch (e: Exception) {
            false to ("ত্রুটি: " + (e.message ?: "unknown"))
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
