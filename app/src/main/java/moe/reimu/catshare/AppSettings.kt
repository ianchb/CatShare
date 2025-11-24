package moe.reimu.catshare

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import moe.reimu.catshare.utils.WifiUtils

class AppSettings(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    var deviceName: String
        get() = prefs.getString(
            "deviceName",
            context.getString(R.string.device_name_default_value)
        )!!
        set(value) {
            prefs.edit { putString("deviceName", value) }
        }

    var verbose: Boolean
        get() = prefs.getBoolean("verbose", false)
        set(value) {
            prefs.edit { putBoolean("verbose", value) }
        }

    var autoAccept: Boolean
        get() = prefs.getBoolean("autoAccept", false)
        set(value) {
            prefs.edit { putBoolean("autoAccept", value) }
        }

    var supports5Ghz: Boolean
        get() {
            if (!prefs.contains("supports5Ghz")) {
                val hardwareSupport = WifiUtils.is5GHzBandSupported(context)
                prefs.edit { putBoolean("supports5Ghz", hardwareSupport) }
                return hardwareSupport
            }
            return prefs.getBoolean("supports5Ghz", false)
        }
        set(value) {
            prefs.edit { putBoolean("supports5Ghz", value) }
        }

    var brandId: Int
        get() = prefs.getInt("brandId", 0)
        set(value) {
            prefs.edit { putInt("brandId", value) }
        }

    var forceSend5Ghz: Boolean
        get() = prefs.getBoolean("forceSend5Ghz", false)
        set(value) {
            prefs.edit { putBoolean("forceSend5Ghz", value) }
        }
}