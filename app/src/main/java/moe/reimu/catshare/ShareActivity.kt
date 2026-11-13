package moe.reimu.catshare

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.ParcelUuid
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.reimu.catshare.models.DiscoveredDevice
import moe.reimu.catshare.models.FileInfo
import moe.reimu.catshare.models.TaskInfo
import moe.reimu.catshare.services.P2pSenderService
import moe.reimu.catshare.ui.ContentRow
import moe.reimu.catshare.ui.ExpressiveCard
import moe.reimu.catshare.ui.IconMedallion
import moe.reimu.catshare.ui.MetaPill
import moe.reimu.catshare.ui.ScreenContentPadding
import moe.reimu.catshare.ui.theme.CatShareTheme
import moe.reimu.catshare.utils.BleUtils
import moe.reimu.catshare.utils.DeviceUtils
import moe.reimu.catshare.utils.NotificationUtils
import moe.reimu.catshare.utils.ShizukuUtils
import moe.reimu.catshare.utils.TAG
import moe.reimu.catshare.utils.WifiUtils
import java.nio.ByteBuffer
import kotlin.random.Random

class ShareActivity : ComponentActivity() {
    private lateinit var bluetoothManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            NotificationUtils.showBluetoothToast(this)
            finish()
            return
        }

        val wifiManager = getSystemService(WifiManager::class.java)
        if (!wifiManager.isWifiEnabled) {
            NotificationUtils.showWifiToast(this)
            finish()
            return
        }

        val fileInfos = try {
            if (intent.action == Intent.ACTION_SEND) {
                @Suppress("DEPRECATION") val uri =
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) {
                    listOf(uri).mapNotNull { extractFileInfo(it) }
                } else {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    listOf(FileInfo(Uri.EMPTY, "", "", 0, text))
                }
            } else {
                @Suppress("DEPRECATION") val uris =
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                uris?.mapNotNull { extractFileInfo(it) } ?: emptyList()
            }
        } catch (e: Throwable) {
            Log.e("ShareActivity", "Failed to extract file info", e)
            Toast.makeText(this, R.string.no_file_shared, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (fileInfos.isEmpty()) {
            Toast.makeText(this, R.string.no_file_shared, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ShizukuUtils.bindService()

        enableEdgeToEdge()
        setContent {
            CatShareTheme {
                ShareActivityContent(files = fileInfos, onFinish = { finish() })
            }
        }
    }

    private fun extractFileInfo(uri: Uri): FileInfo? {
        val cr = contentResolver
        val proj = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE
        )
        return cr.query(uri, proj, null, null)?.use {
            if (it.moveToFirst()) {
                val mimeIndex = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                FileInfo(
                    uri,
                    it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)),
                    if (mimeIndex < 0) "application/octet-stream" else it.getString(mimeIndex),
                    it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                    null
                )
            } else {
                null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareActivityContent(
    files: List<FileInfo>,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val discoveredDevices = deviceScanner()
    val settings = remember { AppSettings(context) }
    val hardware5GHzSupported = remember { WifiUtils.is5GHzBandSupported(context) }
    val deviceForce5GhzStates = remember { mutableStateMapOf<String, Boolean>() }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    BackHandler { onFinish() }

    val formattedSize = remember(files) {
        Formatter.formatFileSize(context, files.sumOf { it.size })
    }

    fun startSending(device: DiscoveredDevice) {
        val task = TaskInfo(
            id = Random.nextInt(),
            device = device,
            files = files,
            force5Ghz = deviceForce5GhzStates[device.id] ?: settings.forceSend5Ghz,
        )
        if (!PendingShareStreams.prepare(context, task.id, files)) {
            Toast.makeText(context, R.string.no_file_shared, Toast.LENGTH_SHORT).show()
            return
        }
        if (P2pSenderService.startTaskChecked(context, task)) {
            onFinish()
        } else {
            PendingShareStreams.discard(task.id)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.choose_recipient)) },
                subtitle = {
                    Text(
                        if (files.size == 1 && files[0].textContent != null) {
                            stringResource(R.string.sharing_text)
                        } else {
                            pluralStringResource(
                                R.plurals.files_summary, files.size, files.size, formattedSize
                            )
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = ScreenContentPadding,
        ) {
            item { FilesCard(files = files) }

            item {
                Text(
                    text = stringResource(R.string.recipients_nearby),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                )
            }

            if (discoveredDevices.isEmpty()) {
                item { ScanningCard() }
            } else {
                items(discoveredDevices, key = { it.id }) { device ->
                    val force5Ghz = deviceForce5GhzStates.getOrPut(device.id) {
                        settings.forceSend5Ghz
                    }
                    DeviceCard(
                        device = device,
                        force5Ghz = force5Ghz,
                        showForce5Ghz = hardware5GHzSupported && !device.supports5Ghz,
                        onForce5GhzChange = { deviceForce5GhzStates[device.id] = it },
                        onClick = { startSending(device) },
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.start_sending_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FilesCard(files: List<FileInfo>) {
    val context = LocalContext.current
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            files.take(3).forEachIndexed { index, file ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                ContentRow(
                    title = file.name.ifEmpty { stringResource(R.string.text_message) },
                    supporting = if (file.textContent != null) {
                        stringResource(R.string.text_desc)
                    } else {
                        Formatter.formatFileSize(context, file.size)
                    },
                    leading = {
                        IconMedallion(
                            icon = ImageVector.vectorResource(R.drawable.ic_attach_file),
                            contentDescription = null,
                            size = 40,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    },
                )
            }
            if (files.size > 3) {
                Spacer(Modifier.height(12.dp))
                MetaPill(
                    text = stringResource(R.string.files_count_more, files.size - 3),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScanningCard() {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            ContainedLoadingIndicator(
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.scanning_desc),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.recipients_nearby_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredDevice,
    force5Ghz: Boolean,
    showForce5Ghz: Boolean,
    onForce5GhzChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            ContentRow(
                title = if (BuildConfig.DEBUG) {
                    "${device.name} (${device.id})"
                } else {
                    device.name
                },
                supporting = device.brand ?: stringResource(R.string.unknown),
                leading = {
                    IconMedallion(
                        icon = ImageVector.vectorResource(R.drawable.ic_smartphone),
                        contentDescription = null,
                        size = 48,
                    )
                },
                trailing = if (device.supports5Ghz) {
                    { MetaPill(text = "5GHz") }
                } else {
                    null
                },
                titleMarquee = true,
            )

            if (showForce5Ghz) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.use_5ghz_this_time),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.force_5ghz_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = force5Ghz, onCheckedChange = onForce5GhzChange)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun deviceScanner(): List<DiscoveredDevice> {
    val context = LocalContext.current
    var discoveredDevices by remember { mutableStateOf(emptyList<DiscoveredDevice>()) }
    val deviceLastSeen = remember { mutableMapOf<String, Long>() }
    val deviceTimeout = 2_000L

    LifecycleResumeEffect(context) {
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager.adapter
        val devicesLock = Object()

        val callback = object : ScanCallback() {
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error code: $errorCode")
            }

            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord ?: return
                var supports5Ghz = false
                var deviceName: String? = null
                var brandId: Byte? = null
                var senderId: String? = null

                for ((uuid, data) in record.serviceData.entries) {
                    when (data.size) {
                        6 -> {
                            val buf = ByteBuffer.allocate(16)
                            buf.putLong(uuid.uuid.mostSignificantBits)
                            buf.putLong(uuid.uuid.leastSignificantBits)
                            val arr = buf.array()
                            supports5Ghz = arr[2].toInt() == 1
                            brandId = arr[3]
                        }

                        27 -> {
                            val nameBuf = mutableListOf<Byte>()
                            for (i in 10..25) {
                                if (data[i].toInt() != 0) {
                                    nameBuf.add(data[i])
                                } else {
                                    break
                                }
                            }

                            val senderIdRaw = data[8].toInt().shl(8).or(data[9].toInt())
                            senderId = String.format("%04x", senderIdRaw)

                            var name = nameBuf.toByteArray().decodeToString()
                            if (name.last() == '\t') {
                                name = name.removeSuffix("\t") + "..."
                            }
                            deviceName = name
                        }
                    }
                }

                if (deviceName == null || senderId == null) {
                    return
                }

                val brand = brandId?.let { DeviceUtils.deviceNameById(it) }
                val newDevice = DiscoveredDevice(
                    result.device, senderId, deviceName, brand, supports5Ghz
                )

                var replaced = false
                synchronized(devicesLock) {
                    deviceLastSeen[senderId] = System.currentTimeMillis()
                    val newList = discoveredDevices.map {
                        if (it.id == senderId) {
                            replaced = true
                            newDevice
                        } else {
                            it
                        }
                    }.toMutableList()
                    if (!replaced) {
                        newList.add(newDevice)
                    }
                    discoveredDevices = newList
                }
            }
        }

        var startedScanner: BluetoothLeScanner? = null

        val cleanupJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(2000)
                val currentTime = System.currentTimeMillis()
                synchronized(devicesLock) {
                    val activeDevices = discoveredDevices.filter { device ->
                        val lastSeen = deviceLastSeen[device.id] ?: 0L
                        val isActive = (currentTime - lastSeen) < deviceTimeout
                        if (!isActive) {
                            Log.d(TAG, "Removing timed out device: ${device.name} (${device.id})")
                            deviceLastSeen.remove(device.id)
                        }
                        isActive
                    }
                    if (activeDevices.size != discoveredDevices.size) {
                        discoveredDevices = activeDevices
                    }
                }
            }
        }

        if (adapter != null) {
            val scanner = adapter.bluetoothLeScanner
            val filters = listOf(
                ScanFilter.Builder().setServiceUuid(ParcelUuid(BleUtils.ADV_SERVICE_UUID)).build()
            )
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            try {
                scanner?.startScan(filters, settings, callback)
                startedScanner = scanner
                Log.d(TAG, "Started scanning")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to start scan", e)
            }
        }

        onPauseOrDispose {
            cleanupJob.cancel()
            try {
                startedScanner?.stopScan(callback)
                Log.d(TAG, "Stopped scanning")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop scan", e)
            }
            synchronized(devicesLock) {
                deviceLastSeen.clear()
                discoveredDevices = emptyList()
            }
        }
    }

    return discoveredDevices
}