package com.example.apextracker

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ApexTipUiState(
    val loaded: Boolean = false,
    val enabled: Boolean = false,
    val loading: Boolean = false,
    val text: String? = null,
    val failed: Boolean = false
)

class ApexTipViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = ApexTipSettings(application)
    private val budgetPrefs = BudgetPrefs(application)
    private val generator = FirebaseApexTipGenerator()

    private val _uiState = MutableStateFlow(ApexTipUiState())
    val uiState: StateFlow<ApexTipUiState> = _uiState.asStateFlow()

    private var preferences: ApexTipPreferences? = null
    private var latestSnapshot: ApexTipSnapshot? = null
    private var generationInFlight = false

    init {
        viewModelScope.launch {
            preferences = settings.preferences.first()
            renderStoredState()
            generateIfNeeded()
        }
    }

    fun updateSnapshot(snapshot: ApexTipSnapshot) {
        latestSnapshot = snapshot
        if (preferences != null) generateIfNeeded()
    }

    fun setEnabled(enabled: Boolean) {
        val current = preferences ?: return
        preferences = current.copy(enabled = enabled)
        renderStoredState()
        viewModelScope.launch {
            settings.setEnabled(enabled)
            if (enabled) generateIfNeeded()
        }
    }

    fun retry() = generateIfNeeded(forceRetry = true)

    private fun renderStoredState() {
        val prefs = preferences ?: return
        val date = latestSnapshot?.date
        val currentText = prefs.tipText?.takeIf { prefs.tipDate == date }
        _uiState.value = ApexTipUiState(
            loaded = true,
            enabled = prefs.enabled,
            text = currentText,
            failed = prefs.enabled && date != null && currentText == null && prefs.lastAttemptDate == date
        )
    }

    private fun generateIfNeeded(forceRetry: Boolean = false) {
        val prefs = preferences ?: return
        val snapshot = latestSnapshot ?: return
        if (generationInFlight || !shouldGenerateApexTip(
                enabled = prefs.enabled,
                date = snapshot.date,
                cachedDate = prefs.tipDate,
                lastAttemptDate = prefs.lastAttemptDate,
                forceRetry = forceRetry
            )) return

        generationInFlight = true
        preferences = prefs.copy(lastAttemptDate = snapshot.date)
        _uiState.value = ApexTipUiState(loaded = true, enabled = true, loading = true)

        viewModelScope.launch {
            try {
                settings.markAttempt(snapshot.date)
                val enriched = snapshot.copy(
                    monthlyBudgetLimit = budgetPrefs.overallMonthlyLimit.first()
                )
                val tip = normalizeApexTip(generator.generate(enriched))
                    ?: error("Gemini returned no text")
                settings.saveTip(snapshot.date, tip)
                preferences = preferences?.copy(tipDate = snapshot.date, tipText = tip)
                renderStoredState()
            } catch (error: Exception) {
                Log.w(TAG, "Daily Apex Tip generation failed", error)
                renderStoredState()
            } finally {
                generationInFlight = false
            }
        }
    }

    private companion object {
        const val TAG = "ApexTipViewModel"
    }
}
