import { useState } from 'react'
import type { Trip } from '../data/models'
import { FEED, PLACES, placeMap } from '../data/samples'
import { forPassengers, plannedRestStops } from '../domain/restGuideline'
import { useNav } from '../nav'
import { setState, uid, useStore } from '../store'
import { sendTripToCar, syncConfigured } from '../sync'
import { Icon, PlaceBadges, Progress, Scene, TopBar } from '../ui'

const fmtDate = (d: string) => d.slice(5).replace('-', '.')
/** 예상 주행 시간(분) — 서버 경로 API 연결 전 샘플: 출발지(서울 기준)에서 거리 비례 */
function estimateMinutes(destId: string, waypointIds: string[]): number {
  const base = placeMap[destId]?.region === '양평' ? 80 : 160
  return base + waypointIds.length * 15
}

// ───────────────────────────── 여행 목록 (MT-01) ─────────────────────────────
export function TripsScreen() {
  const s = useStore()
  const nav = useNav()
  return (
    <>
      <TopBar title="여행" right={<button className="chip soft" onClick={() => nav.push({ name: 'tripBuilder' })}><Icon name="plus" size={16} />새 여행</button>} />
      <div className="page">
        {s.trips.length === 0 && <div className="card soft"><h3>아직 여행이 없어요</h3><p className="muted">여행을 만들어 차로 보내 보세요.</p></div>}
        <div className="stack">
          {s.trips.map((t) => {
            const d = placeMap[t.destinationId]
            return (
              <button key={t.id} className="list-item" onClick={() => nav.push({ name: 'trip', id: t.id })}>
                <Scene img={d?.image ?? 'sea'} w={72} h={72} />
                <div className="grow">
                  <h3>{t.name}</h3>
                  <div className="muted">{fmtDate(t.startDate)} – {fmtDate(t.endDate)} · {d?.name}</div>
                  <div className="badges" style={{ marginTop: 4 }}>
                    <span className={`badge ${t.sentToCar ? 'ok' : 'info'}`}>{t.sentToCar ? '차로 보냄' : '차로 보내기 전'}</span>
                    {t.fromFeedPostId && <span className="badge user">피드에서 담음</span>}
                  </div>
                </div>
                <Icon name="chevron" color="var(--tertiary)" />
              </button>
            )
          })}
        </div>
        <h2>즐겨찾기</h2>
        <div className="chips">
          {s.favorites.map((id) => placeMap[id] && <button key={id} className="chip" onClick={() => nav.push({ name: 'place', id })}>{placeMap[id].name}</button>)}
          {s.favorites.length === 0 && <span className="muted">장소 상세에서 ♡ 를 눌러 모아 보세요.</span>}
        </div>
      </div>
    </>
  )
}

// ───────────────────────────── 여행 만들기 8단계 (MT-02 · MT-03 / WF-M06·M07) ─────────────────────────────
const STEPS = ['여행명', '날짜', '출발지', '목적지', '반려견', '경유지', '휴식 장소 확인', '저장']

