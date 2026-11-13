package com.mungroad.car.domain

/**
 * 반려견 휴식 안내 판단.
 * - 주기 10분 전 예고 1회, 도달 시 안내 1회
 * - '나중에'를 고르면 정해 둔 시간(기본 15분) 뒤 1회만 다시 안내
 */
class RestEngine(var intervalMinutes: Int, private val preNoticeMinutes: Int = 10, private val snoozeMinutes: Int = 15) {
    sealed class Event {
        data class PreNotice(val minutesLeft: Int) : Event()
        object Due : Event()
        object Reminder : Event()
    }

    private var preNoticed = false
    private var dueNotified = false
    private var snoozedAtMillis: Long? = null
    private var reminded = false

    fun check(sinceRestMillis: Long): Event? {
        val interval = intervalMinutes * 60_000L
        val pre = (intervalMinutes - preNoticeMinutes).coerceAtLeast(0) * 60_000L
        snoozedAtMillis?.let { snoozedAt ->
            if (!reminded && sinceRestMillis - snoozedAt >= snoozeMinutes * 60_000L) {
                reminded = true
                return Event.Reminder
            }
            return null
        }
        if (!dueNotified && sinceRestMillis >= interval) {
            dueNotified = true; preNoticed = true
            return Event.Due
        }
        if (!preNoticed && preNoticeMinutes > 0 && sinceRestMillis >= pre && sinceRestMillis < interval) {
            preNoticed = true
            return Event.PreNotice(((interval - sinceRestMillis) / 60_000L).toInt().coerceAtLeast(1))
        }
        return null
    }

    fun snooze(sinceRestMillis: Long) { snoozedAtMillis = sinceRestMillis }

    /** 휴식을 마치면 다음 주기를 처음부터 */
    fun reset() { preNoticed = false; dueNotified = false; snoozedAtMillis = null; reminded = false }
}
