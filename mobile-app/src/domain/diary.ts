import type { Place, TripRecord } from '../data/models'

/**
 * AI 여행 일기 — 기록된 사실만으로 초안을 만듭니다.
 * 원칙: 기록에 없는 장소·사건을 만들지 않고, 반려견의 건강·기분을 단정하지 않음(보호자 메모만 반영).
 *
 * 실서비스: 서버 LLM 에 아래 facts 만 입력으로 주고 결과 형식을 검사합니다(DIARY_API).
 * 서버가 없을 때는 같은 facts 로 문장을 이어 붙이는 로컬 초안을 씁니다(지어내는 문장 없음).
 */
export interface DiaryFact { kind: 'rest' | 'visit' | 'memo' | 'drive'; text: string }

export function factsOf(r: TripRecord, places: Record<string, Place>): DiaryFact[] {
  const f: DiaryFact[] = [{ kind: 'drive', text: `주행 ${Math.floor(r.drivingMinutes / 60)}시간 ${r.drivingMinutes % 60}분 · ${r.distanceKm}km` }]
  for (const t of r.timeline) {
    const name = t.placeId ? places[t.placeId]?.name ?? t.label : t.label
    if (t.kind === 'rest') f.push({ kind: 'rest', text: `${name}에서 휴식 ${t.minutes ?? 0}분` })
    if (t.kind === 'visit') f.push({ kind: 'visit', text: `${name}${t.photos.length ? ` · 사진 ${t.photos.length}장` : ''}` })
  }
  for (const m of r.memo) f.push({ kind: 'memo', text: `메모 “${m}”` })
  return f
}

export function localDraft(r: TripRecord, places: Record<string, Place>, petName: string, pov: 'pet' | 'owner'): { title: string; body: string } {
  const rests = r.timeline.filter((t) => t.kind === 'rest')
  const visits = r.timeline.filter((t) => t.kind === 'visit')
  const nameOf = (t: TripRecord['timeline'][number]) => (t.placeId ? places[t.placeId]?.name ?? t.label : t.label)
  const lines: string[] = []
  const subj = pov === 'pet' ? '나는' : `${petName}와 나는`
  lines.push(`${subj} 차를 타고 ${Math.floor(r.drivingMinutes / 60)}시간 ${r.drivingMinutes % 60}분 동안 ${r.distanceKm}km를 달렸어${pov === 'owner' ? '요' : ''}.`)
  for (const t of rests) lines.push(`${nameOf(t)}에서 ${t.minutes ?? 0}분 쉬었어${pov === 'owner' ? '요' : ''}.`)
  if (visits.length) lines.push(`${visits.map(nameOf).join(', ')}에 들렀어${pov === 'owner' ? '요' : ''}.`)
  for (const m of r.memo) lines.push(pov === 'owner' ? `메모: ${m}.` : `${m}.`)
  lines.push(pov === 'owner' ? '다음엔 또 어디로 갈까요?' : '다음엔 또 어디로 갈까?')
  const first = visits[0] ? nameOf(visits[0]) : r.tripName
  return { title: `${r.dateLabel.split('–')[0].trim()}, ${first}`, body: lines.join(' ') }
}

/** 생성된 일기가 facts 밖의 장소명을 쓰지 않았는지 검사(서버 LLM 결과 검증에도 사용) */
export function mentionsOnlyKnownPlaces(body: string, places: Record<string, Place>, allowed: Set<string>): boolean {
  return Object.values(places).every((p) => !body.includes(p.name) || allowed.has(p.id))
}
