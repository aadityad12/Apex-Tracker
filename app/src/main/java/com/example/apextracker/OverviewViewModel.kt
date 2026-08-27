package com.example.apextracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayOverview(
    val date: LocalDate,
    val pendingReminders: List<Reminder>,
    val completedReminders: List<Reminder>,
    val missedReminders: List<Reminder>,
    val totalSpent: Double,
    val screenTimeMinutes: Long,
    val studyTimeMinutes: Long
)

class OverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val reminderDao = db.reminderDao()
    private val budgetDao = db.budgetDao()
    private val studyDao = db.studySessionDao()
    private val screenTimeDao = db.screenTimeSessionDao()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    init {
        // Issue #209: Room's today row for screen time is only as fresh as ScreenTimeViewModel's
        // last 30s-loop write, which only runs while that screen has been open recently — so
        // Overview, often the first screen opened, could read 0m/stale on a day Screen Time
        // hasn't been visited yet. Freshen it once on load; dayOverview picks up the write
        // automatically since getAllSessions() is a Flow. No-op without Usage Access.
        refreshTodayScreenTimeIfToday(LocalDate.now())
    }

    private fun refreshTodayScreenTimeIfToday(date: LocalDate) {
        if (date != LocalDate.now()) return
        viewModelScope.launch {
            refreshTodayScreenTime(db, getApplication())
        }
    }

    val dayOverview: StateFlow<DayOverview?> = _selectedDate.flatMapLatest { date ->
        combine(
            reminderDao.getActiveReminders(),
            reminderDao.getCompletedReminders(),
            budgetDao.getAllItems(),
            studyDao.getAllSessions(),
            screenTimeDao.getAllSessions()
        ) { activeRem, compRem, budgetItems, studySessions, screenSessions ->
            
            val dayActive = activeRem.filter { it.date == date }
            val dayCompleted = compRem.filter { it.date == date }
            
            val missed = if (date < LocalDate.now()) {
                activeRem.filter { it.date == date }
            } else if (date == LocalDate.now()) {
                // For today, "missed" are active reminders from previous days
                activeRem.filter { it.date < date }
            } else {
                emptyList()
            }

            // Income isn't spend (Issue #218).
            val spent = budgetItems.expensesOnly().filter { it.date == date }.sumOf { it.amount }
            val study = studySessions.filter { it.date == date }.sumOf { it.durationSeconds } / 60
            val screen = (screenSessions.find { it.date == date }?.durationMillis ?: 0L) / 60000

            DayOverview(
                date = date,
                pendingReminders = dayActive,
                completedReminders = dayCompleted,
                missedReminders = missed,
                totalSpent = spent,
                screenTimeMinutes = screen,
                studyTimeMinutes = study
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        refreshTodayScreenTimeIfToday(date)
    }
}
