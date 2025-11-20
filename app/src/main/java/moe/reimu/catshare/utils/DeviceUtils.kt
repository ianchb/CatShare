package moe.reimu.catshare.utils

import java.util.Random

object DeviceUtils {
    private val alphabet =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()

    fun deviceNameById(id: Byte): String? {
        return deviceNameById(id.toInt())
    }
    fun deviceNameById(id: Int): String? {
        return when (id) {
            0 -> {
                "CatShare Default"
            }

            in 10..19 -> {
                if (id == 11) {
                    "realme"
                } else {
                    "OPPO"
                }
            }

            in 20..29 -> {
                "vivo"
            }

            in 30..39 -> {
                "Xiaomi"
            }

            in 41..45 -> {
                "OnePlus"
            }

            in 50..59 -> {
                "Meizu"
            }

            in 70..75 -> {
                "Samsung"
            }

            in 100..109 -> {
                "Lenovo"
            }

            else -> null
        }
    }

    fun getSupportedBrands(): List<Pair<Int, String>> {
        return listOf(
            0 to "Default",
            10 to "OPPO",
            11 to "realme",
            20 to "vivo",
            30 to "Xiaomi",
            41 to "OnePlus",
            50 to "Meizu",
            70 to "Samsung",
            100 to "Lenovo"
        )
    }

    fun isValidBrandId(id: Int): Boolean {
        return id in 0..255
    }

    fun isKnownBrandId(id: Int): Boolean {
        return deviceNameById(id) != null
    }

    fun getSuggestedBrandId(id: Int): Int {
        return when (id) {
            0 -> 0
            in 10..19 -> if (id == 11) 11 else 10  // realme and OPPO
            in 20..29 -> 20  // vivo
            in 30..39 -> 30  // Xiaomi
            in 41..45 -> 41  // OnePlus
            in 50..59 -> 50  // Meizu
            in 70..75 -> 70  // Samsung
            in 100..109 -> 100  // Lenovo
            else -> id
        }
    }

    fun getRandomChars(len: Int): String {
        val sb = StringBuilder()
        val rand = Random()
        repeat(len) {
            sb.append(alphabet[rand.nextInt(alphabet.size)])
        }
        return sb.toString()
    }
}