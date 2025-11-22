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

            in 60..69 -> {
                "Nubia"
            }

            in 70..75 -> {
                "Samsung"
            }

            in 80..89 -> {
                "ZTE"
            }

            in 90..95 -> {
                "Smartisan"
            }

            in 100..109 -> {
                "Lenovo"
            }

            in 110..119 -> {
                "Motorola"
            }

            in 120..129 -> {
                "NIO"
            }

            in 140..149 -> {
                "Honor"
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
            60 to "Nubia",
            70 to "Samsung",
            80 to "ZTE",
            90 to "Smartisan",
            100 to "Lenovo",
            110 to "Motorola",
            120 to "NIO",
            140 to "Honor"
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
            in 60..69 -> 60  // Nubia
            in 70..75 -> 70  // Samsung
            in 80..89 -> 80  // ZTE
            in 90..95 -> 90  // Smartisan
            in 100..109 -> 100  // Lenovo
            in 110..119 -> 110  // Motorola
            in 120..129 -> 120  // NIO
            in 140..149 -> 140  // Honor
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