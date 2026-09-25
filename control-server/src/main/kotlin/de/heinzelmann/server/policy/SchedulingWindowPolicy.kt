package de.heinzelmann.server.policy

import java.time.LocalTime

data class SchedulingWindow(
    val startHour: Int = 22,
    val endHour: Int = 6
) {
    init {
        require(startHour in 0..23) { "startHour must be between 0 and 23" }
        require(endHour in 0..23) { "endHour must be between 0 and 23" }
    }

    fun isWithinWindow(time: LocalTime): Boolean {
        return isHourWithinWindow(time.hour)
    }

    fun isHourWithinWindow(hour: Int): Boolean {
        require(hour in 0..23) { "hour must be between 0 and 23" }
        return if (startHour > endHour) {
            // Window spans midnight (e.g. 22:00 to 06:00)
            hour >= startHour || hour < endHour
        } else if (startHour < endHour) {
            // Window within same day
            hour in startHour until endHour
        } else {
            // 24-hour window if equal
            true
        }
    }
}

class SchedulingWindowPolicy(
    val window: SchedulingWindow = SchedulingWindow()
) {
    fun canDispatchBatchJob(currentTime: LocalTime = LocalTime.now()): Boolean {
        return window.isWithinWindow(currentTime)
    }

    fun canDispatchBatchJob(currentHour: Int): Boolean {
        return window.isHourWithinWindow(currentHour)
    }
}
