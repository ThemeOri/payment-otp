package com.wpcodify.otprelay

/** Pulls the OTP out of a bKash or Rocket payment SMS. */
object OtpParser {

    data class Result(val otp: String, val source: String)

    // bKash: "... Your bKash OTP for PAYMENT of Tk.1,500.00 to IVAC (DGePay) is 117747. Expires in 2 min."
    private val bkash = Regex("""bkash\s*otp.*?\bis\b\s*[:#]?\s*(\d{4,8})""", RegexOption.IGNORE_CASE)

    // Rocket: "552914 is your OTP for e-com transaction of Rocket no# 013****4103 ..."
    private val rocketLeading = Regex("""(\d{4,8})\s+is your otp.*?rocket""", RegexOption.IGNORE_CASE)
    private val rocketAny = Regex("""rocket.*?\botp\b[^0-9]{0,15}(\d{4,8})""", RegexOption.IGNORE_CASE)

    // A safe fallback: any 4-8 digit group when the text clearly names the wallet
    private val anyDigits = Regex("""\b(\d{4,8})\b""")

    fun parse(bodyRaw: String?, sender: String?): Result? {
        val body = bodyRaw ?: return null
        val low = body.lowercase()

        bkash.find(body)?.let { return Result(it.groupValues[1], "bkash") }

        if (low.contains("rocket")) {
            rocketLeading.find(body)?.let { return Result(it.groupValues[1], "rocket") }
            rocketAny.find(body)?.let { return Result(it.groupValues[1], "rocket") }
            if (low.contains("otp")) {
                anyDigits.find(body)?.let { return Result(it.groupValues[1], "rocket") }
            }
        }

        if (low.contains("bkash") && low.contains("otp")) {
            anyDigits.find(body)?.let { return Result(it.groupValues[1], "bkash") }
        }
        return null
    }
}
