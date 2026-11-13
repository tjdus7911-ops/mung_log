package com.mungroad.car.domain

import com.mungroad.car.data.Place
import com.mungroad.car.data.Verification

data class RestCandidate(val place: Place, val aheadKm: Double, val minutes: Int, val reason: String)

/**
 * 경로 앞 휴식 장소 2~3곳 제안.
 * 정렬: (연료·배터리 부족 시) 주유·충전 가능 → 확인됨 → 반려견 놀이터 → 이용자 확인 수(3차) → 가까운 순
 */
object RestCandidates {
    fun pick(
        ahead: List<Pair<Place, Double>>,
        fuelLow: Boolean,
        evLow: Boolean,
        avgKmh: Double = 80.0,
        max: Int = 3,
        minAheadKm: Double = 2.0,
        maxAheadKm: Double = 60.0,
    ): List<RestCandidate> {
        return ahead
            .filter { (_, km) -> km in minAheadKm..maxAheadKm }
            .sortedWith(
                compareByDescending<Pair<Place, Double>> { (p, _) -> (fuelLow && p.hasFuel) || (evLow && p.hasEvCharger) }
                    .thenByDescending { (p, _) -> p.verification == Verification.VERIFIED }
                    .thenByDescending { (p, _) -> p.hasDogPlayground }
                    .thenByDescending { (p, _) -> p.userChecks >= 5 }
                    .thenBy { (_, km) -> km },
            )
            .take(max)
            .map { (p, km) ->
                val reason = buildList {
                    if (fuelLow && p.hasFuel) add("주유 가능")
                    if (evLow && p.hasEvCharger) add("충전 가능")
                    if (p.hasDogPlayground) add("반려견 놀이터")
                    if (p.verification == Verification.CHECK_NEEDED) add("방문 전 확인 필요")
                }.joinToString(" · ")
                RestCandidate(p, km, ((km / avgKmh) * 60).toInt().coerceAtLeast(1), reason)
            }
    }
}
