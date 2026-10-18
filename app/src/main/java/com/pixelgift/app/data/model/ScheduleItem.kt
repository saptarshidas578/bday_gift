package com.pixelgift.app.data.model

import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolConstants

/**
 * Model representing a scheduled rule stored in device slots (0..7) per SPEC 7.4.
 */
data class ScheduleItem(
    val slot: Int,
    val enabled: Boolean,
    val dayMask: Int, // Bit 0: Mon, Bit 1: Tue, ..., Bit 6: Sun
    val hour: Int,    // 0..23
    val minute: Int,  // 0..59
    val action: Int,  // 0: ON, 1: OFF, 2: LOAD_SCENE, 3: SUNRISE_ALARM
    val arg: Int      // scene slot (0..7) or sunrise duration in minutes
) {
    init {
        require(slot in 0..7) { "Schedule slot must be 0..7, got $slot" }
        require(hour in 0..23) { "Hour must be 0..23, got $hour" }
        require(minute in 0..59) { "Minute must be 0..59, got $minute" }
        require(action in 0..3) { "Action must be 0..3, got $action" }
    }

    fun toCommand(): Command.SetSchedule = Command.SetSchedule(
        slot = slot,
        enabled = enabled,
        dayMask = dayMask,
        hour = hour,
        minute = minute,
        action = action,
        arg = arg
    )

    val formattedTime: String
        get() = "%02d:%02d".format(hour, minute)

    val actionName: String
        get() = when (action) {
            ProtocolConstants.SCHEDULE_ACTION_ON -> "Turn Matrix ON"
            ProtocolConstants.SCHEDULE_ACTION_OFF -> "Turn Matrix OFF"
            ProtocolConstants.SCHEDULE_ACTION_LOAD_SCENE -> "Load Scene $arg"
            ProtocolConstants.SCHEDULE_ACTION_SUNRISE_ALARM -> "Sunrise Alarm (${arg}m)"
            else -> "Action $action"
        }

    val daysSummary: String
        get() {
            if (dayMask == 0) return "Never"
            if (dayMask == 0x7F) return "Every day"
            if (dayMask == 0x1F) return "Weekdays"
            if (dayMask == 0x60) return "Weekends"
            val dayNames = listOf("M", "Tu", "W", "Th", "F", "Sa", "Su")
            return dayNames.filterIndexed { index, _ -> (dayMask and (1 shl index)) != 0 }.joinToString(" ")
        }

    companion object {
        // Weekdays: Mon(1) | Tue(2) | Wed(4) | Thu(8) | Fri(16) = 31 (0x1F)
        const val MASK_WEEKDAYS = 0x1F
        // Weekends: Sat(32) | Sun(64) = 96 (0x60)
        const val MASK_WEEKENDS = 0x60
        // All days: 0x7F
        const val MASK_ALL = 0x7F

        val DEFAULT_SCHEDULES = listOf(
            ScheduleItem(slot = 0, enabled = false, dayMask = MASK_WEEKDAYS, hour = 7, minute = 30, action = ProtocolConstants.SCHEDULE_ACTION_SUNRISE_ALARM, arg = 15),
            ScheduleItem(slot = 1, enabled = false, dayMask = MASK_ALL, hour = 23, minute = 0, action = ProtocolConstants.SCHEDULE_ACTION_OFF, arg = 0),
            ScheduleItem(slot = 2, enabled = false, dayMask = MASK_WEEKENDS, hour = 9, minute = 0, action = ProtocolConstants.SCHEDULE_ACTION_LOAD_SCENE, arg = 0),
            ScheduleItem(slot = 3, enabled = false, dayMask = MASK_WEEKDAYS, hour = 18, minute = 0, action = ProtocolConstants.SCHEDULE_ACTION_ON, arg = 0)
        )
    }
}
