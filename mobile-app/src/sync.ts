import { getState, setState } from './store'

/**
 * 차량 앱과의 계정 동기화(2차). Pleos SDK 는 쓰지 않고 서버를 거칩니다(docs/SYNC_API.md).
 * VITE_MUNGROAD_API_BASE_URL 이 없으면 보낼 항목을 기기 안 보낼함(outbox)에 쌓아 두고
 * '서버 연결 전' 상태로 표시합니다 — 보낸 것처럼 꾸미지 않습니다.
 */
const BASE = (import.meta.env.VITE_MUNGROAD_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ?? ''

export const syncConfigured = BASE.length > 0

export type SyncResult = { ok: true; queued: false } | { ok: true; queued: true } | { ok: false; error: string }

async function post(path: string, body: unknown): Promise<SyncResult> {
  if (!BASE) return { ok: true, queued: true }
  try {
    const res = await fetch(`${BASE}${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body), credentials: 'include' })
    return res.ok ? { ok: true, queued: false } : { ok: false, error: `서버 응답 ${res.status}` }
  } catch (e) {
    return { ok: false, error: '네트워크에 연결할 수 없어요' }
  }
}

function enqueue(kind: string, id: string) {
  setState((s) => ({ ...s, outbox: [...s.outbox.filter((o) => !(o.kind === kind && o.id === id)), { kind, id, at: Date.now() }] }))
}

/** 여행 → 차량 앱 '다음 여행' */
export async function sendTripToCar(tripId: string): Promise<SyncResult> {
  const trip = getState().trips.find((t) => t.id === tripId)
  if (!trip) return { ok: false, error: '여행을 찾을 수 없어요' }
  const r = await post('/api/v1/trips', trip)
  if (r.ok && r.queued) enqueue('trip', tripId)
  if (r.ok) setState((s) => ({ ...s, trips: s.trips.map((t) => (t.id === tripId ? { ...t, sentToCar: true } : t)) }))
  return r
}

/** 반려견 프로필 → 차량 앱. 건강 상태는 보내지 않고 계산된 휴식 주기만 보냅니다 */
export async function syncPet(petId: string, restMinutes: number): Promise<SyncResult> {
  const pet = getState().pets.find((p) => p.id === petId)
  if (!pet) return { ok: false, error: '반려견을 찾을 수 없어요' }
  const { health: _omit, ...shareable } = pet
  void _omit
  const r = await post('/api/v1/pets', { ...shareable, restMinutes })
  if (r.ok && r.queued) enqueue('pet', petId)
  return r
}

export async function postMungchelin(placeId: string, payload: unknown): Promise<SyncResult> {
  const r = await post(`/api/v1/places/${placeId}/mungchelin`, payload)
  if (r.ok && r.queued) enqueue('mungchelin', placeId)
  return r
}

export async function postFeed(postId: string, payload: unknown): Promise<SyncResult> {
  const r = await post('/api/v1/feed', payload)
  if (r.ok && r.queued) enqueue('feed', postId)
  return r
}

/** AI 여행 일기 — 서버 LLM 에 기록된 사실만 보냄. 서버가 없으면 null → 로컬 초안 */
export async function requestDiary(facts: string[], pov: 'pet' | 'owner', petName: string): Promise<{ title: string; body: string } | null> {
  if (!BASE) return null
  try {
    const res = await fetch(`${BASE}/api/v1/diary`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ facts, pov, petName }) })
    if (!res.ok) return null
    const j = await res.json()
    return typeof j?.title === 'string' && typeof j?.body === 'string' ? j : null
  } catch {
    return null
  }
}
