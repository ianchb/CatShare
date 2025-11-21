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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import moe.reimu.catshare.services.GattServerService
import moe.reimu.catshare.ui.DefaultCard
import moe.reimu.catshare.ui.theme.CatShareTheme
import moe.reimu.catshare.utils.DeviceUtils
import moe.reimu.catshare.utils.ServiceState
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
    val settings = remember(activity) { AppSettings(context) }

    var deviceNameValue by remember {
        mutableStateOf(settings.deviceName)
    }

    var verboseValue by remember {
        mutableStateOf(settings.verbose)
    }

    var autoAcceptValue by remember {
        mutableStateOf(settings.autoAccept)
    }

    var supports5GhzValue by remember { mutableStateOf(settings.supports5Ghz) }
    var brandIdValue by remember { mutableIntStateOf(settings.brandId) }
    var showBrandDialog by remember { mutableStateOf(false) }
    var showCustomIdDialog by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deviceNameValue, verboseValue, autoAcceptValue, supports5GhzValue, brandIdValue) {
        hasChanges = deviceNameValue != settings.deviceName ||
                verboseValue != settings.verbose ||
                autoAcceptValue != settings.autoAccept ||
                supports5GhzValue != settings.supports5Ghz ||
                brandIdValue != settings.brandId
    }

    val saveSettings: () -> Unit = {
        val nameValue = deviceNameValue
        if (nameValue.isNotBlank()) {
            settings.deviceName = nameValue
        }
        settings.verbose = verboseValue
        settings.autoAccept = autoAcceptValue
        settings.supports5Ghz = supports5GhzValue
        settings.brandId = brandIdValue

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
            IntentFilter(ServiceState.ACTION_UPDATE_RECEIVER_STATE)
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

        Toast.makeText(
            context,
            context.getString(R.string.settings_saved),
            Toast.LENGTH_SHORT
        ).show()

        activity?.finish()
    }

    val handleBack: () -> Unit = {
        if (hasChanges) {
            showExitDialog = true
        } else {
            activity?.finish()
        }
    }

    BackHandler {
        handleBack()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.unsaved_changes),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.unsaved_changes_notice),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveSettings()
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        activity?.finish()
                    }
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_activity_settings),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = hasChanges,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 }
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it / 2 }
                ) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = saveSettings,
                    modifier = Modifier
                        .padding(bottom = 24.dp, end = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 16.dp,
                        hoveredElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_save),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.save_changes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                horizontal = 16.dp
            ),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_general),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                DefaultCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = deviceNameValue,
                            onValueChange = { deviceNameValue = it },
                            label = { Text(stringResource(R.string.device_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            )
                        )

                        Text(
                            text = stringResource(R.string.device_name_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                DefaultCard(
                    onClick = { showBrandDialog = true },
                    modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_device_information),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.device_brand),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayText = buildBrandDisplayText(brandIdValue)
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (showBrandDialog) {
                    BrandSelectionDialog(
                        currentBrandId = brandIdValue,
                        onDismiss = { showBrandDialog = false },
                        onSelect = { newBrandId ->
                            brandIdValue = newBrandId
                            showBrandDialog = false
                        },
                        onCustomId = {
                            showBrandDialog = false
                            showCustomIdDialog = true
                        }
                    )
                }
                if (showCustomIdDialog) {
                    CustomBrandIdDialog(
                        currentBrandId = brandIdValue,
                        onDismiss = { showCustomIdDialog = false },
                        onConfirm = { newBrandId ->
                            brandIdValue = newBrandId
                            showCustomIdDialog = false
                        }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_receiving),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsSwitchCard(
                    title = stringResource(R.string.auto_accept_name),
                    description = stringResource(R.string.auto_accept_desc),
                    checked = autoAcceptValue,
                    onCheckedChange = { autoAcceptValue = it },
                    icon = ImageVector.vectorResource(R.drawable.ic_done_all)
                )
                SettingsSwitchCard(
                    title = stringResource(R.string._5ghz_wi_fi),
                    description = stringResource(R.string._5ghz_wi_fi_desc),
                    checked = supports5GhzValue,
                    onCheckedChange = { supports5GhzValue = it },
                    icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_developer),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsSwitchCard(
                    title = stringResource(R.string.verbose_name),
                    description = stringResource(R.string.verbose_desc),
                    checked = verboseValue,
                    onCheckedChange = { verboseValue = it },
                    icon = Icons.Outlined.Info
                )
            }

            item {
                DefaultCard(onClick = {
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
                                logFile
                            )
                            val intent = Intent(Intent.ACTION_SEND)
                                .putExtra(Intent.EXTRA_STREAM, uri)
                                .setType("text/plain")
                                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("LogcatCapture", "Failed to save logs", e)
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.log_capture_failed),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }.start()
                }) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_bug_report),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.capture_logs),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.capture_logs_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.catshare_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.mutual_transmission_alliance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BrandSelectionDialog(
    currentBrandId: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onCustomId: () -> Unit
) {
    var selectedBrandId by remember { mutableIntStateOf(currentBrandId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_device_brand),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.select_device_brand_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isCustom = currentBrandId !in DeviceUtils.getSupportedBrands().map { it.first }

                        Surface(
                            onClick = onCustomId,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isCustom) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            tonalElevation = if (isCustom) 4.dp else 0.dp,
                            shadowElevation = if (isCustom) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = if (isCustom) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(end = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.custom_brand_id),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isCustom) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (isCustom) {
                                            androidx.compose.ui.text.font.FontWeight.SemiBold
                                        } else {
                                            androidx.compose.ui.text.font.FontWeight.Normal
                                        }
                                    )


                                    Text(
                                        text = stringResource(R.string.current_id, currentBrandId),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Text(
                                        text = stringResource(R.string.enter_custom_id),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = if (isCustom) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    items(DeviceUtils.getSupportedBrands()) { (id, name) ->
                        val isSelected = selectedBrandId == id

                        Surface(
                            onClick = { selectedBrandId = id },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            tonalElevation = if (isSelected) 4.dp else 0.dp,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSelected) {
                                        androidx.compose.ui.text.font.FontWeight.SemiBold
                                    } else {
                                        androidx.compose.ui.text.font.FontWeight.Normal
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (id == 0 && isSelected) {
                                    Text(
                                        text = stringResource(R.string.no_brand_identification),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.7f
                                        ),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(selectedBrandId)
                },
                enabled = selectedBrandId != currentBrandId
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
fun CustomBrandIdDialog(
    currentBrandId: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentBrandId.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    val errorInvalidNumber = stringResource(R.string.error_invalid_number)
    val errorIdRange = stringResource(R.string.error_id_range)
    val warningBrandRangeTemplate = stringResource(R.string.warning_brand_range)

    fun validateInput(input: String): Int? {
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
                val brandName = DeviceUtils.deviceNameById(suggestedId)
                warningMessage = String.format(
                    warningBrandRangeTemplate,
                    value,
                    brandName,
                    suggestedId
                )
                value
            }
            else -> value
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.custom_id_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.custom_id_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = {
                        inputValue = it
                        validateInput(it)
                    },
                    label = { Text(stringResource(R.string.brand_id)) },
                    placeholder = { Text("0-255") },
                    singleLine = true,
                    isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val validated = validateInput(inputValue)
                            if (validated != null && errorMessage == null) {
                                onConfirm(validated)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (warningMessage != null) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                if (errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (warningMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warningMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.custom_id_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validated = validateInput(inputValue)
                    if (validated != null && errorMessage == null) {
                        onConfirm(validated)
                    }
                },
                enabled = errorMessage == null && inputValue.isNotBlank()
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
fun SettingsSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    DefaultCard(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .padding(end = 20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

private fun buildBrandDisplayText(brandId: Int): String {
    val presetBrands = DeviceUtils.getSupportedBrands().map { it.first }
    val knownBrandName = DeviceUtils.deviceNameById(brandId)

    return if (brandId in presetBrands) {
        return "$knownBrandName"
    } else if (knownBrandName != null) {
        "$knownBrandName ($brandId)"
    } else {
        "Custom ($brandId)"
    }
}