package com.wpcodify.otprelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/** Fires on every incoming SMS; forwards bKash/Rocket OTPs to the server. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = Prefs(context)
        if (!prefs.enabled) return

        // A long SMS arrives in parts; join them into one body per sender,
        // and keep the newest part's timestamp as the SMS time.
        val bySender = HashMap<String, StringBuilder>()
        val tsBySender = HashMap<String, Long>()
        for (msg in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            val from = msg.originatingAddress ?: "?"
            bySender.getOrPut(from) { StringBuilder() }.append(msg.messageBody ?: "")
            val t = try { msg.timestampMillis } catch (e: Throwable) { System.currentTimeMillis() }
            tsBySender[from] = maxOf(tsBySender[from] ?: 0L, t)
        }

        for ((sender, sb) in bySender) {
            val body = sb.toString()
            val hit = OtpParser.parse(body, sender) ?: continue
            val smsTs = tsBySender[sender] ?: System.currentTimeMillis()
            // Ignore an SMS that is already old when it reaches us (replayed/queued)
            if (System.currentTimeMillis() - smsTs > 150_000) {
                prefs.log(SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()) + " · পুরনো SMS বাদ")
                continue
            }
            val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            thread {
                val (ok, note) = Uploader.submit(prefs, hit.otp, hit.source, body, smsTs)
                prefs.log("$stamp ${if (ok) "✓" else "✗"} $note")
            }
        }
    }
}
