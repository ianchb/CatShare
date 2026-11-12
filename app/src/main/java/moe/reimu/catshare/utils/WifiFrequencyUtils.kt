package moe.reimu.catshare.utils

object WifiFrequencyUtils {
    fun is5GHzFrequency(frequencyMHz: Int?): Boolean {
        if (frequencyMHz == null) return false
        return frequencyMHz in 5150..5925
    }

    fun is24GHzFrequency(frequencyMHz: Int?): Boolean {
        if (frequencyMHz == null) return false
        return frequencyMHz in 2400..2500
    }

    fun getFrequencyBandDescription(frequencyMHz: Int?): String {
        return when {
            is5GHzFrequency(frequencyMHz) -> "5GHz"
            is24GHzFrequency(frequencyMHz) -> "2.4GHz"
            else -> "Unknown"
        }
    }
}