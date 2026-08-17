package com.example.ui.viewmodel

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.ClickProfile
import com.example.data.ClickProfileRepository
import com.example.service.AutoClickService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.catch

class ClickerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = com.example.data.AppDatabase.getDatabase(application)

    private val repository = ClickProfileRepository(db.clickProfileDao())

    val allProfiles: StateFlow<List<ClickProfile>> = repository.allProfiles
        .catch { e ->
            e.printStackTrace()

            db.clickProfileDao().deleteAllProfiles()
            emit(emptyList())
        }
        .flowOn(kotlinx.coroutines.Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isAccessibilityGranted = mutableStateOf(false)
    val isServiceRunningState = mutableStateOf(false)
    val isOverlayGranted = mutableStateOf(false)

    init {
        val prefs = application.getSharedPreferences("tapper_prefs", Context.MODE_PRIVATE)
        val systemDark = (application.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val hasCustomTheme = prefs.getBoolean("has_custom_theme", false)
        AutoClickService.isDarkMode.value = if (hasCustomTheme) prefs.getBoolean("dark_mode", systemDark) else systemDark
        AutoClickService.isAdvancedMode.value = prefs.getBoolean("advanced_mode", false)
        AutoClickService.enableTimingVariance.value = prefs.getBoolean("timing_variance_enabled", false)
        AutoClickService.timingVarianceMs.value = prefs.getLong("timing_variance_ms", 20L)
        checkPermissions()
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {

        if (AutoClickService.isServiceRunning) return true

        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            if (am != null) {
                val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) ?: emptyList()
                for (service in enabledServices) {
                    val info = service.resolveInfo?.serviceInfo
                    if (info != null && info.packageName == context.packageName && info.name == AutoClickService::class.java.name) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {

        }

        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (enabledServicesSetting != null) {
                val expectedComponent = ComponentName(context, AutoClickService::class.java)
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServicesSetting)
                while (colonSplitter.hasNext()) {
                    val componentNameString = colonSplitter.next()
                    val enabledService = ComponentName.unflattenFromString(componentNameString)
                    if (enabledService != null && enabledService == expectedComponent) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {

        }

        return false
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        isAccessibilityGranted.value = isAccessibilityServiceEnabled(context)
        isServiceRunningState.value = AutoClickService.isServiceRunning
        isOverlayGranted.value = Settings.canDrawOverlays(context)
    }

    fun toggleDarkMode() {
        val newValue = !AutoClickService.isDarkMode.value
        AutoClickService.isDarkMode.value = newValue
        saveDarkMode(newValue)
    }

    fun saveDarkMode(enabled: Boolean) {
        val prefs = getApplication<Application>().getSharedPreferences("tapper_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("dark_mode", enabled)
            .putBoolean("has_custom_theme", true)
            .apply()
    }

    fun saveAdvancedMode(enabled: Boolean) {
        val prefs = getApplication<Application>().getSharedPreferences("tapper_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("advanced_mode", enabled).apply()
    }

    fun saveTimingVarianceEnabled(enabled: Boolean) {
        val prefs = getApplication<Application>().getSharedPreferences("tapper_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("timing_variance_enabled", enabled).apply()
    }

    fun saveTimingVarianceMs(ms: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("tapper_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("timing_variance_ms", ms).apply()
    }

    fun saveCurrentAsProfile(name: String) {

        val service = AutoClickService.getInstance()
        if (AutoClickService.isRunning.value && service != null) {
            service.stopClicking()
        }

        viewModelScope.launch {
            val profile = ClickProfile(
                name = name,
                intervalMs = AutoClickService.clickIntervalMs.value,
                points = AutoClickService.points.toList(),
                isAdvanced = AutoClickService.isAdvancedMode.value
            )
            repository.insert(profile)
        }
    }

    fun loadProfile(profile: ClickProfile) {
        saveAdvancedMode(profile.isAdvanced)
        AutoClickService.clickIntervalMs.value = profile.intervalMs
        AutoClickService.points.clear()
        AutoClickService.points.addAll(profile.points)
        AutoClickService.activeProfileId = profile.id
        AutoClickService.autoLaunchedForPackage = null

        AutoClickService.getInstance()?.let { service ->
            if (AutoClickService.isOverlayActive.value) {
                service.hideOverlay()
                service.showOverlay()
            }
        }
    }

    fun updateProfile(profile: ClickProfile) {
        val service = AutoClickService.getInstance()
        if (AutoClickService.isRunning.value && service != null) {
            service.stopClicking()
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.update(profile)
        }
    }

    fun deleteProfile(profile: ClickProfile) {
        val service = AutoClickService.getInstance()
        if (AutoClickService.isRunning.value && service != null) {
            service.stopClicking()
        }
        viewModelScope.launch {
            repository.delete(profile)
        }
    }

    fun startClickerOverlay() {
        val service = AutoClickService.getInstance()
        if (service != null) {
            service.showOverlay()
        } else {
            android.widget.Toast.makeText(
                getApplication(),
                "Accessibility service is not running yet. Please toggle 'Tapper' OFF and ON in Accessibility Settings.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    fun stopClickerOverlay() {
        val service = AutoClickService.getInstance()
        if (service != null) {
            service.hideOverlay()
        } else {
            AutoClickService.isOverlayActive.value = false
        }
    }
}
