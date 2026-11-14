package moe.reimu.catshare.utils

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import moe.reimu.catshare.AppSettings
import moe.reimu.catshare.BuildConfig
import moe.reimu.catshare.IMacAddressService
import moe.reimu.catshare.services.MacAddressService
import rikka.shizuku.Shizuku
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object ShizukuUtils {
    private val binderLock = Any()
    private val serviceLock = ReentrantLock()
    private val serviceNotify = serviceLock.newCondition()
    private var macService: IMacAddressService? = null

    init {
        Shizuku.addBinderReceivedListenerSticky {
            bindService()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder?) {
            if (service != null && service.pingBinder()) {
                Log.d(ShizukuUtils.TAG, "Got service connection for $name")

                synchronized(binderLock) {
                    macService = IMacAddressService.Stub.asInterface(service)
                }

                serviceLock.withLock { serviceNotify.signalAll() }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(ShizukuUtils.TAG, "Connection lost for $name")
            synchronized(binderLock) {
                macService = null
            }
        }
    }

    fun unsafeBindService() {
        val cn = ComponentName(
            BuildConfig.APPLICATION_ID, MacAddressService::class.java.name
        )
        val args = Shizuku.UserServiceArgs(cn)
            .daemon(false)
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
        Shizuku.bindUserService(args, serviceConnection)
    }

    fun bindService() {
        try {
            unsafeBindService()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to bind service", e)
        }
    }

    fun unsafeGetMacAddress(name: String): String? {
        synchronized(binderLock) {
            val svc = macService
            if (svc == null) {
                Log.d(TAG, "MAC service is null, trying to bind")
                unsafeBindService()
            } else {
                return svc.getMacAddressByName(name)
            }
        }

        serviceLock.withLock { serviceNotify.await(20, TimeUnit.SECONDS) }

        synchronized(binderLock) {
            return macService?.p2pMacAddress
        }
    }

    fun getMacAddress(context: Context, name: String, l: (String?) -> Unit) {
        val settings = AppSettings(context)
        val overrideMac = settings.macAddressOverride
        if (overrideMac.isNotEmpty() && isValidMacAddress(overrideMac)) {
            Log.d(TAG, "Using overridden MAC address: $overrideMac")
            l(overrideMac)
            return
        }

        if (context.checkSelfPermission("android.permission.LOCAL_MAC_ADDRESS") == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted, using native method")
            l(nativeGetMacAddressByName(name))
            return
        }

        val th = Thread {
            val res = try {
                unsafeGetMacAddress(name)
            } catch (e: Throwable) {
                Log.e(ShizukuUtils.TAG, "Failed to obtain MAC address for $name", e)
                null
            }

            l(res)
        }
        th.start()
    }

    private fun nativeGetMacAddressByName(name: String): String? {
        val ifs = NetworkInterface.getNetworkInterfaces()
        for (intf in ifs) {
            if (intf.name == name) {
                return intf.hardwareAddress?.toHexString(HexFormat {
                    bytes.byteSeparator = ":"
                })
            }
        }
        return null
    }

    suspend fun getMacAddress(context: Context, name: String): String? {
        val fut = CompletableDeferred<String?>()
        getMacAddress(context, name) {
            fut.complete(it)
        }
        return fut.await()
    }

    fun isValidMacAddress(mac: String): Boolean {
        val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
        return macPattern.matches(mac)
    }

    fun canGetMacAddressAutomatically(context: Context): Boolean {
        return context.checkSelfPermission("android.permission.LOCAL_MAC_ADDRESS") == PackageManager.PERMISSION_GRANTED ||
                (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
    }
}