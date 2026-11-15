package com.wpcodify.otprelay

import android.content.Context

/** Thin wrapper over SharedPreferences for the app's settings. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("otp_relay", Context.MODE_PRIVATE)

    var server: String
        get() = sp.getString("server", "https://sms2.wpcodify.com")!!
        set(v) = sp.edit().putString("server", v.trim().trimEnd('/')).apply()

    var pair: String
        get() = sp.getString("pair", "")!!
        set(v) = sp.edit().putString("pair", v.trim().uppercase()).apply()

    var appKey: String
        get() = sp.getString("appKey", "CHANGE-THIS-TO-A-LONG-RANDOM-STRING")!!
        set(v) = sp.edit().putString("appKey", v).apply()

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    fun log(line: String) {
        val old = sp.getString("log", "") ?: ""
        val trimmed = (line + "\n" + old).take(4000)
        sp.edit().putString("log", trimmed).apply()
    }

    val logText: String get() = sp.getString("log", "") ?: ""
}
