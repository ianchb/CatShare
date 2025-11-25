package moe.reimu.catshare

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import moe.reimu.catshare.services.GattServerService
import moe.reimu.catshare.ui.DefaultCard
import moe.reimu.catshare.ui.theme.CatShareTheme
import moe.reimu.catshare.utils.ServiceState
import moe.reimu.catshare.utils.TAG
import moe.reimu.catshare.utils.registerInternalBroadcastReceiver
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        enableEdgeToEdge()
        setContent {
            CatShareTheme {
                MainActivityContent()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT <= 32 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= 31) {
            for (perm in listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )) {
                if (ContextCompat.checkSelfPermission(
                        this, perm
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(perm)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int
    ) {
        for ((name, status) in permissions.zip(grantResults.toList())) {
            if (status == PackageManager.PERMISSION_GRANTED) {
                continue
            }

            Toast.makeText(this, "$name not granted", Toast.LENGTH_LONG).show()
            finish()

            return
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityContent() {
    var checked by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ServiceState.ACTION_UPDATE_RECEIVER_STATE) {
                    checked = intent.getBooleanExtra("isRunning", false)
                }
            }
        }

        context.registerInternalBroadcastReceiver(
            receiver,
            IntentFilter(ServiceState.ACTION_UPDATE_RECEIVER_STATE),
        )
        context.sendBroadcast(ServiceState.getQueryIntent())

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val localMacAddressGranted = remember {
        context.checkSelfPermission("android.permission.LOCAL_MAC_ADDRESS") == PackageManager.PERMISSION_GRANTED
    }

    var shizukuGranted by remember {
        mutableStateOf(false)
    }

    var shizukuAvailable by remember {
        mutableStateOf(false)
    }

    DisposableEffect(Unit) {
        val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            Log.d(TAG, "Shizuku grant result: $grantResult")
            shizukuGranted = grantResult == PackageManager.PERMISSION_GRANTED
        }

        val binderRecvListener = Shizuku.OnBinderReceivedListener {
            shizukuAvailable = true
            shizukuGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }

        val binderDeadReceiver = Shizuku.OnBinderDeadListener {
            shizukuAvailable = false
        }

        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderRecvListener)
        Shizuku.addBinderDeadListener(binderDeadReceiver)

        onDispose {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderRecvListener)
            Shizuku.removeBinderDeadListener(binderDeadReceiver)
        }
    }

    val pickFilesLauncher = rememberLauncherForActivityResult(ChooseFilesContract()) { pickedUris ->
        if (pickedUris.isNotEmpty()) {
            val intent = Intent(context, ShareActivity::class.java)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(pickedUris))
            context.startActivity(intent)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            actions = {
                IconButton(onClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.title_activity_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp),
        ) {
            item {
                DefaultCard(onClick = {
                    pickFilesLauncher.launch()
                }) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.send),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.send_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                DefaultCard(onClick = {
                    if (checked) {
                        GattServerService.stop(context)
                    } else {
                        GattServerService.start(context)
                    }
                }) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_bluetooth_searching),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.discoverable),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.discoverable_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = {
                                if (it) {
                                    GattServerService.start(context)
                                } else {
                                    GattServerService.stop(context)
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (!localMacAddressGranted) {
                item {
                    DefaultCard(onClick = {
                        if (shizukuAvailable && !shizukuGranted) {
                            try {
                                Shizuku.requestPermission(0)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        } else if (!shizukuAvailable) {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    }) {
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconColor = when {
                                shizukuAvailable && shizukuGranted -> MaterialTheme.colorScheme.primary
                                shizukuAvailable -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }

                            Icon(
                                imageVector = if (shizukuAvailable && shizukuGranted) {
                                    ImageVector.vectorResource(R.drawable.ic_done)
                                } else {
                                    ImageVector.vectorResource(R.drawable.ic_close)
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(end = 20.dp),
                                tint = iconColor
                            )
                            Column {
                                Text(
                                    text = stringResource(
                                        if (shizukuAvailable) {
                                            if (shizukuGranted) R.string.shizuku_available
                                            else R.string.shizuku_not_granted
                                        } else {
                                            R.string.shizuku_unavailable
                                        }
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.shizuku_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


class ChooseFilesContract : ActivityResultContract<Void?, List<Uri>>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        val cf = Intent(Intent.ACTION_GET_CONTENT)
            .setType("*/*")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        return Intent.createChooser(cf, context.getString(R.string.choose_files))
    }

    override fun getSynchronousResult(
        context: Context,
        input: Void?
    ): SynchronousResult<List<Uri>>? =
        null

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (intent == null) {
            return emptyList()
        }

        val ret = mutableListOf<Uri>()

        val clipData = intent.clipData
        if (clipData != null) {
            for (i in 0..<clipData.itemCount) {
                clipData.getItemAt(i).uri?.let {
                    ret.add(it)
                }
            }
        } else {
            intent.data?.let {
                ret.add(it)
            }
        }

        return ret
    }
}