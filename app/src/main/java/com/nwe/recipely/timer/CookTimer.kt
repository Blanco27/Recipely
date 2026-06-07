package com.nwe.recipely.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Snapshot of the single active cook timer. */
data class TimerUiState(
    val stepNumber: Int,       // 1-based step the timer belongs to
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val running: Boolean,
)

/** Process-global handle to the active timer; the cook-mode UI observes [state]. */
object CookTimer {
    private val _state = MutableStateFlow<TimerUiState?>(null)
    val state: StateFlow<TimerUiState?> = _state.asStateFlow()

    /** Called only by [TimerService]. */
    internal fun publish(state: TimerUiState?) { _state.value = state }
}
