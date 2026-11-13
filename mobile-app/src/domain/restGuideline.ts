import type { Pet } from '../data/models'

/**
 * 휴식 간격 기준표(초안) — 차량 앱 RestGuideline.kt 와 같은 규칙.
 * 근거: 농림축산식품부(최소 2시간마다 휴식), AKC, PetMD(체급별 노령 기준), RVC VetCompass(단두종), AAHA·PetPlace.
 * 분 단위 값은 초안이며 출시 전 수의사 자문으로 확정. 참고 안내이며 건강 상태를 측정하지 않음.
 */
export const DEFAULT_REST_MINUTES = 120

export type DogSize = '소형' | '중형' | '대형' | '초대형'

export function sizeOf(kg: number): DogSize {
  if (kg < 10) return '소형'
  if (kg < 25) return '중형'
  if (kg < 45) return '대형'
  return '초대형'
}

export const SENIOR_AGE: Record<DogSize, number> = { 소형: 10, 중형: 8, 대형: 7, 초대형: 6 }

export function ageYears(pet: Pick<Pet, 'birthYear' | 'birthMonth'>, now = new Date()): number {
  const months = (now.getFullYear() - pet.birthYear) * 12 + (now.getMonth() + 1 - pet.birthMonth)
  return Math.max(0, months) / 12
}

export interface Recommendation { minutes: number; reason: string; tip: string }

export function recommend(pet: Pet, now = new Date()): Recommendation {
  if (pet.vetRestMinutes) return { minutes: pet.vetRestMinutes, reason: '수의사가 정한 간격', tip: '수의사 안내를 가장 먼저 따라요' }
  const age = ageYears(pet, now)
  const size = sizeOf(pet.weightKg)
  const c: Recommendation[] = [{ minutes: DEFAULT_REST_MINUTES, reason: '성견 기준', tip: '배변 · 물 · 짧은 산책' }]
  if (age < 1) c.push({ minutes: age < 0.5 ? 60 : 90, reason: '어린 강아지', tip: '배변을 참을 수 있는 시간이 짧아요' })
  if (age >= SENIOR_AGE[size]) c.push({ minutes: 90, reason: `노령견(${size} ${SENIOR_AGE[size]}살~)`, tip: '오래 앉아 있으면 관절이 굳어요 · 짧은 산책' })
  if (pet.brachycephalic) c.push({ minutes: 90, reason: '단두종', tip: '더운 날 그늘에서 쉬고 에어컨 상태를 확인해요' })
  if (pet.health.some((h) => h === 'JOINT' || h === 'HEART' || h === 'RESPIRATORY')) c.push({ minutes: 90, reason: '관절·심장·호흡기 질환', tip: '무리하지 않게 자주 쉬어요' })
  if (pet.health.some((h) => h === 'KIDNEY' || h === 'DIABETES')) c.push({ minutes: 60, reason: '신장 질환·당뇨', tip: '물을 자주 마시고 배변 기회가 더 필요해요' })
  const picked = c.reduce((a, b) => (b.minutes < a.minutes ? b : a))
  return pet.customRestMinutes ? { minutes: pet.customRestMinutes, reason: '보호자 설정', tip: picked.tip } : picked
}

/** 여러 마리가 함께 타면 가장 짧은 간격 */
export function forPassengers(pets: Pet[], now = new Date()): Recommendation {
  if (pets.length === 0) return { minutes: DEFAULT_REST_MINUTES, reason: '기본 주기', tip: '배변 · 물 · 짧은 산책' }
  return pets.map((p) => recommend(p, now)).reduce((a, b) => (b.minutes < a.minutes ? b : a))
}

/** 여행 만들기 7단계 — 예상 주행 시간으로 휴식 지점을 미리 계산(실제 안내는 차량이 실제 주행 시간으로 다시 계산) */
export function plannedRestStops(totalMinutes: number, intervalMinutes: number): number[] {
  const out: number[] = []
  for (let t = intervalMinutes; t < totalMinutes; t += intervalMinutes) out.push(t)
  return out
}
