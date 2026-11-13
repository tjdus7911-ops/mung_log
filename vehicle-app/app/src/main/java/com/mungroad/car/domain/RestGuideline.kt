package com.mungroad.car.domain

import com.mungroad.car.data.Health
import com.mungroad.car.data.Pet

/**
 * 휴식 간격 기준표(초안) — 기획서 v4 '드라이브 모드와 반려견 맞춤 휴식 추천' 표.
 * 근거: 농림축산식품부(장거리 이동 시 최소 2시간마다 휴식), AKC(강아지 배변·노령견), PetMD(체급별 노령 기준),
 * RVC VetCompass(단두종 열사병 위험), AAHA·PetPlace(당뇨·신장 질환).
 * 분 단위 값은 멍로드가 정한 초안이며 출시 전 수의사 자문으로 확정합니다.
 * 참고 안내이며 차량이 반려견의 피로도·건강 상태를 측정하지 않습니다.
 */
object RestGuideline {
    const val DEFAULT_MINUTES = 120

    data class Recommendation(val minutes: Int, val reason: String, val tip: String)

    fun recommend(pet: Pet): Recommendation {
        pet.vetRestMinutes?.let { return Recommendation(it, "수의사가 정한 간격", "수의사 안내를 가장 먼저 따라요") }
        val candidates = mutableListOf(Recommendation(DEFAULT_MINUTES, "성견 기준", "배변 · 물 · 짧은 산책"))
        if (pet.ageYears < 1.0) {
            // 월령이 낮을수록 짧게: 6개월 미만 60분, 그 외 90분
            val m = if (pet.ageYears < 0.5) 60 else 90
            candidates += Recommendation(m, "어린 강아지", "배변을 참을 수 있는 시간이 짧아요")
        }
        if (pet.ageYears >= pet.size.seniorAge) {
            candidates += Recommendation(90, "노령견(${pet.size.label} ${pet.size.seniorAge}살~)", "오래 앉아 있으면 관절이 굳어요 · 짧은 산책")
        }
        if (pet.brachycephalic) {
            candidates += Recommendation(90, "단두종", "더운 날 그늘에서 쉬고 에어컨 상태를 확인해요")
        }
        if (pet.health.any { it == Health.JOINT || it == Health.HEART || it == Health.RESPIRATORY }) {
            candidates += Recommendation(90, "관절·심장·호흡기 질환", "무리하지 않게 자주 쉬어요")
        }
        if (pet.health.any { it == Health.KIDNEY || it == Health.DIABETES }) {
            candidates += Recommendation(60, "신장 질환·당뇨", "물을 자주 마시고 배변 기회가 더 필요해요")
        }
        val picked = candidates.minBy { it.minutes }
        // 보호자는 추천값보다 짧게만 바꿀 수 있는 것이 아니라 자유롭게 바꿀 수 있음(설정에서 직접 입력)
        return pet.customRestMinutes?.let { Recommendation(it, "보호자 설정", picked.tip) } ?: picked
    }

    /** 여러 마리가 함께 타면 가장 짧은 간격 */
    fun forPassengers(pets: List<Pet>): Recommendation =
        if (pets.isEmpty()) Recommendation(DEFAULT_MINUTES, "기본 주기", "배변 · 물 · 짧은 산책")
        else pets.map { recommend(it) }.minBy { it.minutes }
}
