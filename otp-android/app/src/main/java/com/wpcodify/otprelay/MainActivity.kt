package com.wpcodify.otprelay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var server: EditText
    private lateinit var pair: EditText
    private lateinit var appKey: EditText
    private lateinit var enabled: CheckBox
    private lateinit var logView: TextView

    private val needPerms = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        server = findViewById(R.id.server)
        pair = findViewById(R.id.pair)
        appKey = findViewById(R.id.appKey)
        enabled = findViewById(R.id.enabled)
        logView = findViewById(R.id.log)

        server.setText(prefs.server)
        pair.setText(prefs.pair)
        appKey.setText(prefs.appKey)
        enabled.isChecked = prefs.enabled

        findViewById<Button>(R.id.genPair).setOnClickListener {
            pair.setText(randomPair())
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.server = server.text.toString()
            prefs.pair = pair.text.toString()
            prefs.appKey = appKey.text.toString()
            prefs.enabled = enabled.isChecked
            if (enabled.isChecked) ensurePerms()
            Toast.makeText(this, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
            refreshLog()
        }

        findViewById<Button>(R.id.test).setOnClickListener {
            prefs.server = server.text.toString()
            prefs.pair = pair.text.toString()
            prefs.appKey = appKey.text.toString()
            val sample = "Your bKash OTP for PAYMENT of Tk.1,500.00 to IVAC (DGePay) is 123456."
            val hit = OtpParser.parse(sample, "bKash")!!
            thread {
                val (ok, note) = Uploader.submit(prefs, hit.otp, hit.source, sample, System.currentTimeMillis())
                prefs.log("TEST ${if (ok) "✓" else "✗"} $note")
                runOnUiThread { refreshLog() }
            }
        }

        findViewById<Button>(R.id.clearLog).setOnClickListener {
            getSharedPreferences("otp_relay", MODE_PRIVATE).edit().putString("log", "").apply()
            refreshLog()
        }

        ensurePerms()
        refreshLog()
    }

    override fun onResume() {
        super.onResume()
        refreshLog()
    }

    private fun refreshLog() {
        logView.text = prefs.logText.ifBlank { "এখনো কোনো OTP পাঠানো হয়নি।" }
    }

    private fun ensurePerms() {
        val missing = needPerms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        }
    }

    private fun randomPair(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
