import { describe, expect, it } from 'vitest'
import type { Pet } from '../data/models'
import { SAMPLE_RECORDS, placeMap } from '../data/samples'
import { factsOf, localDraft, mentionsOnlyKnownPlaces } from './diary'
import { forPassengers, plannedRestStops, recommend } from './restGuideline'

const now = new Date('2026-11-13')
const pet = (o: Partial<Pet>): Pet => ({ id: 'p', name: '테스트', breed: '믹스', birthYear: 2022, birthMonth: 1, weightKg: 12, brachycephalic: false, activity: '보통', caution: '', health: ['NONE'], ...o })

describe('휴식 간격 기준표 (차량 앱 RestGuideline.kt 와 같은 결과)', () => {
  it('성견 120분', () => expect(recommend(pet({}), now).minutes).toBe(120))
  it('중형 9살 노령견 90분(사슴이)', () => expect(recommend(pet({ birthYear: 2017, birthMonth: 5 }), now).minutes).toBe(90))
  it('소형 9살은 아직 노령 아님', () => expect(recommend(pet({ birthYear: 2017, weightKg: 5 }), now).minutes).toBe(120))
  it('6개월 미만 강아지 60분', () => expect(recommend(pet({ birthYear: 2026, birthMonth: 8 }), now).minutes).toBe(60))
  it('신장 질환 60분', () => expect(recommend(pet({ health: ['KIDNEY'] }), now).minutes).toBe(60))
  it('단두종 90분', () => expect(recommend(pet({ brachycephalic: true }), now).minutes).toBe(90))
  it('수의사 간격 최우선', () => expect(recommend(pet({ vetRestMinutes: 150, health: ['KIDNEY'] }), now).minutes).toBe(150))
  it('여러 마리면 가장 짧은 간격', () => expect(forPassengers([pet({}), pet({ health: ['DIABETES'] })], now).minutes).toBe(60))
  it('예상 주행 160분, 90분 주기 → 휴식 1회', () => expect(plannedRestStops(160, 90)).toEqual([90]))
})

describe('AI 여행 일기 — 기록된 사실만 사용', () => {
  const r = SAMPLE_RECORDS[0]
  it('facts 에 휴식·방문·메모가 들어간다', () => {
    const f = factsOf(r, placeMap).map((x) => x.text)
    expect(f).toContain('옥계휴게소에서 휴식 12분')
    expect(f.some((t) => t.includes('메모'))).toBe(true)
  })
  it('로컬 초안은 기록에 없는 장소를 쓰지 않는다', () => {
    for (const pov of ['pet', 'owner'] as const) {
      const d = localDraft(r, placeMap, '사슴이', pov)
      const allowed = new Set(r.timeline.map((t) => t.placeId).filter(Boolean) as string[])
      expect(mentionsOnlyKnownPlaces(d.body, placeMap, allowed)).toBe(true)
      expect(d.body).toContain('12분')
    }
  })
  it('기록 밖 장소가 들어가면 검사에 걸린다', () => {
    expect(mentionsOnlyKnownPlaces('솔밭 공원에 갔어', placeMap, new Set(['p-cafe']))).toBe(false)
  })
})