export function TripBuilderScreen({ fromPostId, placeId }: { fromPostId?: string; placeId?: string }) {
  const s = useStore()
  const nav = useNav()
  const post = fromPostId ? s.feed.find((f) => f.id === fromPostId) ?? FEED.find((f) => f.id === fromPostId) : undefined
  const today = new Date()
  const iso = (d: Date) => d.toISOString().slice(0, 10)
  const [step, setStep] = useState(post ? 1 : 0)
  const [t, setT] = useState<Omit<Trip, 'id' | 'createdAt' | 'sentToCar'>>({
    name: post ? post.title : placeId ? `${placeMap[placeId]?.name} 나들이` : '',
    startDate: iso(new Date(today.getTime() + 2 * 86400000)),
    endDate: iso(new Date(today.getTime() + 2 * 86400000)),
    origin: '집',
    destinationId: post ? post.stopIds[post.stopIds.length - 1] : placeId ?? '',
    waypointIds: post ? post.stopIds.slice(0, -1).slice(0, 2) : [],
    petIds: s.pets.map((p) => p.id),
    restPlaceIds: post ? post.restIds : [],
    fromFeedPostId: post?.id,
  })
  const pets = s.pets.filter((p) => t.petIds.includes(p.id))
  const rec = forPassengers(pets)
  const total = estimateMinutes(t.destinationId, t.waypointIds)
  const restAt = plannedRestStops(total, rec.minutes)
  const restAreas = PLACES.filter((p) => p.category === 'REST_AREA')
  const canNext = [t.name.trim().length > 0, t.endDate >= t.startDate, t.origin.trim().length > 0, !!t.destinationId, t.petIds.length > 0, true, true, true][step]

  const save = () => {
    const id = uid('t')
    setState((st) => ({ ...st, trips: [...st.trips, { ...t, id, createdAt: Date.now(), sentToCar: false }] }))
    nav.back()
    nav.push({ name: 'trip', id })
    nav.toast('여행을 저장했어요')
  }
  const dests = PLACES.filter((p) => p.category !== 'REST_AREA')

  return (
    <>
      <TopBar title="여행 만들기" onBack={() => (step > 0 ? setStep(step - 1) : nav.back())} />
      <div className="page no-tabs">
        <div className="row between tiny"><span>{step + 1} / 8 · {STEPS[step]}</span>{post && <span className="badge user">‘{post.title}’ 코스에서 시작</span>}</div>
        <div style={{ margin: '8px 0 16px' }}><Progress step={step + 1} total={8} /></div>

        {step === 0 && (<><h2>여행 이름을 정해 주세요</h2><label className="field"><span>여행명</span><input value={t.name} onChange={(e) => setT({ ...t, name: e.target.value })} placeholder="예: 사슴이와 강릉 바다" /></label></>)}
        {step === 1 && (<><h2>언제 떠나요?</h2>
          <label className="field"><span>출발일</span><input type="date" value={t.startDate} onChange={(e) => setT({ ...t, startDate: e.target.value, endDate: e.target.value > t.endDate ? e.target.value : t.endDate })} /></label>
          <label className="field"><span>돌아오는 날</span><input type="date" value={t.endDate} min={t.startDate} onChange={(e) => setT({ ...t, endDate: e.target.value })} /></label>
          <p className="tiny">여행은 집에 돌아와서 마무리돼요(차량 앱이 집 도착을 알아요).</p></>)}
        {step === 2 && (<><h2>어디서 출발해요?</h2>
          <div className="chips">{['집', '회사', '현재 위치'].map((o) => <button key={o} className={`chip ${t.origin === o ? 'on' : ''}`} onClick={() => setT({ ...t, origin: o })}>{o}</button>)}</div>
          <p className="tiny">‘집’은 차량 내장 내비의 즐겨찾기를 그대로 써요. 집 위치는 출발지 설정에만 써요.</p></>)}
        {step === 3 && (<><h2>목적지를 골라 주세요</h2>
          <div className="stack">{dests.map((p) => (
            <button key={p.id} className="list-item" style={{ outline: t.destinationId === p.id ? '2px solid var(--orange)' : 'none' }} onClick={() => setT({ ...t, destinationId: p.id, waypointIds: t.waypointIds.filter((w) => w !== p.id) })}>
              <Scene img={p.image} w={52} h={52} /><div className="grow"><h3 style={{ fontSize: 15 }}>{p.name}</h3><PlaceBadges p={p} max={1} /></div>
              {t.destinationId === p.id && <Icon name="check" color="var(--orange)" />}
            </button>))}</div></>)}
        {step === 4 && (<><h2>함께할 반려견</h2>
          <div className="stack">{s.pets.map((p) => {
            const on = t.petIds.includes(p.id)
            return (<button key={p.id} className="list-item" onClick={() => setT({ ...t, petIds: on ? t.petIds.filter((x) => x !== p.id) : [...t.petIds, p.id] })}>
              <div className="thumb" style={{ borderRadius: 32, background: 'var(--orange-soft)', display: 'grid', placeItems: 'center' }}><Icon name="paw" color="var(--orange)" /></div>
              <div className="grow"><h3>{p.name}</h3><div className="muted">{p.breed} · {p.weightKg}kg</div></div>
              <span className={`toggle ${on ? 'on' : ''}`} /></button>)
          })}</div>
          <p className="tiny">여러 마리가 함께 타면 가장 짧은 휴식 간격을 따라요 → {rec.minutes}분</p></>)}
        {step === 5 && (<><h2>들를 곳을 추가하세요</h2>
          <p className="muted" style={{ marginTop: -4 }}>차량 내비는 한 구간에 경유지를 최대 2곳까지 안내해요.</p>
          <div className="stack">
            {t.waypointIds.map((w) => (
              <div key={w} className="list-item"><Scene img={placeMap[w]?.image ?? 'cafe'} w={48} h={48} /><div className="grow"><h3 style={{ fontSize: 15 }}>{placeMap[w]?.name}</h3><PlaceBadges p={placeMap[w]} max={1} /></div>
                <button className="icon-btn" aria-label="빼기" onClick={() => setT({ ...t, waypointIds: t.waypointIds.filter((x) => x !== w) })}><Icon name="minus" /></button></div>
            ))}
          </div>
          {t.waypointIds.length < 2 ? (
            <div className="chips" style={{ marginTop: 12 }}>{dests.filter((p) => p.id !== t.destinationId && !t.waypointIds.includes(p.id)).map((p) => (
              <button key={p.id} className="chip" onClick={() => setT({ ...t, waypointIds: [...t.waypointIds, p.id] })}><Icon name="plus" size={14} />{p.name}</button>))}</div>
          ) : <p className="tiny">경유지 2곳이 다 찼어요. 더 들르려면 코스로 저장해 구간별로 안내받을 수 있어요.</p>}</>)}
        {step === 6 && (<><h2>쉬어 갈 곳을 확인해 주세요</h2>
          <p className="muted" style={{ marginTop: -4 }}>{pets.map((p) => p.name).join('·')} 휴식 주기 {rec.minutes}분({rec.reason}) · 예상 주행 {Math.floor(total / 60)}시간 {total % 60}분</p>
          <div className="card timeline" style={{ marginTop: 10 }}>
            <div className="tl-item"><div className="tl-dot"><Icon name="home" size={18} color="var(--orange)" /></div><div><h3>{t.origin} 출발</h3><div className="tiny">0분</div></div></div>
            {restAt.map((m, i) => {
              const pick = t.restPlaceIds[i] ?? restAreas[i % restAreas.length].id
              return (
                <div key={m} className="tl-item"><div className="tl-dot on"><Icon name="clock" size={18} color="#fff" /></div>
                  <div className="grow"><h3>휴식 {i + 1} · {placeMap[pick]?.name}</h3><div className="tiny">약 {m}분 · {placeMap[pick]?.conditions.join(' · ')}</div>
                    <button className="chip" style={{ marginTop: 6, height: 30 }} onClick={() => {
                      const next = restAreas[(restAreas.findIndex((r) => r.id === pick) + 1) % restAreas.length].id
                      const arr = [...t.restPlaceIds]; arr[i] = next; setT({ ...t, restPlaceIds: arr })
                    }}>다른 곳</button></div></div>)
            })}
            {t.waypointIds.map((w) => <div key={w} className="tl-item"><div className="tl-dot"><Icon name="pin" size={18} color="var(--orange)" /></div><div><h3>{placeMap[w]?.name}</h3><PlaceBadges p={placeMap[w]} max={1} /></div></div>)}
            <div className="tl-item"><div className="tl-dot"><Icon name="pin" size={18} color="var(--orange)" /></div><div><h3>{placeMap[t.destinationId]?.name} 도착</h3><div className="tiny">약 {Math.floor(total / 60)}시간 {total % 60}분</div></div></div>
          </div>
          <div className="card row" style={{ marginTop: 12, background: 'var(--sunken)', boxShadow: 'none' }}><Icon name="info" color="var(--sub)" /><span className="tiny">예상 시간으로 미리 본 계획이에요. 실제 휴식 안내는 차가 실제 주행 시간으로 다시 계산해요.</span></div></>)}
        {step === 7 && (<><h2>이대로 저장할까요?</h2>
          <div className="card stack">
            {[['여행명', t.name], ['날짜', `${fmtDate(t.startDate)} – ${fmtDate(t.endDate)}`], ['출발', t.origin], ['목적지', placeMap[t.destinationId]?.name], ['경유', t.waypointIds.map((w) => placeMap[w]?.name).join(' · ') || '없음'], ['반려견', pets.map((p) => p.name).join(', ')], ['휴식', `${restAt.length}회 예정 (${rec.minutes}분 주기)`]].map(([k, v]) => (
              <div key={k} className="row"><span className="tiny" style={{ width: 64 }}>{k}</span><b style={{ fontSize: 15 }}>{v}</b></div>))}
          </div></>)}
      </div>
      <div className="bottom-cta">
        {step < 7 ? <button className="btn primary" disabled={!canNext} onClick={() => setStep(step + 1)}>다음</button> : <button className="btn primary" onClick={save}>저장</button>}
      </div>
    </>
  )
}

