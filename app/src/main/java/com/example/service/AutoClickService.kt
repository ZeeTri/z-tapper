package com.example.service

import androidx.compose.foundation.combinedClickable
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Toast
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalTextStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.model.TouchPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoClickService : AccessibilityService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var controlMenuView: ComposeView? = null
    private var linesOverlayView: ComposeView? = null
    private val markerViews = mutableMapOf<String, ComposeView>()

    private var clickJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var menuCurrentX = 100f
    private var menuCurrentY = 300f
    private var lastScreenWidth = 0
    private var lastScreenHeight = 0

    companion object {
        private var instance: AutoClickService? = null
        fun getInstance(): AutoClickService? = instance
        val isServiceRunning: Boolean get() = instance != null

        val points = mutableStateListOf<TouchPoint>()
        val isRunning = mutableStateOf(false)
        val clickIntervalMs = mutableStateOf(1000L)
        val isDarkMode = mutableStateOf(false)
        val isOverlayActive = mutableStateOf(false)
        val targetTriggerKey = mutableStateOf(KeyEvent.KEYCODE_VOLUME_UP)
        val activePointNumber = mutableStateOf<Int?>(null)
        val isAdvancedMode = mutableStateOf(false)
        val enableTimingVariance = mutableStateOf(false)
        val timingVarianceMs = mutableStateOf(20L)

        var activeProfileId: Int? = null
        var autoLaunchedForPackage: String? = null
    }

    private var currentPackage: String? = null
    private var appDatabase: com.example.data.AppDatabase? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        appDatabase = com.example.data.AppDatabase.getDatabase(applicationContext)
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val displayMetrics = resources.displayMetrics
        lastScreenWidth = displayMetrics.widthPixels
        lastScreenHeight = displayMetrics.heightPixels
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val displayMetrics = resources.displayMetrics
        val newWidth = displayMetrics.widthPixels
        val newHeight = displayMetrics.heightPixels

        if (lastScreenWidth > 0 && lastScreenHeight > 0 && (lastScreenWidth != newWidth || lastScreenHeight != newHeight)) {
            val widthRatio = newWidth.toFloat() / lastScreenWidth.toFloat()
            val heightRatio = newHeight.toFloat() / lastScreenHeight.toFloat()

            for (i in points.indices) {
                val point = points[i]
                points[i] = point.copy(
                    x = point.x * widthRatio,
                    y = point.y * heightRatio,
                    endX = point.endX?.times(widthRatio),
                    endY = point.endY?.times(heightRatio)
                )
            }
            updateMarkersFromPoints()

            menuCurrentX *= widthRatio
            menuCurrentY *= heightRatio

            controlMenuView?.let {
                val menuParams = it.layoutParams as WindowManager.LayoutParams
                menuParams.x = menuCurrentX.toInt()
                menuParams.y = menuCurrentY.toInt()
                try {
                    windowManager.updateViewLayout(it, menuParams)
                } catch (e: Exception) {}
            }
        }
        lastScreenWidth = newWidth
        lastScreenHeight = newHeight
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        stopClicking()
        hideOverlay()
        store.clear()
        instance = null
        super.onDestroy()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        stopClicking()
        hideOverlay()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null && packageName != currentPackage) {
                currentPackage = packageName
                if (!isRunning.value) {
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            val matchedProfile = appDatabase?.clickProfileDao()?.getProfileByPackage(packageName)
                            if (matchedProfile != null) {
                                if (activeProfileId != matchedProfile.id) {
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        points.clear()
                                        points.addAll(matchedProfile.points)
                                        clickIntervalMs.value = matchedProfile.intervalMs
                                        activeProfileId = matchedProfile.id
                                        android.widget.Toast.makeText(this@AutoClickService, "Auto-loaded profile: ${matchedProfile.name}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                if (matchedProfile.autoLaunchEnabled) {
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        autoLaunchedForPackage = packageName
                                        if (!isOverlayActive.value) {
                                            showOverlay()
                                        }
                                    }
                                }
                            } else {

                                if (autoLaunchedForPackage != null && packageName != autoLaunchedForPackage && packageName != applicationContext.packageName) {
                                    val appInfo = try { packageManager.getApplicationInfo(packageName, 0) } catch (e: Exception) { null }
                                    val isSystemApp = appInfo != null && (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                                    val isLaunchable = packageManager.getLaunchIntentForPackage(packageName) != null

                                    val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply { addCategory(android.content.Intent.CATEGORY_HOME) }
                                    val resolveInfo = packageManager.resolveActivity(homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                                    val homePackage = resolveInfo?.activityInfo?.packageName

                                    if (isLaunchable || (appInfo != null && !isSystemApp) || packageName == homePackage) {
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            hideOverlay()
                                            autoLaunchedForPackage = null

                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == targetTriggerKey.value && isOverlayActive.value) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (isRunning.value) {
                    stopClicking()
                } else {
                    startClicking()
                }
            }
            return true 
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {
        stopClicking()
    }

    fun showOverlay() {
        if (isOverlayActive.value) return
        isOverlayActive.value = true

        if (points.isEmpty()) {
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            points.add(TouchPoint(1, (screenWidth / 2 - 100).toFloat(), (screenHeight / 2 - 100).toFloat()))
        }

        createLinesOverlay()
        createControlMenuOverlay()
        updateMarkersFromPoints()
    }

    fun hideOverlay() {
        isOverlayActive.value = false
        stopClicking()

        controlMenuView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {

            }
        }
        controlMenuView = null

        linesOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        linesOverlayView = null

        for (markerView in markerViews.values) {
            try {
                windowManager.removeView(markerView)
            } catch (e: Exception) {

            }
        }
        markerViews.clear()
    }

    private fun createLinesOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        linesOverlayView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@AutoClickService)
            setViewTreeViewModelStoreOwner(this@AutoClickService)
            setViewTreeSavedStateRegistryOwner(this@AutoClickService)

            setContent {
                val dark = isDarkMode.value
                val isRunningState = isRunning.value
                val activePoint = activePointNumber.value
                val density = resources.displayMetrics.density
                val offset = 24 * density

                Canvas(modifier = Modifier.fillMaxSize()) {
                    points.forEach { point ->
                        if (point.isSwipe && point.endX != null && point.endY != null) {
                            val isActive = isRunningState && activePoint == point.number
                            val color = if (isActive) {
                                if (dark) Color.White else Color.Black
                            } else {
                                if (dark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                            }

                            val startOffset = Offset(point.x + offset, point.y + offset)
                            val endOffset = Offset(point.endX!! + offset, point.endY!! + offset)

                            drawLine(
                                color = color,
                                start = startOffset,
                                end = endOffset,
                                strokeWidth = 8f * density,
                                cap = StrokeCap.Round
                            )

                            val angle = kotlin.math.atan2(endOffset.y - startOffset.y, endOffset.x - startOffset.x)
                            val arrowLength = 20f * density
                            val arrowAngle = Math.PI / 6 

                            val p1x = endOffset.x - arrowLength * kotlin.math.cos(angle - arrowAngle).toFloat()
                            val p1y = endOffset.y - arrowLength * kotlin.math.sin(angle - arrowAngle).toFloat()
                            val p2x = endOffset.x - arrowLength * kotlin.math.cos(angle + arrowAngle).toFloat()
                            val p2y = endOffset.y - arrowLength * kotlin.math.sin(angle + arrowAngle).toFloat()

                            val arrowPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(endOffset.x, endOffset.y)
                                lineTo(p1x, p1y)
                                moveTo(endOffset.x, endOffset.y)
                                lineTo(p2x, p2y)
                            }
                            drawPath(
                                path = arrowPath,
                                color = color,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 8f * density,
                                    cap = StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }

        try {
            windowManager.addView(linesOverlayView, params)
        } catch (e: Exception) {}
    }

    private fun createControlMenuOverlay() {
        val menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = menuCurrentX.toInt()
            y = menuCurrentY.toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alpha = 1.0f
            }
        }

        controlMenuView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AutoClickService)
            setViewTreeSavedStateRegistryOwner(this@AutoClickService)
            setContent {
                OverlayTheme {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                            .onSizeChanged { size ->
                                if (menuParams.width != size.width || menuParams.height != size.height) {
                                    menuParams.width = size.width
                                    menuParams.height = size.height
                                    try {
                                        windowManager.updateViewLayout(this@apply, menuParams)
                                    } catch (e: Exception) {}
                                }
                            }
                    ) {
                        ControlMenuContent(
                            onDrag = { dx, dy ->
                                val metrics = resources.displayMetrics
                                val screenW = metrics.widthPixels
                                val screenH = metrics.heightPixels
                                val w = this@apply.width.toFloat()
                                val h = this@apply.height.toFloat()

                                menuCurrentX = (menuCurrentX + dx).coerceIn(0f, (screenW - w).coerceAtLeast(0f))
                                menuCurrentY = (menuCurrentY + dy).coerceIn(0f, (screenH - h).coerceAtLeast(0f))
                                menuParams.x = menuCurrentX.toInt()
                                menuParams.y = menuCurrentY.toInt()
                                try {
                                    windowManager.updateViewLayout(this@apply, menuParams)
                                } catch (e: Exception) {}
                            },
                            onPlayPause = {
                                if (isRunning.value) {
                                    stopClicking()
                                } else {
                                    startClicking()
                                }
                            },
                            onAddPoint = {
                                val density = resources.displayMetrics.density
                                val newNum = points.size + 1
                                val newPoint = TouchPoint(
                                    number = newNum,
                                    x = menuCurrentX + (50 * density) + (newNum * 10),
                                    y = menuCurrentY + (50 * density) + (newNum * 10)
                                )
                                points.add(newPoint)
                                addMarkerForPoint(newPoint)
                            },
                            onAddSwipe = {
                                val density = resources.displayMetrics.density
                                val newNum = points.size + 1
                                val newPoint = TouchPoint(
                                    number = newNum,
                                    x = menuCurrentX + (50 * density) + (newNum * 10),
                                    y = menuCurrentY + (50 * density) + (newNum * 10),
                                    isSwipe = true,
                                    endX = menuCurrentX + (150 * density) + (newNum * 10),
                                    endY = menuCurrentY + (150 * density) + (newNum * 10),
                                    holdDurationMs = 500L
                                )
                                points.add(newPoint)
                                addMarkerForPoint(newPoint)
                            },
                            onRemovePoint = {
                                if (points.isNotEmpty()) {
                                    val removed = points.removeAt(points.size - 1)
                                    markerViews["${removed.number}_start"]?.let {
                                        try {
                                            windowManager.removeView(it)
                                        } catch (e: Exception) {}
                                        markerViews.remove("${removed.number}_start")
                                    }
                                    markerViews["${removed.number}_end"]?.let {
                                        try {
                                            windowManager.removeView(it)
                                        } catch (e: Exception) {}
                                        markerViews.remove("${removed.number}_end")
                                    }
                                }
                            },
                            onHideTargets = { hide ->
                                setTargetsVisible(!hide)
                            },
                            onClose = {
                                hideOverlay()
                            }
                        )
                    }
                }
            }
        }

        try {
            windowManager.addView(controlMenuView, menuParams)
        } catch (e: Exception) {
            isOverlayActive.value = false
        }
    }
    fun setTargetsVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        for (view in markerViews.values) {
            view.visibility = visibility
        }
        linesOverlayView?.visibility = visibility
    }

    fun updateMarkersFromPoints() {
        if (!isOverlayActive.value) return

        for (view in markerViews.values) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {}
        }
        markerViews.clear()

        for (point in points) {
            addMarkerForPoint(point)
        }
    }

    private fun setMarkersTouchable(touchable: Boolean) {
        val flags = if (touchable) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        for (view in markerViews.values) {
            val params = view.layoutParams as? WindowManager.LayoutParams ?: continue
            params.flags = flags
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {}
        }
    }

    private fun addMarkerForPoint(point: TouchPoint) {
        val initialFlags = if (isRunning.value) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

        var currentX = point.x
        var currentY = point.y
        val markerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            initialFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentX.toInt()
            y = currentY.toInt()
        }

        val markerView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@AutoClickService)
            setViewTreeViewModelStoreOwner(this@AutoClickService)
            setViewTreeSavedStateRegistryOwner(this@AutoClickService)

            setContent {
                val currentPoint = points.find { it.number == point.number } ?: point
                val label = if (currentPoint.isSwipe) "S${currentPoint.number}" else currentPoint.number.toString()
                val isSwipe = currentPoint.isSwipe
                OverlayTheme {
                    MarkerContent(
                        pointNumber = point.number,
                        label = label,
                        showPopupConfig = true,
                        onDrag = { dx, dy ->
                            currentX += dx
                            currentY += dy
                            markerParams.x = currentX.toInt()
                            markerParams.y = currentY.toInt()
                            try {
                                windowManager.updateViewLayout(this, markerParams)
                            } catch (e: Exception) {}

                            val index = points.indexOfFirst { it.number == point.number }
                            if (index != -1) {
                                points[index] = points[index].copy(x = currentX, y = currentY)
                            } else {
                                point.x = currentX
                                point.y = currentY
                            }
                        },
                        onExpandedChange = { expanded ->
                            if (expanded) {
                                markerParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            } else {
                                markerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            }
                            try {
                                windowManager.updateViewLayout(this, markerParams)
                            } catch (e: Exception) {}
                        }
                    )
                }
            }
        }

        markerViews["${point.number}_start"] = markerView
        try {
            windowManager.addView(markerView, markerParams)
        } catch (e: Exception) {}

        if (point.isSwipe) {
            var endCurrentX = point.endX ?: (point.x + 100f)
            var endCurrentY = point.endY ?: (point.y + 100f)
            val endMarkerParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                initialFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = endCurrentX.toInt()
                y = endCurrentY.toInt()
            }

            val endMarkerView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(this@AutoClickService)
                setViewTreeViewModelStoreOwner(this@AutoClickService)
                setViewTreeSavedStateRegistryOwner(this@AutoClickService)

                setContent {
                    val currentPoint = points.find { it.number == point.number } ?: point
                    OverlayTheme {
                        MarkerContent(
                            pointNumber = currentPoint.number,
                            label = "E${currentPoint.number}",
                            showPopupConfig = false,
                            onDrag = { dx, dy ->
                                endCurrentX += dx
                                endCurrentY += dy
                                endMarkerParams.x = endCurrentX.toInt()
                                endMarkerParams.y = endCurrentY.toInt()
                                try {
                                    windowManager.updateViewLayout(this, endMarkerParams)
                                } catch (e: Exception) {}

                                val index = points.indexOfFirst { it.number == point.number }
                                if (index != -1) {
                                    points[index] = points[index].copy(endX = endCurrentX, endY = endCurrentY)
                                } else {
                                    point.endX = endCurrentX
                                    point.endY = endCurrentY
                                }
                            },
                            onExpandedChange = {} 
                        )
                    }
                }
            }
            markerViews["${point.number}_end"] = endMarkerView
            try {
                windowManager.addView(endMarkerView, endMarkerParams)
            } catch (e: Exception) {}
        }
    }

    fun startClicking() {
        if (isRunning.value) return
        if (points.isEmpty()) return

        isRunning.value = true
        setMarkersTouchable(false)
        clickJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive && isRunning.value) {
                val currentPoints = points.toList()
                if (currentPoints.isEmpty()) {
                    isRunning.value = false
                    break
                }
                for (point in currentPoints) {
                    if (!isRunning.value) break
                    activePointNumber.value = point.number

                    val density = resources.displayMetrics.density
                    val offset = (24 * density).toInt() 
                    val targetX = point.x + offset
                    val targetY = point.y + offset

                    if (point.isSwipe && point.endX != null && point.endY != null) {
                        val endTargetX = point.endX!! + offset
                        val endTargetY = point.endY!! + offset
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            dispatchSwipe(targetX, targetY, endTargetX, endTargetY, point.holdDurationMs)
                        }
                    } else {
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            dispatchClick(targetX, targetY, point.holdDurationMs)
                        }
                    }

                    var baseDelay = if (isAdvancedMode.value) {
                        point.holdDurationMs + point.delayAfterMs
                    } else {
                        clickIntervalMs.value
                    }

                    if (enableTimingVariance.value) {
                        val variance = timingVarianceMs.value
                        if (variance > 0) {
                            val offset = kotlin.random.Random.nextLong(-variance, variance + 1)
                            baseDelay += offset
                        }
                    }

                    delay(Math.max(10L, baseDelay))
                }
                activePointNumber.value = null
            }
        }
    }

    fun stopClicking() {
        isRunning.value = false
        activePointNumber.value = null
        setMarkersTouchable(true)
        clickJob?.cancel()
        clickJob = null
    }

    private fun dispatchClick(x: Float, y: Float, durationMs: Long = 50) {
        try {
            val metrics = resources.displayMetrics
            val safeX = Math.max(0f, Math.min(metrics.widthPixels.toFloat() - 1f, x))
            val safeY = Math.max(0f, Math.min(metrics.heightPixels.toFloat() - 1f, y))

            val path = Path().apply {
                moveTo(safeX, safeY)
            }
            val gestureBuilder = GestureDescription.Builder()
            val strokeDescription = GestureDescription.StrokeDescription(
                path,
                0, 
                Math.max(1L, durationMs) 
            )
            gestureBuilder.addStroke(strokeDescription)
            val result = dispatchGesture(gestureBuilder.build(), object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this@AutoClickService, "Click blocked by system UI", Toast.LENGTH_SHORT).show()
                    }
                }
            }, null)
            if (!result) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@AutoClickService, "Failed to dispatch click", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 500) {
        try {
            val metrics = resources.displayMetrics
            val maxX = metrics.widthPixels.toFloat() - 1f
            val maxY = metrics.heightPixels.toFloat() - 1f

            val safeStartX = Math.max(0f, Math.min(maxX, startX))
            val safeStartY = Math.max(0f, Math.min(maxY, startY))
            var safeEndX = Math.max(0f, Math.min(maxX, endX))
            var safeEndY = Math.max(0f, Math.min(maxY, endY))

            if (safeStartX == safeEndX && safeStartY == safeEndY) {
                safeEndX = Math.min(maxX, safeEndX + 1f)
                safeEndY = Math.min(maxY, safeEndY + 1f)
            }

            val path = Path().apply {
                moveTo(safeStartX, safeStartY)
                lineTo(safeEndX, safeEndY)
            }
            val gestureBuilder = GestureDescription.Builder()
            val strokeDescription = GestureDescription.StrokeDescription(
                path,
                0, 
                Math.max(1L, durationMs) 
            )
            gestureBuilder.addStroke(strokeDescription)
            val result = dispatchGesture(gestureBuilder.build(), object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this@AutoClickService, "Swipe blocked by system UI", Toast.LENGTH_SHORT).show()
                    }
                }
            }, null)
            if (!result) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@AutoClickService, "Failed to dispatch swipe", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun HelpIcon(tooltipText: String) {
    var expanded by remember { mutableStateOf(false) }
    val dark = AutoClickService.isDarkMode.value
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "Help",
                modifier = Modifier.size(16.dp),
                tint = if (dark) Color.LightGray else Color.DarkGray
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
fun OverlayTheme(content: @Composable () -> Unit) {
    val dark = AutoClickService.isDarkMode.value
    val colors = if (dark) {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFFE5E5EA),
            onPrimary = Color(0xFF1C1C1E),
            surface = Color(0xFF1C1C1E),
            onSurface = Color(0xFFFFFFFF),
            outline = Color(0xFF38383A)
        )
    } else {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF1C1C1E),
            onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1C1C1E),
            outline = Color(0xFFE5E5EA)
        )
    }

    MaterialTheme(colorScheme = colors) {
        Surface(color = Color.Transparent) {
            content()
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ControlMenuContent(
    onDrag: (Float, Float) -> Unit,
    onPlayPause: () -> Unit,
    onAddPoint: () -> Unit,
    onAddSwipe: () -> Unit,
    onRemovePoint: () -> Unit,
    onHideTargets: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val isRunningState = AutoClickService.isRunning.value
    val dark = AutoClickService.isDarkMode.value
    val isAdvancedMode = AutoClickService.isAdvancedMode.value
    var isMinimized by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val pillThickness = 56.dp
    val primaryButtonSize = 42.dp
    val secondaryButtonSize = 38.dp
    val iconSize = 20.dp
    val dragHandleSize = 36.dp

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        Crossfade(targetState = isMinimized, label = "MenuTransition") { minimized ->
            if (minimized) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) Color(0x4D1C1C1E) else Color(0x4DFFFFFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(4.dp)
                        .size(pillThickness)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .size(primaryButtonSize)
                                .clip(CircleShape)
                                .background(if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF))
                                .clickable { 
                                    isMinimized = false 
                                    onHideTargets(false)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Maximize menu",
                                tint = if (dark) Color.White else Color.Black,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            } else {
                Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) Color(0x4D1C1C1E) else Color(0x4DFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        if (isLandscape) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(6.dp)
            ) {

                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.height(pillThickness)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 8.dp).fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier.width(dragHandleSize).fillMaxHeight().pointerInput(Unit) {
                                detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount.x, dragAmount.y) }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DragHandle, "Drag menu", tint = Color(0xFF8E8E93), modifier = Modifier.size(24.dp))
                        }
                        Box(
                            modifier = Modifier.size(primaryButtonSize).clip(CircleShape).background(if (dark) Color.White else Color.Black).clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isRunningState) Icons.Default.Pause else Icons.Default.PlayArrow, "Toggle", tint = if (dark) Color.Black else Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                if (isAdvancedMode) {

                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.height(pillThickness)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 8.dp).fillMaxHeight()
                        ) {
                            Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).clickable(onClick = onAddPoint), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, "Add point", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                            }
                            Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).clickable(onClick = onAddSwipe), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Add swipe", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                            }
                            Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).clickable(onClick = onRemovePoint), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Remove, "Remove point", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.height(pillThickness)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 8.dp).fillMaxHeight()
                    ) {
                        Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).combinedClickable(
                            onClick = { 
                                isMinimized = true
                                onHideTargets(false)
                            },
                            onLongClick = { 
                                isMinimized = true
                                onHideTargets(true)
                            }
                        ), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, "Minimize", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                        }
                        Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, "Close", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                        }
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {

                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.width(pillThickness)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(dragHandleSize).pointerInput(Unit) {
                                detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount.x, dragAmount.y) }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DragHandle, "Drag menu", tint = Color(0xFF8E8E93), modifier = Modifier.size(24.dp))
                        }
                        Box(
                            modifier = Modifier.size(primaryButtonSize).clip(CircleShape).background(if (dark) Color.White else Color.Black).clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isRunningState) Icons.Default.Pause else Icons.Default.PlayArrow, "Toggle", tint = if (dark) Color.Black else Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                if (isAdvancedMode) {

                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.width(pillThickness)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).clickable(onClick = onAddPoint), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, "Add point", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                            }
                            Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).clickable(onClick = onAddSwipe), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Add swipe", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                            }
                            Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).clickable(onClick = onRemovePoint), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Remove, "Remove point", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.width(pillThickness)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).combinedClickable(
                            onClick = { 
                                isMinimized = true
                                onHideTargets(false)
                            },
                            onLongClick = { 
                                isMinimized = true
                                onHideTargets(true)
                            }
                        ), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, "Minimize", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                        }
                        Box(modifier = Modifier.size(secondaryButtonSize).clip(CircleShape).background(if (dark) Color(0x1AFFFFFF) else Color(0x0D000000)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, "Close", tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(iconSize))
                        }
                    }
                }
            }
        }
    }
}
}
}
}

