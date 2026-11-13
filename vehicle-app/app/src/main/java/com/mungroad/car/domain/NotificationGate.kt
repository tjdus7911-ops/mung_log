package com.mungroad.car.domain

/**
 * '말해도 되는 순간' 판단 — 기획서 v4 연동 흐름 ②.
 * 알림 직전에 조건을 차례로 확인해 하나라도 해당하면 기다렸다가 재생합니다.
 * 신호가 오지 않는(미지원) 조건은 null 로 두고 건너뜁니다.
 */
data class DriverContext(
    /** 방향지시등 작동 중 (Vehicle SDK TurnSignal) */
    val turnSignalOn: Boolean? = null,
    /** 방향지시등이 꺼진 시각 */
    val turnSignalOffAtMillis: Long? = null,
    /** 다음 회전·분기까지 거리 m (NaviHelper TBT) */
    val nextTurnMeters: Int? = null,
    /** 단속·주의 구간 안 (NaviHelper camera/caution) */
    val inCautionZone: Boolean? = null,
    /** 내비 음성 안내 중(오디오 포커스를 얻지 못함) */
    val naviSpeaking: Boolean? = null,
    /** 차량 경고 표시 중 (Vehicle SDK Safety) */
    val vehicleWarning: Boolean? = null,
)

enum class Priority(val rank: Int) { SAFETY(0), REST(1), ROUTE(2) }

data class Notice(val id: String, val priority: Priority, val speech: String, val createdAtMillis: Long)

sealed class GateDecision {
    data class Speak(val notice: Notice) : GateDecision()
    data class CardOnly(val notice: Notice) : GateDecision()
    data class Wait(val reason: String) : GateDecision()
    object Idle : GateDecision()
}

class NotificationGate(
    private val turnSoonMeters: Int = 300,
    private val afterTurnSignalMillis: Long = 3_000,
    private val maxWaitMillis: Long = 3 * 60_000,
) {
    private val queue = mutableListOf<Notice>()

    fun offer(n: Notice) {
        if (queue.none { it.id == n.id }) queue += n // 중복 제거
    }

    fun clear(id: String) { queue.removeAll { it.id == id } }

    fun pending(): List<Notice> = queue.sortedWith(compareBy({ it.priority.rank }, { it.createdAtMillis }))

    fun blockingReason(ctx: DriverContext, now: Long): String? = when {
        ctx.vehicleWarning == true -> "차량 경고 표시 중"
        ctx.turnSignalOn == true -> "방향지시등 작동 중"
        ctx.turnSignalOffAtMillis != null && now - ctx.turnSignalOffAtMillis < afterTurnSignalMillis -> "차로 변경 직후"
        ctx.nextTurnMeters != null && ctx.nextTurnMeters in 0..turnSoonMeters -> "회전·분기 ${ctx.nextTurnMeters}m 전"
        ctx.inCautionZone == true -> "단속·주의 구간"
        ctx.naviSpeaking == true -> "내비 음성 안내 중"
        else -> null
    }

    /** 한 번에 하나만: 가장 우선순위가 높은 알림 1건을 꺼냅니다 */
    fun next(ctx: DriverContext, now: Long): GateDecision {
        val top = pending().firstOrNull() ?: return GateDecision.Idle
        val reason = blockingReason(ctx, now)
        if (reason == null) { queue.remove(top); return GateDecision.Speak(top) }
        if (now - top.createdAtMillis >= maxWaitMillis) { queue.remove(top); return GateDecision.CardOnly(top) }
        return GateDecision.Wait(reason)
    }
}
