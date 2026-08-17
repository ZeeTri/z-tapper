package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClickProfile
import com.example.service.AutoClickService
import com.example.ui.viewmodel.ClickerViewModel
import com.example.model.TouchPoint

@Composable
fun TargetCard(
    point: TouchPoint,
    onUpdateHoldDuration: (String) -> Unit,
    onUpdateDelayAfter: (String) -> Unit
) {
    val title = remember(point.isSwipe, point.number) {
        if (point.isSwipe) "Target ${point.number} (Swipe)" else "Target ${point.number} (Click)"
    }
    val holdDurationStr = remember(point.holdDurationMs) { point.holdDurationMs.toString() }
    val delayAfterStr = remember(point.delayAfterMs) { point.delayAfterMs.toString() }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = holdDurationStr,
                    onValueChange = onUpdateHoldDuration,
                    label = { Text("Hold (ms)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = delayAfterStr,
                    onValueChange = onUpdateDelayAfter,
                    label = { Text("Delay (ms)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun HelpIcon(tooltipText: String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "Help",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.padding(8.dp).widthIn(max = 250.dp)
        ) {
            Text(
                text = tooltipText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFFFFFFF),
            onPrimary = Color(0xFF121212),
            background = Color(0xFF121212),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF1C1C1E),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF2C2C2E),
            onSurfaceVariant = Color(0xFFE5E5EA),
            outline = Color(0xFF38383A)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1C1C1E),
            onPrimary = Color(0xFFFFFFFF),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1C1C1E),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1C1C1E),
            surfaceVariant = Color(0xFFF2F2F7),
            onSurfaceVariant = Color(0xFF3A3A3C),
            outline = Color(0xFFE5E5EA)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun MainScreen(viewModel: ClickerViewModel) {
    val context = LocalContext.current
    val isDark = AutoClickService.isDarkMode.value
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main"

    val isAccessibilityGranted by viewModel.isAccessibilityGranted
    val isOverlayGranted by viewModel.isOverlayGranted

    var intervalString by remember { mutableStateOf(AutoClickService.clickIntervalMs.value.toString()) }

    val isOverlayActive = AutoClickService.isOverlayActive.value
    val isAdvancedMode by AutoClickService.isAdvancedMode

    LaunchedEffect(AutoClickService.clickIntervalMs.value) {
        intervalString = AutoClickService.clickIntervalMs.value.toString()
    }

    LaunchedEffect(isAdvancedMode) {
        viewModel.saveAdvancedMode(isAdvancedMode)
    }

    AppTheme(darkTheme = isDark) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Z-TAPPER",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Precision Auto-Clicker",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme Icon",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE5E5EA)
                            )
                        )
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
                ) {
                    if (isAccessibilityGranted && isOverlayGranted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentRoute != "profiles") {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { 
                                            navController.navigate("settings") { launchSingleTop = true }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (isOverlayActive) {
                                        viewModel.stopClickerOverlay()
                                    } else {
                                        viewModel.startClickerOverlay()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverlayActive) Color(0xFFFF3B30) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isOverlayActive) Color.White else MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOverlayActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isOverlayActive) "Stop" else "Start"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOverlayActive) "CLOSE FLOATING MENU" else "LAUNCH FLOATING MENU",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Please grant all permissions above to customize, save profiles, and launch the real-time clicking overlay menu.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF3B30),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {

                if (!isAccessibilityGranted || !isOverlayGranted) {
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
                                text = "Permissions Required",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAccessibilityGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = "Status",
                                    tint = if (isAccessibilityGranted) Color(0xFF34C759) else Color(0xFFFF3B30),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Accessibility Service",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Allows performing automated click coordinates on game screens safely without root.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }

                            if (!isAccessibilityGranted) {
                                if (android.os.Build.VERSION.SDK_INT >= 33) {
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Warning",
                                                tint = Color(0xFFFF9500),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Android 13+ Important Step:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("If the Accessibility setting is grayed out or says 'Restricted', you must first:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("1. Open Z-TAPPER App Info (button below)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("2. Tap the 3-dots (⋮) in top right", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("3. Tap 'Allow restricted settings'", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("4. Come back and tap Grant Accessibility", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("App Info", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Grant Accessibility", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOverlayGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = "Status",
                                    tint = if (isOverlayGranted) Color(0xFF34C759) else Color(0xFFFF3B30),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Display Over Other Apps",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Allows showing floating control panel overlay directly on top of games during gameplay.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }

                            if (!isOverlayGranted) {
                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Grant Overlay Permission", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                }

                val isServiceRunningState by viewModel.isServiceRunningState
                if (isAccessibilityGranted && !isServiceRunningState) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF9500).copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFFF9500),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Background Service Inactive",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFFF9500)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Although enabled in settings, the Android system has not yet started the service process. Please turn Tapper OFF and back ON in settings to activate it.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF9500),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Open Accessibility Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "UI Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                HelpIcon(tooltipText = "Basic mode uses a single target for simple clicking. Advanced mode supports multi-target macros and swipes.")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        AutoClickService.isAdvancedMode.value = false
                                        if (AutoClickService.points.size > 1) {
                                            val first = AutoClickService.points.first()
                                            AutoClickService.points.clear()
                                            AutoClickService.points.add(first.copy(isSwipe = false))
                                            AutoClickService.getInstance()?.updateMarkersFromPoints()
                                        } else if (AutoClickService.points.size == 1) {
                                            val first = AutoClickService.points.first()
                                            if (first.isSwipe) {
                                                AutoClickService.points[0] = first.copy(isSwipe = false)
                                                AutoClickService.getInstance()?.updateMarkersFromPoints()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isAdvancedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (!isAdvancedMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Basic", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { AutoClickService.isAdvancedMode.value = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAdvancedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isAdvancedMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Advanced", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (isAccessibilityGranted && isOverlayGranted) {

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
                                    text = "Clicker Parameters",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Click Interval (milliseconds)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        HelpIcon(tooltipText = "The pause duration between consecutive clicks or loop iterations.")
                                    }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {

                                            IconButton(
                                                onClick = {
                                                    val current = AutoClickService.clickIntervalMs.value
                                                    if (current > 100) {
                                                        AutoClickService.clickIntervalMs.value = current - 100
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline,
                                                        CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Decrease interval by 100ms"
                                                )
                                            }

                                            OutlinedTextField(
                                                value = intervalString,
                                                onValueChange = { newValue ->
                                                    intervalString = newValue.filter { it.isDigit() }
                                                    val longVal = intervalString.toLongOrNull() ?: 1000L
                                                    AutoClickService.clickIntervalMs.value =
                                                        if (longVal < 10) 10L else longVal
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    val current = AutoClickService.clickIntervalMs.value
                                                    AutoClickService.clickIntervalMs.value = current + 100
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline,
                                                        CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Increase interval by 100ms"
                                                )
                                            }
                                        }
                                    }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Hardware Button Toggle",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Choose which volume button starts/stops the clicker when the overlay is active.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { AutoClickService.targetTriggerKey.value = KeyEvent.KEYCODE_VOLUME_UP },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (AutoClickService.targetTriggerKey.value == KeyEvent.KEYCODE_VOLUME_UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (AutoClickService.targetTriggerKey.value == KeyEvent.KEYCODE_VOLUME_UP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Volume Up", fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = { AutoClickService.targetTriggerKey.value = KeyEvent.KEYCODE_VOLUME_DOWN },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (AutoClickService.targetTriggerKey.value == KeyEvent.KEYCODE_VOLUME_DOWN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (AutoClickService.targetTriggerKey.value == KeyEvent.KEYCODE_VOLUME_DOWN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Volume Down", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Enable Timing Variance",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Adds random fluctuation to input rhythm",
                                                fontSize = 12.sp,
                                                color = Color(0xFF8E8E93)
                                            )
                                        }
                                        Switch(
                                            checked = AutoClickService.enableTimingVariance.value,
                                            onCheckedChange = { 
                                                AutoClickService.enableTimingVariance.value = it
                                                viewModel.saveTimingVarianceEnabled(it)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                uncheckedThumbColor = if (isDark) Color(0xFF8E8E93) else Color(0xFFFFFFFF),
                                                uncheckedTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                                                uncheckedBorderColor = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
                                            )
                                        )
                                    }

                                    if (AutoClickService.enableTimingVariance.value) {
                                        var varianceString by remember { mutableStateOf(AutoClickService.timingVarianceMs.value.toString()) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Variance Window (ms):",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            OutlinedTextField(
                                                value = varianceString,
                                                onValueChange = { newValue ->
                                                    varianceString = newValue.filter { it.isDigit() }
                                                    val longVal = varianceString.toLongOrNull() ?: 20L
                                                    val clampedVal = if (longVal < 0) 0L else if (longVal > 5000) 5000L else longVal
                                                    AutoClickService.timingVarianceMs.value = clampedVal
                                                    viewModel.saveTimingVarianceMs(clampedVal)
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                if (isAdvancedMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Mapped Click Points",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${AutoClickService.points.size} Active",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isAdvancedMode && AutoClickService.points.isNotEmpty()) {
                        item {
                            Text(
                                text = "Macro Configuration",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(items = AutoClickService.points, key = { it.number }) { point ->
                            TargetCard(
                                point = point,
                                onUpdateHoldDuration = { newValue ->
                                    if (newValue.isEmpty()) {
                                        val index = AutoClickService.points.indexOfFirst { it.number == point.number }
                                        if (index != -1) AutoClickService.points[index] = point.copy(holdDurationMs = 0)
                                    } else {
                                        newValue.toLongOrNull()?.let {
                                            val index = AutoClickService.points.indexOfFirst { it.number == point.number }
                                            if (index != -1) AutoClickService.points[index] = point.copy(holdDurationMs = it)
                                        }
                                    }
                                },
                                onUpdateDelayAfter = { newValue ->
                                    if (newValue.isEmpty()) {
                                        val index = AutoClickService.points.indexOfFirst { it.number == point.number }
                                        if (index != -1) AutoClickService.points[index] = point.copy(delayAfterMs = 0)
                                    } else {
                                        newValue.toLongOrNull()?.let {
                                            val index = AutoClickService.points.indexOfFirst { it.number == point.number }
                                            if (index != -1) AutoClickService.points[index] = point.copy(delayAfterMs = it)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                } 
                } 

                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { backDispatcher?.onBackPressed() },
                        onNavigateToProfiles = { 
                            navController.navigate("profiles") { launchSingleTop = true }
                        },
                        isDark = isDark,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable("profiles") {
                    ProfilesScreen(
                        viewModel = viewModel,
                        onNavigateBack = { backDispatcher?.onBackPressed() },
                        isDark = isDark,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
