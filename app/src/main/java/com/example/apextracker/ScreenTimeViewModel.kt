package com.example.apextracker

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apextracker.widget.refreshTodayWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isExcluded: Boolean,
    val usageTimeMillis: Long = 0L,
    // Per-app daily budget in minutes, or null if uncapped (Issue #124).
    val limitMinutes: Int? = null
) {
    val isOverLimit: Boolean
        get() = limitMinutes != null && isOverLimit(usageTimeMillis, limitMinutes)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenTimeViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val TAG = "ScreenTimeViewModel"
    }

    private val database = AppDatabase.getDatabase(application)
    private val screenTimeDao = database.screenTimeSessionDao()
    private val excludedAppDao = database.excludedAppDao()
    private val appUsageLimitDao = database.appUsageLimitDao()
    private val firebaseManager = FirebaseManager(application)

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _todayScreenTimeMillis = MutableStateFlow(0L)
    val todayScreenTimeMillis: StateFlow<Long> = _todayScreenTimeMillis.asStateFlow()

    private val _aggregatedUsage = MutableStateFlow<List<DeviceSession>>(emptyList())
    val aggregatedUsage: StateFlow<List<DeviceSession>> = _aggregatedUsage.asStateFlow()

    // Updated by the live cross-device listener (arbitrary cadence); recombined with the
    // freshly-measured self value on every 30s tick in refreshAggregatedUsage(). Each side
    // has exactly one writer, so there's no race — worst case is a few hundred ms staleness
    // on one side, never a dropped entry.
    private var latestOtherDevices: List<DeviceSession> = emptyList()

    private val _excludedApps = excludedAppDao.getExcludedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _limits = appUsageLimitDao.getLimits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val installedApps: StateFlow<List<AppUsageInfo>> = combine(_installedApps, _excludedApps, _limits, _todayScreenTimeMillis) { installed, excluded, limits, _ ->
        val currentStats = calculateAppSpecificUsage()
        val limitByPackage = limits.associate { it.packageName to it.dailyLimitMinutes }
        installed.map { app ->
            app.copy(
                isExcluded = excluded.any { it.packageName == app.packageName },
                usageTimeMillis = currentStats[app.packageName] ?: 0L,
                limitMinutes = limitByPackage[app.packageName]
            )
        }.sortedByDescending { it.usageTimeMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Sets (or updates) a per-app daily limit; a non-positive value clears it (Issue #124). */
    fun setAppLimit(app: AppUsageInfo, minutes: Int?) {
        viewModelScope.launch {
            if (minutes == null || minutes <= 0) {
                appUsageLimitDao.clearLimit(app.packageName)
            } else {
                // Preserve lastNotifiedDate on edit so raising a limit mid-day doesn't re-alert; a
                // genuinely new/lowered limit that's already exceeded will alert on the next poll
                // because today's usage still clears the bar and the guard only blocks same-day
                // repeats for the *existing* record.
                val existing = appUsageLimitDao.getLimit(app.packageName)
                appUsageLimitDao.setLimit(
                    AppUsageLimit(
                        packageName = app.packageName,
                        dailyLimitMinutes = minutes,
                        lastNotifiedDate = existing?.lastNotifiedDate
                    )
                )
            }
        }
    }

    init {
        checkPermission()
        loadInstalledApps()
        startScreenTimeUpdates()
        viewModelScope.launch {
            // flatMapLatest, not a one-shot uid check: an account switch must cancel the old
            // account's listener and start a fresh one for the new uid, or the old account's
            // cross-device data keeps streaming into this ViewModel's state (Issue #230).
            firebaseManager.userIdFlow().flatMapLatest { uid ->
                // Clear immediately on any uid change so the old account's data can't linger
                // on screen even for the brief window before the new listener's first snapshot.
                latestOtherDevices = emptyList()
                refreshAggregatedUsage(_todayScreenTimeMillis.value)
                if (uid != null) firebaseManager.getOtherDevicesScreenTimeFlow() else flowOf(emptyList())
            }.collect { others ->
                latestOtherDevices = others
                refreshAggregatedUsage(_todayScreenTimeMillis.value)
            }
        }
    }

    fun checkPermission() {
        // Delegates to the stateless check in ScreenTimeRefresh.kt (Issue #209) so Overview's
        // refresh path and this one can't drift into two different definitions of "granted".
        _hasPermission.value = hasUsageAccess(getApplication())
    }

    fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            // On API 30+ the <queries> LAUNCHER element in the manifest already limits what this
            // returns to launchable packages (Issue #72 — it replaced QUERY_ALL_PACKAGES). Below
            // API 30 package visibility doesn't exist and this returns every installed package,
            // so this filter is still what bounds the list on API 26-29. Keep it.
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null
                    !isSystemApp || hasLauncher
                }
                .map { app ->
                    AppUsageInfo(
                        packageName = app.packageName,
                        appName = pm.getApplicationLabel(app).toString(),
                        icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null },
                        isExcluded = false
                    )
                }
                .distinctBy { it.packageName }
            _installedApps.value = apps
        }
    }

    fun toggleAppExclusion(app: AppUsageInfo) {
        viewModelScope.launch {
            if (app.isExcluded) {
                excludedAppDao.includeApp(ExcludedApp(app.packageName))
                safeCloudCall(TAG, "remove excluded app") {
                    firebaseManager.removeExcludedApp(app.packageName)
                }
            } else {
                excludedAppDao.excludeApp(ExcludedApp(app.packageName))
                safeCloudCall(TAG, "push excluded app") {
                    firebaseManager.pushExcludedApp(app.packageName)
                }
            }
            updateScreenTime()
        }
    }

    private fun startScreenTimeUpdates() {
        // 30s interval to avoid spamming Firestore
        viewModelScope.launchPeriodic(30_000) {
            if (_hasPermission.value) {
                updateScreenTime()
            }
        }
    }

    private suspend fun updateScreenTime() {
        val usageMap = calculateAppSpecificUsage()
        val excludedPackageNames = _excludedApps.value.map { it.packageName }.toSet()
        val myPackageName = getApplication<Application>().packageName

        // Delegates to the shared resolution in ScreenTimeRefresh.kt (Issue #228) so this screen
        // and Overview's refresh path can't drift into two different, differently-buggy notions
        // of "the launcher."
        val homePackages = resolveHomePackages(getApplication())

        // Restrict to the same package set the itemized "Today's Apps" list is built from
        // (installedApps, filtered in loadInstalledApps() to !isSystemApp || hasLauncher) so the
        // headline total can never outrun what the list can show — a launcher-less system package
        // (a background service, IME, permission controller, ...) used to count toward the total
        // while being unrepresentable in the list (Issue #159). Empty means installedApps hasn't
        // loaded yet (a brief window at cold start); fall back to the old unrestricted filter
        // rather than reporting a false zero for that one tick.
        val trackablePackages = _installedApps.value.map { it.packageName }.toSet()

        val totalFilteredTime = filterTrackableUsage(
            usageMap, excludedPackageNames, myPackageName, homePackages, trackablePackages
        )

        _todayScreenTimeMillis.value = totalFilteredTime
        saveTodayScreenTime(totalFilteredTime)

        // Upload to Firebase if logged in
        if (firebaseManager.userId != null) {
            safeCloudCall(TAG, "upload screen time") {
                firebaseManager.uploadScreenTimeSession(ScreenTimeSession(date = LocalDate.now(), durationMillis = totalFilteredTime))
            }
        }
        checkAppLimits(usageMap)
        refreshAggregatedUsage(totalFilteredTime)
    }

    /**
     * Alerts once per day for each app that has crossed its per-app limit (Issue #124). Reads the
     * DAO directly (not the throttled _limits flow) so a limit set moments ago is seen immediately,
     * and stamps lastNotifiedDate so the 30s loop doesn't re-alert until tomorrow.
     */
    private suspend fun checkAppLimits(usageMap: Map<String, Long>) {
        val today = LocalDate.now().toString()
        val limits = appUsageLimitDao.getLimitsOneShot()
        val nameByPackage = _installedApps.value.associate { it.packageName to it.appName }
        limitsToNotify(usageMap, limits, today).forEach { limit ->
            val appName = nameByPackage[limit.packageName] ?: limit.packageName
            postAppLimitNotification(getApplication(), limit.packageName, appName, limit.dailyLimitMinutes)
            appUsageLimitDao.markNotified(limit.packageName, today)
        }
    }

    private fun refreshAggregatedUsage(currentDeviceMillis: Long) {
        val currentDevice = DeviceSession(
            deviceId = firebaseManager.deviceId,
            deviceName = Build.MODEL,
            date = LocalDate.now().toString(),
            durationMillis = currentDeviceMillis,
            isCurrentDevice = true
        )
        _aggregatedUsage.value = listOf(currentDevice) + latestOtherDevices
    }

    // Delegates to the shared query in ScreenTimeRefresh.kt (Issue #209) so Overview's refresh
    // path runs the exact same UsageStatsManager query and event aggregation as this screen.
    private suspend fun calculateAppSpecificUsage(): Map<String, Long> =
        calculateTodayAppUsage(getApplication())

    private suspend fun saveTodayScreenTime(millis: Long) {
        val today = LocalDate.now()
        screenTimeDao.insertSession(ScreenTimeSession(date = today, durationMillis = millis))
        // The at-a-glance widget (Issue #44) reads this row, so it has to follow the 30s poll —
        // but only when the figure it renders has actually moved. Usage is measured in
        // milliseconds and displayed in whole minutes, so most polls change nothing worth
        // rebuilding a launcher view for.
        val minutes = millis / 60_000
        if (minutes != lastWidgetMinutes || today != lastWidgetDate) {
            lastWidgetMinutes = minutes
            lastWidgetDate = today
            refreshTodayWidget(getApplication())
        }
    }

    private var lastWidgetMinutes: Long = -1L
    private var lastWidgetDate: LocalDate? = null

    fun getAllSessions() = screenTimeDao.getAllSessions()
}