@Composable
fun MarkerContent(
    pointNumber: Int,
    label: String,
    showPopupConfig: Boolean = true,
    onDrag: (Float, Float) -> Unit,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    val dark = AutoClickService.isDarkMode.value
    val isRunningState = AutoClickService.isRunning.value
    val activePoint by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { AutoClickService.activePointNumber.value == pointNumber } }
    val isAdvancedMode = AutoClickService.isAdvancedMode.value
    var isExpanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isRunningState) {
                        if (activePoint) {
                            if (dark) Color(0x40FFFFFF) else Color(0x40000000)
                        } else {
                            if (dark) Color(0x1AFFFFFF) else Color(0x1A000000)
                        }
                    } else {
                        if (dark) Color(0x1AFFFFFF) else Color(0x1A000000)
                    },
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isRunningState) {
                        if (activePoint) {
                            if (dark) Color.White else Color.Black
                        } else {
                            if (dark) Color(0x80FFFFFF) else Color(0x80000000)
                        }
                    } else {
                        if (dark) Color(0x40FFFFFF) else Color(0x40000000)
                    },
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (!isRunningState) {
                            isExpanded = !isExpanded
                            onExpandedChange(isExpanded)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (dark) Color(0xFF38383A) else Color(0xFFE5E5EA),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (dark) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (isExpanded && !isRunningState && showPopupConfig) {
            val point = AutoClickService.points.find { it.number == pointNumber } ?: return@Row

            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val density = androidx.compose.ui.platform.LocalDensity.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

            val popupWidthPx = with(density) { 240.dp.toPx() }
            val popupHeightPx = with(density) { 220.dp.toPx() }

            val popupOnLeft = point.x + with(density){ 48.dp.toPx() } + popupWidthPx > screenWidthPx
            val popupOnTop = point.y + popupHeightPx > screenHeightPx

            val align = if (popupOnLeft) {
                if (popupOnTop) Alignment.BottomEnd else Alignment.TopEnd
            } else {
                if (popupOnTop) Alignment.BottomStart else Alignment.TopStart
            }

            val offsetX = if (popupOnLeft) -56 else 56
            val offsetY = if (popupOnTop) -56 else 0

            androidx.compose.ui.window.Popup(
                alignment = align,
                offset = androidx.compose.ui.unit.IntOffset(with(density){offsetX.dp.roundToPx()}, with(density){offsetY.dp.roundToPx()}),
                properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true),
                onDismissRequest = {
                    isExpanded = false
                    onExpandedChange(false)
                }
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.width(240.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isAdvancedMode) {
                            Text("Target ${point.number} ${if(point.isSwipe) "Swipe" else "Click"} Macro", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (dark) Color.White else Color.Black)

                            OutlinedTextField(
                                value = point.holdDurationMs.toString(),
                                onValueChange = { newValue ->
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
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Hold (ms)", fontSize = 10.sp, color = if (dark) Color(0xFF8E8E93) else Color.DarkGray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        HelpIcon(tooltipText = "Duration to hold down the click or swipe on the screen.")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = if (dark) Color.White else Color.Black)
                            )
                        } else {
                            Text("Click Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (dark) Color.White else Color.Black)
                        }

                        OutlinedTextField(
                            value = point.delayAfterMs.toString(),
                            onValueChange = { newValue ->
                                if (newValue.isEmpty()) {
                                    val index = AutoClickService.points.indexOfFirst { it.number == point.number }
                                    if (index != -1) AutoClickService.points[index] = point.copy(delayAfterMs = 0)
                                } else {
                                    newValue.toLongOrNull()?.let { 
                                        val index = AutoClickService.points.indexOfFirst { it.number == point.number }
                                        if (index != -1) AutoClickService.points[index] = point.copy(delayAfterMs = it)
                                    }
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Click Interval (ms)", fontSize = 10.sp, color = if (dark) Color(0xFF8E8E93) else Color.DarkGray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    HelpIcon(tooltipText = "The pause duration after this action completes, before the next action starts.")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = if (dark) Color.White else Color.Black)
                        )
                    }
                }
            }
        }
    }
}