// ───────────────────────────── 여행 상세 · 차로 보내기 (MT-04·05 / D-M05) ─────────────────────────────
export function TripDetailScreen({ id }: { id: string }) {
  const s = useStore()
  const nav = useNav()
  const t = s.trips.find((x) => x.id === id)
  const [busy, setBusy] = useState(false)
  if (!t) return <TopBar title="여행을 찾을 수 없어요" onBack={nav.back} />
  const d = placeMap[t.destinationId]
  const pets = s.pets.filter((p) => t.petIds.includes(p.id))
  const rec = forPassengers(pets)
  const total = estimateMinutes(t.destinationId, t.waypointIds)
  const send = async () => {
    if (!s.car.linked) { nav.push({ name: 'carLink' }); return }
    setBusy(true)
    const r = await sendTripToCar(t.id)
    setBusy(false)
    nav.toast(!r.ok ? r.error : r.queued ? '동기화 서버 연결 전이라 휴대폰에 보관했어요' : '차량 앱 ‘다음 여행’에 보냈어요')
  }
  return (
    <>
      <div style={{ position: 'relative' }}>
        <Scene img={d?.image ?? 'sea'} h={200} radius={0} shade>
          <h1 style={{ position: 'absolute', left: 20, bottom: 16, color: '#fff', margin: 0, fontSize: 24 }}>{t.name}</h1>
        </Scene>
        <button className="icon-btn light" style={{ position: 'absolute', top: 14, left: 14 }} onClick={nav.back} aria-label="뒤로"><Icon name="back" /></button>
      </div>
      <div className="page">
        <div className="muted">{fmtDate(t.startDate)} – {fmtDate(t.endDate)} · 함께할 반려견 {pets.map((p) => p.name).join(', ')}</div>
        <div className="card stack" style={{ marginTop: 12 }}>
          {([['home', '출발', t.origin], ['route', '경유', t.waypointIds.map((w) => placeMap[w]?.name).join(' · ') || '없음'], ['pin', '목적지', d?.name], ['clock', '휴식', `${plannedRestStops(total, rec.minutes).length}회 예정 (${rec.minutes}분 주기) · ${t.restPlaceIds.map((r) => placeMap[r]?.name).join(', ')}`]] as const).map(([ic, k, v]) => (
            <div key={k} className="row"><div className="tl-dot"><Icon name={ic} size={18} color="var(--orange)" /></div><div><div className="tiny">{k}</div><b style={{ fontSize: 15 }}>{v}</b></div></div>
          ))}
        </div>
        <button className="btn primary" style={{ marginTop: 16 }} disabled={busy} onClick={send}><Icon name="car" />{t.sentToCar ? '차로 다시 보내기' : '차로 보내기'}</button>
        <p className="tiny" style={{ textAlign: 'center' }}>
          {s.car.linked ? `같은 계정의 차량 앱 홈 ‘다음 여행’에 나타나요${syncConfigured ? '' : ' · 동기화 서버 연결 전'}` : '먼저 차량 앱과 연결해 주세요'}
        </p>
        <div className="btn-row">
          <button className="btn secondary" onClick={() => { if (!window.confirm(`‘${t.name}’ 여행을 지울까요? 되돌릴 수 없어요.`)) return; setState((st) => ({ ...st, trips: st.trips.filter((x) => x.id !== t.id) })); nav.back(); nav.toast('여행을 지웠어요') }}>지우기</button>
          <button className="btn secondary" onClick={() => nav.push({ name: 'tripBuilder', placeId: t.destinationId })}>비슷하게 새로 만들기</button>
        </div>
      </div>
    </>
  )
}
