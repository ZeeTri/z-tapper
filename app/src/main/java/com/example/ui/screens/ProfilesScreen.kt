package com.example.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ClickProfile
import com.example.ui.viewmodel.ClickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: ClickerViewModel,
    onNavigateBack: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.allProfiles.collectAsState()
    var newProfileName by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.padding(end = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Preset Manager", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Touch-Point Mapping Profiles",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newProfileName,
                                onValueChange = { newProfileName = it },
                                placeholder = { Text("Profile Name (e.g. RPG Farm)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    if (newProfileName.isNotBlank()) {
                                        viewModel.saveCurrentAsProfile(newProfileName.trim())
                                        newProfileName = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save layout profile",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        if (profiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No profiles saved yet.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }

            if (profiles.isNotEmpty()) {
                items(items = profiles, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        isDark = isDark,
                        onLoad = { viewModel.loadProfile(it) },
                        onDelete = { viewModel.deleteProfile(it) },
                        onUpdate = { viewModel.updateProfile(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileRow(
    profile: ClickProfile,
    isDark: Boolean,
    onLoad: (ClickProfile) -> Unit,
    onDelete: (ClickProfile) -> Unit,
    onUpdate: (ClickProfile) -> Unit
) {
    val isAdvancedMode = profile.isAdvanced || profile.points.size > 1
    val subtitle = remember(profile.points.size, profile.intervalMs, isAdvancedMode) {
        if (isAdvancedMode) {
            "${profile.points.size} points | Advanced"
        } else {
            "${profile.points.size} points | ${profile.intervalMs}ms interval"
        }
    }
    val bgColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    var showAppPicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editNameText by remember(profile.name) { mutableStateOf(profile.name) }
    var editIntervalText by remember(profile.intervalMs) { mutableStateOf(profile.intervalMs.toString()) }
    var editedPoints by remember(profile.points) { mutableStateOf(profile.points) }

    if (showEditDialog) {
        Dialog(
            onDismissRequest = { showEditDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isAdvancedMode) {
                    editedPoints.forEach { point ->
                        Box(
                            modifier = Modifier
                                .offset { androidx.compose.ui.unit.IntOffset(point.x.toInt(), point.y.toInt()) }
                                .size(48.dp)
                                .background(Color(0xFFFF3B30).copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                                .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(point.number.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (point.isSwipe) {
                            Box(
                                modifier = Modifier
                                    .offset { androidx.compose.ui.unit.IntOffset((point.endX ?: point.x).toInt(), (point.endY ?: point.y).toInt()) }
                                    .size(48.dp)
                                    .background(Color(0xFF007AFF).copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                                    .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(point.number.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .widthIn(min = 280.dp, max = 400.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(if (isAdvancedMode) "Edit Advanced Preset" else "Edit Preset", style = MaterialTheme.typography.headlineSmall)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = editNameText,
                                onValueChange = { editNameText = it },
                                singleLine = true,
                                label = { Text("Preset Name") }
                            )

                            if (!isAdvancedMode) {
                                OutlinedTextField(
                            value = editIntervalText,
                            onValueChange = { editIntervalText = it.filter { char -> char.isDigit() } },
                            singleLine = true,
                            label = { Text("Click Interval (ms)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    } else {
                        Text("Targets:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        editedPoints.forEachIndexed { index, point ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Target ${point.number}", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)

                                    Text("X Position: ${point.x.toInt()}", fontSize = 12.sp)
                                    Slider(
                                        value = point.x,
                                        onValueChange = { newX ->
                                            val newList = editedPoints.toMutableList()
                                            newList[index] = point.copy(x = newX)
                                            editedPoints = newList
                                        },
                                        valueRange = 0f..2500f
                                    )

                                    Text("Y Position: ${point.y.toInt()}", fontSize = 12.sp)
                                    Slider(
                                        value = point.y,
                                        onValueChange = { newY ->
                                            val newList = editedPoints.toMutableList()
                                            newList[index] = point.copy(y = newY)
                                            editedPoints = newList
                                        },
                                        valueRange = 0f..2500f
                                    )

                                    if (point.isSwipe) {
                                        Text("End X Position: ${(point.endX ?: point.x).toInt()}", fontSize = 12.sp)
                                        Slider(
                                            value = point.endX ?: point.x,
                                            onValueChange = { newX ->
                                                val newList = editedPoints.toMutableList()
                                                newList[index] = point.copy(endX = newX)
                                                editedPoints = newList
                                            },
                                            valueRange = 0f..2500f
                                        )

                                        Text("End Y Position: ${(point.endY ?: point.y).toInt()}", fontSize = 12.sp)
                                        Slider(
                                            value = point.endY ?: point.y,
                                            onValueChange = { newY ->
                                                val newList = editedPoints.toMutableList()
                                                newList[index] = point.copy(endY = newY)
                                                editedPoints = newList
                                            },
                                            valueRange = 0f..2500f
                                        )
                                    }

                                    OutlinedTextField(
                                        value = point.delayAfterMs.toString(),
                                        onValueChange = { newDelay ->
                                            val delay = newDelay.filter { it.isDigit() }.toLongOrNull() ?: 0L
                                            val newList = editedPoints.toMutableList()
                                            newList[index] = point.copy(delayAfterMs = delay)
                                            editedPoints = newList
                                        },
                                        singleLine = true,
                                        label = { Text("Delay After (ms)") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        )
                                    )

                                    OutlinedTextField(
                                        value = point.holdDurationMs.toString(),
                                        onValueChange = { newHold ->
                                            val hold = newHold.filter { it.isDigit() }.toLongOrNull() ?: 0L
                                            val newList = editedPoints.toMutableList()
                                            newList[index] = point.copy(holdDurationMs = hold)
                                            editedPoints = newList
                                        },
                                        singleLine = true,
                                        label = { Text("Hold Duration (ms)") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val interval = editIntervalText.toLongOrNull() ?: profile.intervalMs
                            if (editNameText.isNotBlank()) {
                                onUpdate(profile.copy(name = editNameText, intervalMs = if (isAdvancedMode) profile.intervalMs else interval, points = editedPoints))
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
    }
}

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { pkg ->
                onUpdate(profile.copy(linkedAppPackage = pkg))
                showAppPicker = false
            },
            onClear = {
                onUpdate(profile.copy(linkedAppPackage = null))
                showAppPicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )
                if (profile.linkedAppPackage != null) {
                    Text(
                        text = "Linked: ${profile.linkedAppPackage}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit preset",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { showAppPicker = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Link app",
                        tint = if (profile.linkedAppPackage != null) MaterialTheme.colorScheme.primary else Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { onLoad(profile) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Load profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { onDelete(profile) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete profile",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (profile.linkedAppPackage != null) {
            HorizontalDivider(color = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Launch",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Open overlay when linked app starts",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                Switch(
                    checked = profile.autoLaunchEnabled,
                    onCheckedChange = { onUpdate(profile.copy(autoLaunchEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = if (isDark) Color(0xFF8E8E93) else Color(0xFFFFFFFF),
                        uncheckedTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                        uncheckedBorderColor = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
                    )
                )
            }
        }
    }
}

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    data class AppItem(val packageName: String, val name: String)
    var installedApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val apps = try {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { appInfo ->
                        val hasLaunchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) != null
                        val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        hasLaunchIntent || !isSystemApp
                    }
                    .map { 
                        val name = try { it.loadLabel(pm).toString() } catch (e: Exception) { it.packageName }
                        AppItem(it.packageName, name)
                    }
                    .sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
            installedApps = apps
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(400.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Target App", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClear, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Clear Linked App")
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(installedApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app.packageName) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = app.name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}
