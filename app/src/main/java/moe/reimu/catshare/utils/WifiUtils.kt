package moe.reimu.catshare.utils

import android.content.Context
import android.net.wifi.WifiManager

object WifiUtils {
    fun is5GHzBandSupported(context: Context): Boolean {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return false
        return wifiManager.is5GHzBandSupported
    }
}