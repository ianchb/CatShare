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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import moe.reimu.catshare.models.DiscoveredDevice
import moe.reimu.catshare.models.FileInfo
import moe.reimu.catshare.models.TaskInfo
import moe.reimu.catshare.services.P2pSenderService
import moe.reimu.catshare.ui.DefaultCard
import moe.reimu.catshare.ui.theme.CatShareTheme
import moe.reimu.catshare.utils.BleUtils
import moe.reimu.catshare.utils.DeviceUtils
import moe.reimu.catshare.utils.NotificationUtils
import moe.reimu.catshare.utils.ShizukuUtils
import moe.reimu.catshare.utils.TAG
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
                @Suppress("DEPRECATION") val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) {
                    listOf(uri).mapNotNull { extractFileInfo(it) }
                } else {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    listOf(
                        FileInfo(
                            Uri.EMPTY, "", "", 0, text
                        )
                    )
                }
            } else {
                @Suppress("DEPRECATION") val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
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

        Log.i(TAG, "Shared ${fileInfos.size} files")

        ShizukuUtils.bindService()

        enableEdgeToEdge()
        setContent {
            CatShareTheme {
                ShareActivityContent(fileInfos)
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
                    if (mimeIndex < 0) {
                        "application/octet-stream"
                    } else {
                        it.getString(mimeIndex)
                    },
                    it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                    null
                )
            } else {
                null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareActivityContent(files: List<FileInfo>) {
    val context = LocalContext.current
    val discoveredDevices = deviceScanner()
    val listState = rememberLazyListState()

    BackHandler {
        (context as? ComponentActivity)?.finish()
    }

    val totalSize = remember(files) {
        files.sumOf { it.size }
    }

    val formattedSize = remember(totalSize) {
        Formatter.formatFileSize(context, totalSize)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.choose_recipient),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (files.size == 1 && files[0].textContent != null) {
                                stringResource(R.string.sharing_text)
                            } else {
                                pluralStringResource(R.plurals.files_summary, files.size, files.size, formattedSize)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
        ) {
            item {
                Text(
                    text = stringResource(R.string.files_to_send),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }

            item {
                DefaultCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        files.take(3).forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_bluetooth_searching),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 8.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name.ifEmpty { stringResource(R.string.text_message) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (file.textContent != null) {
                                            stringResource(R.string.text_desc)
                                        } else {
                                            Formatter.formatFileSize(context, file.size)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (files.size > 3) {
                            Text(
                                text = "And ${files.size - 3} more file${if (files.size - 3 > 1) "s" else ""}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, start = 32.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.available_devices),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    if (discoveredDevices.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.devices_found, discoveredDevices.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    DefaultCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )

                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(end = 20.dp)
                                    .rotate(rotation),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column {
                                Text(
                                    text = stringResource(R.string.scanning_desc),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.scanning_notice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(discoveredDevices, key = { it.id }) { device ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut() + slideOutVertically(
                            targetOffsetY = { -it / 4 }
                        )
                    ) {
                        DefaultCard(onClick = {
                            val task = TaskInfo(
                                id = Random.nextInt(),
                                device = device,
                                files = files
                            )
                            P2pSenderService.startTaskChecked(context, task)

                            (context as? ComponentActivity)?.finish()
                        }) {
                            Row(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(end = 20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (BuildConfig.DEBUG) {
                                            "${device.name} (${device.id}, ${device.device.address})"
                                        } else {
                                            device.name
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = device.brand ?: stringResource(R.string.unknown),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (device.supports5Ghz) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = MaterialTheme.shapes.small,
                                                modifier = Modifier.padding(0.dp)
                                            ) {
                                                Text(
                                                    text = "5GHz",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.start_sending_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            // UUID contains brand and 5GHz flag
                            val buf = ByteBuffer.allocate(16)
                            buf.putLong(uuid.uuid.mostSignificantBits)
                            buf.putLong(uuid.uuid.leastSignificantBits)
                            val arr = buf.array()
                            supports5Ghz = arr[2].toInt() == 1
                            brandId = arr[3]
                        }

                        27 -> {
                            // Data contains device name and ID
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

                val brand = brandId?.let {
                    DeviceUtils.deviceNameById(it)
                }

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

        val cleanupJob = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.Dispatchers.Main
        ).launch {
            while (true) {
                kotlinx.coroutines.delay(2000)

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
            val settings =
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

            try {
                scanner.startScan(filters, settings, callback)
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