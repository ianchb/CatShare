package moe.reimu.catshare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import moe.reimu.catshare.services.GattServerService
import moe.reimu.catshare.ui.IconMedallion
import moe.reimu.catshare.ui.InlineNotice
import moe.reimu.catshare.ui.NoticeSeverity
import moe.reimu.catshare.ui.ScreenContentPadding
import moe.reimu.catshare.ui.SectionHeader
import moe.reimu.catshare.ui.theme.CatShareTheme
import moe.reimu.catshare.utils.DeviceUtils
import moe.reimu.catshare.utils.ServiceState
import moe.reimu.catshare.utils.ShizukuUtils
import moe.reimu.catshare.utils.WifiUtils
import moe.reimu.catshare.utils.registerInternalBroadcastReceiver
import java.io.File

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatShareTheme {
                SettingsActivityContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsActivityContent() {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val hardware5GHzSupported = remember { WifiUtils.is5GHzBandSupported(context) }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var deviceNameValue by remember { mutableStateOf(settings.deviceName) }
    var verboseValue by remember { mutableStateOf(settings.verbose) }
    var autoAcceptValue by remember { mutableStateOf(settings.autoAccept) }
    var supports5GhzValue by remember { mutableStateOf(settings.supports5Ghz) }
    var brandIdValue by remember { mutableIntStateOf(settings.brandId) }
    var forceSend5GhzValue by remember { mutableStateOf(settings.forceSend5Ghz) }
    var macAddressOverrideValue by remember { mutableStateOf(settings.macAddressOverride) }
    val canAutoGetMac = remember { ShizukuUtils.canGetMacAddressAutomatically(context) }

    var showBrandSheet by remember { mutableStateOf(false) }
    var showCustomIdDialog by remember { mutableStateOf(false) }
    var showMacAddressDialog by remember { mutableStateOf(false) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(
        deviceNameValue, verboseValue, autoAcceptValue, supports5GhzValue,
        brandIdValue, forceSend5GhzValue, macAddressOverrideValue,
    ) {
        hasChanges = deviceNameValue != settings.deviceName ||
                verboseValue != settings.verbose ||
                autoAcceptValue != settings.autoAccept ||
                supports5GhzValue != settings.supports5Ghz ||
                brandIdValue != settings.brandId ||
                forceSend5GhzValue != settings.forceSend5Ghz ||
                macAddressOverrideValue != settings.macAddressOverride
    }

    val saveSettings: () -> Unit = {
        val nameValue = deviceNameValue
        if (nameValue.isNotBlank()) {
            settings.deviceName = nameValue
        }
        settings.macAddressOverride = macAddressOverrideValue
        settings.verbose = verboseValue
        settings.autoAccept = autoAcceptValue
        settings.supports5Ghz = supports5GhzValue
        settings.brandId = brandIdValue
        settings.forceSend5Ghz = forceSend5GhzValue

        var isServiceRunning = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ServiceState.ACTION_UPDATE_RECEIVER_STATE) {
                    isServiceRunning = intent.getBooleanExtra("isRunning", false)
                }
            }
        }

        context.registerInternalBroadcastReceiver(
            receiver,
            IntentFilter(ServiceState.ACTION_UPDATE_RECEIVER_STATE),
        )
        context.sendBroadcast(ServiceState.getQueryIntent())

        Handler(Looper.getMainLooper()).postDelayed({
            context.unregisterReceiver(receiver)
            if (isServiceRunning) {
                GattServerService.stop(context)
                Handler(Looper.getMainLooper()).postDelayed({
                    GattServerService.start(context)
                }, 500)
            }
        }, 100)

        Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        activity?.finish()
    }

    val handleBack: () -> Unit = {
        if (hasChanges) showExitDialog = true else activity?.finish()
    }

    BackHandler { handleBack() }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(ImageVector.vectorResource(R.drawable.ic_info), null) },
            title = { Text(stringResource(R.string.unsaved_changes)) },
            text = { Text(stringResource(R.string.unsaved_changes_notice)) },
            confirmButton = {
                TextButton(onClick = saveSettings) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text(stringResource(R.string.discard))
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.title_activity_settings)) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = hasChanges) {
                ExtendedFloatingActionButton(
                    onClick = saveSettings,
                    icon = {
                        Icon(ImageVector.vectorResource(R.drawable.ic_save), contentDescription = null)
                    },
                    text = { Text(stringResource(R.string.save_changes)) },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = rememberLazyListState(),
            contentPadding = ScreenContentPadding,
        ) {
            item { SectionHeader(stringResource(R.string.settings_general)) }

            item {
                SettingsList {
                    SettingsSegment(
                        index = 0,
                        count = 2,
                        icon = ImageVector.vectorResource(R.drawable.ic_smartphone),
                        title = stringResource(R.string.device_name),
                        supporting = deviceNameValue,
                        onClick = { showDeviceNameDialog = true },
                    )
                    SettingsSegment(
                        index = 1,
                        count = 2,
                        icon = ImageVector.vectorResource(R.drawable.ic_devices),
                        title = stringResource(R.string.device_brand),
                        supporting = buildBrandDisplayText(brandIdValue),
                        onClick = { showBrandSheet = true },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.settings_sending)) }

            item {
                SettingsList {
                    SettingsSegment(
                        index = 0,
                        count = 2,
                        icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                        title = stringResource(R.string.force_send_5ghz_name),
                        supporting = stringResource(R.string.force_send_5ghz_desc),
                        trailing = {
                            Switch(
                                checked = forceSend5GhzValue,
                                onCheckedChange = null,
                                enabled = hardware5GHzSupported,
                            )
                        },
                        onClick = {
                            if (hardware5GHzSupported) {
                                forceSend5GhzValue = !forceSend5GhzValue
                            }
                        },
                        enabled = hardware5GHzSupported,
                        notice = when {
                            !hardware5GHzSupported -> stringResource(R.string._5ghz_unsupported)
                            forceSend5GhzValue -> stringResource(R.string.force_send_5ghz_warning)
                            else -> null
                        },
                        noticeSeverity = if (!hardware5GHzSupported) {
                            NoticeSeverity.Error
                        } else {
                            NoticeSeverity.Warning
                        },
                    )
                    SettingsSegment(
                        index = 1,
                        count = 2,
                        icon = ImageVector.vectorResource(R.drawable.ic_sync),
                        title = stringResource(R.string.mac_address_override_name),
                        supporting = macAddressOverrideValue.ifEmpty {
                            stringResource(R.string.mac_address_override_not_set)
                        },
                        onClick = { showMacAddressDialog = true },
                        notice = when {
                            !canAutoGetMac -> stringResource(R.string.mac_address_override_hint)
                            macAddressOverrideValue.isNotEmpty() ->
                                stringResource(R.string.mac_address_override_warning)

                            else -> null
                        },
                        noticeSeverity = if (!canAutoGetMac) {
                            NoticeSeverity.Info
                        } else {
                            NoticeSeverity.Warning
                        },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.settings_receiving)) }

            item {
                SettingsList {
                    SettingsSegment(
                        index = 0,
                        count = 2,
                        icon = ImageVector.vectorResource(R.drawable.ic_done_all),
                        title = stringResource(R.string.auto_accept_name),
                        supporting = stringResource(R.string.auto_accept_desc),
                        trailing = {
                            Switch(checked = autoAcceptValue, onCheckedChange = null)
                        },
                        onClick = { autoAcceptValue = !autoAcceptValue },
                    )
                    SettingsSegment(
                        index = 1,
                        count = 2,
                        icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                        title = stringResource(R.string._5ghz_wi_fi),
                        supporting = stringResource(R.string._5ghz_wi_fi_desc),
                        trailing = {
                            Switch(
                                checked = supports5GhzValue,
                                onCheckedChange = null,
                                enabled = hardware5GHzSupported,
                            )
                        },
                        onClick = {
                            if (hardware5GHzSupported) supports5GhzValue = !supports5GhzValue
                        },
                        enabled = hardware5GHzSupported,
                        notice = when {
                            !hardware5GHzSupported -> stringResource(R.string._5ghz_unsupported)
                            !supports5GhzValue -> stringResource(R.string._5ghz_suggestion)
                            else -> null
                        },
                        noticeSeverity = if (!hardware5GHzSupported) {
                            NoticeSeverity.Error
                        } else {
                            NoticeSeverity.Info
                        },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.settings_developer)) }

            item {
                SettingsList {
                    SettingsSegment(
                        index = 0,
                        count = 2,
                        icon = ImageVector.vectorResource(R.drawable.ic_info),
                        title = stringResource(R.string.verbose_name),
                        supporting = stringResource(R.string.verbose_desc),
                        trailing = {
                            Switch(checked = verboseValue, onCheckedChange = null)
                        },
                        onClick = { verboseValue = !verboseValue },
                    )
                    SettingsSegment(
                        index = 1,
                        count = 2,
                        icon = ImageVector.vectorResource(R.drawable.ic_bug_report),
                        title = stringResource(R.string.capture_logs),
                        supporting = stringResource(R.string.capture_logs_desc),
                        onClick = { captureLogs(context) },
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.catshare_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.mutual_transmission_alliance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showBrandSheet) {
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
        BrandSelectionSheet(
            currentBrandId = brandIdValue,
            sheetState = sheetState,
            onDismiss = { showBrandSheet = false },
            onSelect = {
                brandIdValue = it
                showBrandSheet = false
            },
            onCustomId = {
                showBrandSheet = false
                showCustomIdDialog = true
            },
        )
    }

    if (showCustomIdDialog) {
        CustomBrandIdDialog(
            currentBrandId = brandIdValue,
            onDismiss = { showCustomIdDialog = false },
            onConfirm = {
                brandIdValue = it
                showCustomIdDialog = false
            },
        )
    }

    if (showDeviceNameDialog) {
        DeviceNameDialog(
            currentName = deviceNameValue,
            onDismiss = { showDeviceNameDialog = false },
            onConfirm = {
                deviceNameValue = it
                showDeviceNameDialog = false
            },
        )
    }

    if (showMacAddressDialog) {
        MacAddressDialog(
            currentMacAddress = macAddressOverrideValue,
            onDismiss = { showMacAddressDialog = false },
            onConfirm = {
                macAddressOverrideValue = it
                showMacAddressDialog = false
            },
        )
    }
}

private fun captureLogs(context: Context) {
    Thread {
        try {
            val logDir = File(context.cacheDir, "logs")
            logDir.mkdirs()
            val logFile = File(logDir, "logcat.txt")

            logFile.outputStream().use {
                val proc = Runtime.getRuntime().exec("logcat -d")
                try {
                    proc.inputStream.copyTo(it)
                } finally {
                    proc.destroy()
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileProvider",
                logFile,
            )
            val intent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .setType("text/plain")
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("LogcatCapture", "Failed to save logs", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, R.string.log_capture_failed, Toast.LENGTH_LONG).show()
            }
        }
    }.start()
}

/** Visually groups rows with the layered surface colour Expressive uses for settings lists. */
@Composable
private fun SettingsList(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) { content() }
}

/**
 * One row of a grouped settings list. Expressive lists read as a single surface whose top and
 * bottom rows carry the large corner radius; the middle rows stay square against their neighbours,
 * and the pressed row expands slightly against them.
 */
@Composable
private fun SettingsSegment(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    notice: String? = null,
    noticeSeverity: NoticeSeverity = NoticeSeverity.Info,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val mutedVariant = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Column(modifier = Modifier.fillMaxWidth()) {
        // SegmentedListItem owns both the container and its clip. Keeping the container opaque
        // lets its pressed shape animation and ripple use the same moving geometry.
        SegmentedListItem(
            onClick = onClick,
            shapes = shapes,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            leadingContent = {
                IconMedallion(
                    icon = icon,
                    contentDescription = null,
                    size = 40,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (enabled) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        mutedVariant
                    },
                )
            },
            trailingContent = trailing,
            supportingContent = supporting?.let { text -> { Text(text) } },
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else muted,
                supportingContentColor = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    mutedVariant
                },
            ),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }

        AnimatedVisibility(
            visible = notice != null,
            enter = expandVertically(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
            ) + fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            InlineNotice(
                text = notice.orEmpty(),
                severity = noticeSeverity,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrandSelectionSheet(
    currentBrandId: Int,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onCustomId: () -> Unit,
) {
    var selectedBrandId by remember { mutableIntStateOf(currentBrandId) }
    val brands = remember { DeviceUtils.getSupportedBrands() }
    val isCustom = brands.none { it.first == currentBrandId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = stringResource(R.string.select_device_brand),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.select_device_brand_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                contentPadding = ScreenContentPadding,
            ) {
                item {
                    ListItem(
                        onClick = onCustomId,
                        leadingContent = {
                            IconMedallion(
                                icon = ImageVector.vectorResource(R.drawable.ic_edit),
                                contentDescription = null,
                                size = 40,
                                containerColor = if (isCustom) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (isCustom) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                        supportingContent = {
                            Text(stringResource(R.string.current_id, currentBrandId))
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ) {
                        Text(stringResource(R.string.custom_brand_id))
                    }
                }
                items(brands) { (id, name) ->
                    ListItem(
                        selected = selectedBrandId == id,
                        onClick = { selectedBrandId = id },
                        leadingContent = {
                            RadioButton(selected = selectedBrandId == id, onClick = null)
                        },
                        supportingContent = if (id == 0) {
                            { Text(stringResource(R.string.no_brand_identification)) }
                        } else {
                            null
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) { Text(name) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = { onSelect(selectedBrandId) },
                    enabled = selectedBrandId != currentBrandId,
                ) { Text(stringResource(R.string.confirm)) }
            }
        }
    }
}

@Composable
private fun CustomBrandIdDialog(
    currentBrandId: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var inputValue by remember { mutableStateOf(currentBrandId.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    val errorInvalidNumber = stringResource(R.string.error_invalid_number)
    val errorIdRange = stringResource(R.string.error_id_range)
    val warningBrandRangeTemplate = stringResource(R.string.warning_brand_range)

    fun validate(input: String): Int? {
        errorMessage = null
        warningMessage = null
        val value = input.toIntOrNull()
        return when {
            value == null -> {
                errorMessage = errorInvalidNumber
                null
            }

            !DeviceUtils.isValidBrandId(value) -> {
                errorMessage = errorIdRange
                null
            }

            DeviceUtils.isKnownBrandId(value) -> {
                val suggestedId = DeviceUtils.getSuggestedBrandId(value)
                warningMessage = String.format(
                    warningBrandRangeTemplate,
                    value,
                    DeviceUtils.deviceNameById(suggestedId),
                    suggestedId,
                )
                value
            }

            else -> value
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(ImageVector.vectorResource(R.drawable.ic_edit), null) },
        title = { Text(stringResource(R.string.custom_id_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.custom_id_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = {
                        inputValue = it
                        validate(it)
                    },
                    label = { Text(stringResource(R.string.brand_id)) },
                    placeholder = { Text("0-255") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (warningMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = warningMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.custom_id_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validated = validate(inputValue)
                    if (validated != null && errorMessage == null) onConfirm(validated)
                },
                enabled = errorMessage == null && inputValue.isNotBlank(),
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun MacAddressDialog(
    currentMacAddress: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var inputValue by remember { mutableStateOf(currentMacAddress) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val errorInvalidFormat = stringResource(R.string.error_invalid_mac_format)

    fun validate(input: String): String? {
        errorMessage = null
        if (input.isEmpty()) return ""
        return if (ShizukuUtils.isValidMacAddress(input)) {
            input
        } else {
            errorMessage = errorInvalidFormat
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(ImageVector.vectorResource(R.drawable.ic_edit), null) },
        title = { Text(stringResource(R.string.mac_address_override_name)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.mac_address_override_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = {
                        inputValue = it
                        validate(it)
                    },
                    label = { Text(stringResource(R.string.mac_address_override_label)) },
                    placeholder = { Text("00:00:00:00:00:00") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val validated = validate(inputValue)
                            if (validated != null && errorMessage == null) onConfirm(validated)
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.mac_address_override_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validated = validate(inputValue)
                    if (validated != null && errorMessage == null) onConfirm(validated)
                },
                enabled = errorMessage == null,
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun buildBrandDisplayText(brandId: Int): String {
    val presetBrands = DeviceUtils.getSupportedBrands().map { it.first }
    val knownBrandName = DeviceUtils.deviceNameById(brandId)

    return when {
        brandId in presetBrands -> knownBrandName ?: ""
        knownBrandName != null -> "$knownBrandName ($brandId)"
        else -> "Custom ($brandId)"
    }
}

@Composable
private fun DeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var inputValue by remember { mutableStateOf(currentName) }
    val blank = inputValue.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(ImageVector.vectorResource(R.drawable.ic_smartphone), null) },
        title = { Text(stringResource(R.string.device_name)) },
        text = {
            Column {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text(stringResource(R.string.device_name)) },
                    supportingText = { Text(stringResource(R.string.device_name_desc)) },
                    singleLine = true,
                    isError = blank,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (!blank) onConfirm(inputValue.trim()) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(inputValue.trim()) }, enabled = !blank) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}