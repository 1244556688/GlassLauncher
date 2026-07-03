package com.example

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extension for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "glass_launcher_prefs")

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val iconBitmap: ImageBitmap?,
    val isSystemApp: Boolean
)

enum class WallpaperTheme(val displayName: String, val description: String) {
    SPACE_NEBULA("Space Nebula (Default)", "Futuristic cosmic glassmorphism"),
    CYAN_AURORA("Cyan Aurora", "Cybernetic green and teal lighting"),
    OBSIDIAN_DEEP("Obsidian Deep", "Premium minimal dark slate"),
    CYBER_SUNSET("Cyber Sunset", "Vibrant pink and neon orange glow")
}

class LauncherViewModel(private val context: Context) : ViewModel() {

    // Preferences Keys
    private val DOCK_APPS_KEY = stringSetPreferencesKey("dock_apps")
    private val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme")

    // Core States
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Loaded dock app package names from DataStore
    val dockPackageNames: StateFlow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[DOCK_APPS_KEY] ?: setOf(
                "com.android.chrome",
                "com.google.android.youtube",
                "com.android.settings",
                "com.android.camera"
            )
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    // Selected wallpaper theme
    val selectedTheme: StateFlow<WallpaperTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[SELECTED_THEME_KEY] ?: WallpaperTheme.SPACE_NEBULA.name
            try {
                WallpaperTheme.valueOf(themeName)
            } catch (e: Exception) {
                WallpaperTheme.SPACE_NEBULA
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, WallpaperTheme.SPACE_NEBULA)

    // Derived list of filtered apps for workspace
    val filteredApps: StateFlow<List<AppItem>> = combine(installedApps, searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Derived list of Dock apps
    val dockApps: StateFlow<List<AppItem>> = combine(installedApps, dockPackageNames) { apps, dockPkgs ->
        // Return apps in the order of the dockPackageNames if they exist, or matching apps
        apps.filter { dockPkgs.contains(it.packageName) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Telemetry & Systems States
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isBatteryCharging = MutableStateFlow(false)
    val isBatteryCharging: StateFlow<Boolean> = _isBatteryCharging.asStateFlow()

    private val _ramUsage = MutableStateFlow(0f) // ratio 0.0f to 1.0f
    val ramUsage: StateFlow<String> = _ramUsage.map {
        String.format(Locale.US, "%.1f GB / %.1f GB", (it * getTotalRamGb()), getTotalRamGb())
    }.stateIn(viewModelScope, SharingStarted.Lazily, "0 GB / 0 GB")

    private val _ramPercentage = MutableStateFlow(0f)
    val ramPercentage: StateFlow<Float> = _ramPercentage.asStateFlow()

    // Quick toggles states (local triggers)
    val wifiEnabled = MutableStateFlow(true)
    val bluetoothEnabled = MutableStateFlow(false)
    val systemVolume = MutableStateFlow(0.6f)
    val systemBrightness = MutableStateFlow(0.7f)

    // Battery Broadcast Receiver
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    _batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                }
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                _isBatteryCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    init {
        loadInstalledApps()
        startClockUpdates()
        startTelemetryUpdates()
        registerBatteryReceiver()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val apps = resolveInfos.mapNotNull { info ->
                val packageName = info.activityInfo.packageName
                // Skip our own app to avoid recursive self-launch
                if (packageName == context.packageName) return@mapNotNull null

                val label = info.loadLabel(pm).toString()
                val activityName = info.activityInfo.name
                val isSystem = (info.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                val drawableIcon = info.loadIcon(pm)
                val imageBitmap = drawableToBitmap(drawableIcon)?.asImageBitmap()

                AppItem(
                    label = label,
                    packageName = packageName,
                    activityName = activityName,
                    iconBitmap = imageBitmap,
                    isSystemApp = isSystem
                )
            }.sortedBy { it.label.lowercase(Locale.getDefault()) }

            _installedApps.value = apps
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun startClockUpdates() {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            while (true) {
                val now = Date()
                _currentTime.value = timeFormat.format(now)
                _currentDate.value = dateFormat.format(now)
                delay(1000)
            }
        }
    }

    private fun startTelemetryUpdates() {
        viewModelScope.launch {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            while (true) {
                am.getMemoryInfo(memInfo)
                val totalMem = memInfo.totalMem.toFloat()
                val availMem = memInfo.availMem.toFloat()
                val usedMem = totalMem - availMem
                val ratio = usedMem / totalMem
                _ramUsage.value = ratio
                _ramPercentage.value = ratio
                delay(3000)
            }
        }
    }

    private fun getTotalRamGb(): Float {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024f * 1024f * 1024f)
    }

    private fun registerBatteryReceiver() {
        context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    fun launchApp(app: AppItem) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleDockApp(packageName: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                val currentDock = preferences[DOCK_APPS_KEY]?.toMutableSet() ?: mutableSetOf()
                if (currentDock.contains(packageName)) {
                    currentDock.remove(packageName)
                } else {
                    if (currentDock.size < 10) { // Limit to 10 dock apps
                        currentDock.add(packageName)
                    }
                }
                preferences[DOCK_APPS_KEY] = currentDock
            }
        }
    }

    fun updateTheme(theme: WallpaperTheme) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[SELECTED_THEME_KEY] = theme.name
            }
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Quick Settings Launchers
    fun openSettings(action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to standard settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
