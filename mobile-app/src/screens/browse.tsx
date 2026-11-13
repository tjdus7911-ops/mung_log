import { useState } from 'react'
import { CATEGORY_LABEL, type Category, type Place } from '../data/models'
import { PLACES, placeMap } from '../data/samples'
import { recommend } from '../domain/restGuideline'
import { useNav } from '../nav'
import { setState, useStore } from '../store'
import { syncConfigured } from '../sync'
import { Icon, PlaceBadges, Scene, TopBar } from '../ui'
import { homeBackground, petFace } from '../photos'
import './home.css'

const BROWSABLE = PLACES.filter((p) => p.category !== 'REST_AREA')

function FavButton({ id }: { id: string }) {
  const s = useStore()
  const on = s.favorites.includes(id)
  return (
    <button className="icon-btn" aria-label="즐겨찾기" onClick={(e) => {
      e.stopPropagation()
      setState((st) => ({ ...st, favorites: on ? st.favorites.filter((f) => f !== id) : [...st.favorites, id] }))
    }}>
      <Icon name={on ? 'heartFill' : 'heart'} color={on ? 'var(--orange)' : 'var(--tertiary)'} />
    </button>
  )
}

export function PlaceItem({ p }: { p: Place }) {
  const nav = useNav()
  return (
    <button className="list-item" onClick={() => nav.push({ name: 'place', id: p.id })}>
      <Scene img={p.image} w={64} h={64} />
      <div className="grow">
        <h3 style={{ fontSize: 15 }}>{p.name}</h3>
        <div className="muted" style={{ margin: '2px 0 4px' }}>{p.region} · {CATEGORY_LABEL[p.category]}</div>
        <PlaceBadges p={p} max={1} showUser />
      </div>
      <FavButton id={p.id} />
    </button>
  )
}

// ───────────────────────────── 홈 (WF-M03 · 최종 시안 2026-11-13 블루 글래스) ─────────────────────────────
const todayStr = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const md = (d: string) => `${Number(d.slice(5, 7))}.${Number(d.slice(8, 10))}`

export function HomeScreen() {
  const s = useStore()
  const nav = useNav()
  const pet = s.pets[0]
  const sorted = [...s.trips].sort((a, b) => a.startDate.localeCompare(b.startDate))
  const upcoming = sorted.filter((t) => t.endDate >= todayStr())
  const trips = upcoming.length ? upcoming : sorted.slice(0, 1)
  const bg = homeBackground(s)
  const face = petFace(pet)
  const [idx, setIdx] = useState(0)

  // 바로가기 — 모두 기존 화면으로 연결(새 기능 없음)
  const quick: { label: string; icon: string; go: () => void; hl?: boolean }[] = [
    { label: '여행 계획', icon: 'mapPin', go: () => nav.push({ name: 'tripBuilder' }), hl: true },
    { label: '코스 탐색', icon: 'nodes', go: () => nav.setTab('community') },
    { label: '멍스팟', icon: 'paw', go: () => nav.setTab('explore') },
    { label: '내 여행', icon: 'calendar', go: () => nav.setTab('trips') },
    { label: '커뮤니티', icon: 'users', go: () => nav.setTab('community') },
    { label: '반려견 정보', icon: 'heart', go: () => nav.push({ name: 'pet', id: pet?.id }) },
  ]

  return (
    <div className="home">
      <header className="home-head">
        <div className="home-logo">
          <span className="mark"><Icon name="paw" size={30} /></span>
          <div><b>멍로드</b><small>MUNGROAD</small></div>
        </div>
        <button className="home-icon-btn" aria-label="설정" onClick={() => nav.setTab('my')}><Icon name="settings" size={24} /></button>
      </header>

      <div className="home-greet">
        <h1>오늘은 어디로<br />떠나볼까요?<span className="paw"><Icon name="paw" size={22} /></span></h1>
        <button className="home-ava" aria-label={pet ? `${pet.name} 정보` : '반려견 등록'} onClick={() => nav.push({ name: 'pet', id: pet?.id })}>
          {face ? <img src={face} alt={pet ? `${pet.name} 사진` : ''} /> : <span className="ph"><Icon name="paw" size={44} /></span>}
        </button>
      </div>

      <button className="home-search glass" onClick={() => nav.setTab('explore')}>
        <Icon name="search" size={22} color="var(--ink)" />
        <span className="t">반려견과 가기 좋은 곳 검색</span>
        <span className="f"><Icon name="sliders" size={22} /></span>
      </button>

      <div className="trip-rail" onScroll={(e) => { const el = e.currentTarget; setIdx(Math.round(el.scrollLeft / (el.clientWidth - 28))) }}>
        {trips.length ? trips.map((t) => {
          const dest = placeMap[t.destinationId]
          const dday = Math.ceil((new Date(t.startDate).getTime() - Date.now()) / 86400000)
          return (
            <button key={t.id} className="trip" onClick={() => nav.push({ name: 'trip', id: t.id })}>
              {bg.src && <img src={bg.src} alt="" style={{ objectPosition: bg.position }} />}
              <div className="shade" /><div className="edge" />
              <div className="chips">
                <span className="glass-chip"><Icon name="paw" size={14} />{dday > 0 ? `D-${dday}` : dday === 0 ? 'D-DAY' : '지난'} 여행</span>
                <span className="glass-chip">{t.sentToCar ? '차로 보냄' : '아직 차로 보내지 않음'}</span>
              </div>
              <div className="txt">
                <h2>{t.name}</h2>
                <div className="meta">{md(t.startDate)} – {md(t.endDate)}{dest ? ` · ${dest.name}` : ''} · 경유 {t.waypointIds.length}곳</div>
              </div>
              <span className="go"><Icon name="chevron" size={20} /></span>
            </button>
          )
        }) : (
          <button className="trip empty" onClick={() => nav.push({ name: 'tripBuilder' })}>
            <div className="edge" />
            <div className="txt" style={{ right: 18 }}>
              <h2>예정된 여행이 없어요</h2>
              <div className="meta">여행을 만들어 차로 보내면, 차에서 ‘경로 안내’ 한 번으로 출발해요</div>
            </div>
            <span className="go"><Icon name="plus" size={20} /></span>
          </button>
        )}
      </div>
      <div className="dots" aria-hidden>{(trips.length ? trips : [0]).map((_, i) => <i key={i} className={i === Math.min(idx, trips.length - 1) ? 'on' : ''} />)}</div>

      <div className="quick">
        {quick.map((q) => (
          <button key={q.label} className={q.hl ? 'hl' : 'glass'} onClick={q.go}>
            <Icon name={q.icon} size={26} />{q.label}
          </button>
        ))}
      </div>

      {/* 시안 아래: 기존 홈 요소(주변 장소·즐겨찾기 / 반려견 휴식 간격 / 차량 연결) */}
      <section className="home-sec">
        <div className="home-sec-head"><h3>지금 주변 반려견 동반 장소</h3><button onClick={() => nav.setTab('explore')}>전체보기<Icon name="chevron" size={16} /></button></div>
        {!s.consent.location && <p className="home-note">위치 동의 전이라 강릉 주변 샘플을 보여 드려요.</p>}
        <div className="home-list glass">
          {BROWSABLE.slice(0, 2).map((p) => (
            <div key={p.id} className="home-place">
              <button onClick={() => nav.push({ name: 'place', id: p.id })} style={{ display: 'flex', gap: 12, alignItems: 'center', flex: 1, minWidth: 0, textAlign: 'left' }}>
                <span className="ic glass tint"><Icon name="mapPin" size={24} /></span>
                <div style={{ minWidth: 0 }}>
                  <h4>{p.name}</h4>
                  <div className="sub">{p.region} · {CATEGORY_LABEL[p.category]}</div>
                  <PlaceBadges p={p} max={1} showUser />
                </div>
              </button>
              <FavButton id={p.id} />
            </div>
          ))}
        </div>
      </section>

      {pet && (
        <button className="home-pet glass" onClick={() => nav.push({ name: 'rest', petId: pet.id })}>
          {face ? <img src={face} alt="" /> : <span className="ph"><Icon name="paw" size={28} /></span>}
          <div style={{ flex: 1, minWidth: 0 }}>
            <h4>{pet.name}</h4>
            <div className="sub">{pet.breed} · {pet.weightKg}kg</div>
            <div className="sub">{recommend(pet).reason}</div>
          </div>
          <span className="rest-pill">휴식 {recommend(pet).minutes}분</span>
        </button>
      )}

      <button className="home-car glass" onClick={() => nav.push({ name: 'carLink' })}>
        <span className="ic glass tint"><Icon name="car" size={24} /></span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <h4>{s.car.linked ? `차량 앱과 연결됨 · ${s.car.name}` : '차량 앱 연결하기'}</h4>
          <div className="sub">{s.car.linked ? (syncConfigured ? `마지막 동기화 ${s.car.lastSync ?? '-'}` : '동기화 서버 연결 전 · 보낼 항목은 휴대폰에 보관돼요') : 'Pleos Connect 차량의 멍로드 앱과 같은 계정으로 연결'}</div>
        </div>
        <Icon name="chevron" color="var(--tertiary)" />
      </button>
    </div>
  )
}

// ───────────────────────────── 탐색 (WF-M04) ─────────────────────────────
const SIZE_FILTERS = ['소형견', '중형견', '대형견'] as const

export function ExploreScreen() {
  const [q, setQ] = useState('')
  const [cat, setCat] = useState<Category | 'ALL'>('ALL')
  const [size, setSize] = useState<(typeof SIZE_FILTERS)[number] | null>(null)
  const [indoor, setIndoor] = useState(false)
  const [verifiedOnly, setVerifiedOnly] = useState(false)
  const [view, setView] = useState<'list' | 'map'>('list')
  const list = BROWSABLE.filter((p) =>
    (cat === 'ALL' || p.category === cat) &&
    (!q || p.name.includes(q) || p.region.includes(q)) &&
    (!size || p.sizes.includes(size)) &&
    (!indoor || p.indoor) &&
    (!verifiedOnly || p.verification === 'VERIFIED'))
  return (
    <>
      <TopBar title="탐색" right={<button className="chip" onClick={() => setView(view === 'list' ? 'map' : 'list')}>{view === 'list' ? '지도' : '목록'}</button>} />
      <div className="page">
        <label className="row card" style={{ padding: '0 14px', height: 46, borderRadius: 23 }}>
          <Icon name="search" color="var(--tertiary)" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="장소·지역 검색" style={{ border: 0, outline: 0, flex: 1, background: 'transparent', fontSize: 15 }} />
        </label>
        <div className="chips" style={{ marginTop: 12 }}>
          {(['ALL', 'CAFE', 'RESTAURANT', 'PARK', 'STAY'] as const).map((c) => (
            <button key={c} className={`chip ${cat === c ? 'on' : ''}`} onClick={() => setCat(c)}>{c === 'ALL' ? '전체' : CATEGORY_LABEL[c]}</button>
          ))}
        </div>
        <div className="chips" style={{ marginTop: 8 }}>
          {SIZE_FILTERS.map((z) => <button key={z} className={`chip ${size === z ? 'soft' : ''}`} onClick={() => setSize(size === z ? null : z)}>{z} 가능</button>)}
          <button className={`chip ${indoor ? 'soft' : ''}`} onClick={() => setIndoor(!indoor)}>실내 동반</button>
          <button className={`chip ${verifiedOnly ? 'soft' : ''}`} onClick={() => setVerifiedOnly(!verifiedOnly)}>확인된 곳만</button>
        </div>
        {view === 'map' && (
          <div className="map" style={{ marginTop: 12, height: 220 }}>
            {list.map((p, i) => (
              <button key={p.id} title={p.name} onClick={() => setQ(p.name)} style={{ position: 'absolute', left: `${12 + ((i * 37) % 76)}%`, top: `${15 + ((i * 53) % 70)}%`, width: 22, height: 22, borderRadius: 11, background: p.verification === 'VERIFIED' ? 'var(--orange)' : 'var(--chk)', border: '3px solid #fff' }} />
            ))}
            <span className="tiny" style={{ position: 'absolute', left: 10, top: 8 }}>지도 SDK 영역(시연)</span>
          </div>
        )}
        <div className="stack" style={{ marginTop: 12 }}>
          {list.length === 0 && <p className="muted">조건에 맞는 곳이 없어요. 필터를 줄여 보세요.</p>}
          {list.map((p) => <PlaceItem key={p.id} p={p} />)}
        </div>
      </div>
    </>
  )
}

// ───────────────────────────── 장소 상세 (WF-M05 / D-M04, 멍슐랭 ME-05) ─────────────────────────────
export function PlaceScreen({ id }: { id: string }) {
  const nav = useNav()
  const s = useStore()
  const p = placeMap[id]
  if (!p) return <TopBar title="장소를 찾을 수 없어요" onBack={nav.back} />
  const fav = s.favorites.includes(id)
  const mine = s.mungchelin.find((m) => m.placeId === id)
  return (
    <>
      <div style={{ position: 'relative' }}>
        <Scene img={p.image} h={240} radius={0} />
        <button className="icon-btn light" style={{ position: 'absolute', top: 14, left: 14 }} onClick={nav.back} aria-label="뒤로"><Icon name="back" /></button>
      </div>
      <div className="page" style={{ marginTop: -24, background: 'var(--cream)', borderRadius: '24px 24px 0 0', position: 'relative' }}>
        <h1 style={{ fontSize: 24, margin: '8px 0 2px' }}>{p.name}</h1>
        <div className="muted">{p.region} · {CATEGORY_LABEL[p.category]}</div>

        <div className="card" style={{ marginTop: 14 }}>
          <div className="row between"><h3>반려견 동반 조건</h3>{p.verification === 'VERIFIED' ? <span className="badge ok">확인됨</span> : <span className="badge chk">방문 전 확인 필요</span>}</div>
          {p.verification === 'VERIFIED' ? (
            <>
              <div className="badges" style={{ marginTop: 10 }}>
                {p.conditions.map((c) => <span key={c} className="badge ok">{c}</span>)}
                {p.restrictions.map((c) => <span key={c} className="badge info">{c}</span>)}
              </div>
              <p className="tiny" style={{ marginBottom: 0 }}>출처 {p.source} · 확인일 {p.verifiedAt}</p>
            </>
          ) : <p className="muted" style={{ marginBottom: 0 }}>동반 여부가 확인되지 않았어요. 방문 전에 전화로 확인해 주세요.</p>}
          {p.hours && (<><div className="divider" /><div className="row"><Icon name="clock" size={18} /> <span>{p.hours}{p.closedDays ? ` · ${p.closedDays} 휴무` : ''}</span></div></>)}
        </div>

        {/* 3차 멍슐랭 — 공식 정보와 분리 */}
        <div className="card user" style={{ marginTop: 12 }}>
          <div className="row between"><h3>멍슐랭 · 보호자 확인</h3>{p.mungchelin && <span className="badge user">이용자 확인 {p.mungchelin.count}건 · {p.mungchelin.lastAt}</span>}</div>
          {p.mungchelin ? (
            <>
              <div className="stack" style={{ marginTop: 10 }}>
                {p.mungchelin.items.map((it) => (
                  <div key={it.label} className="row between"><span>{it.label}</span><span className="tiny">맞아요 {it.yes} · 아니에요 {it.no}{it.yes && it.no >= it.yes / 2 ? ' · 의견이 엇갈려요' : ''}</span></div>
                ))}
              </div>
              {p.mungchelin.reviews.map((r) => (
                <div key={r.text} style={{ marginTop: 10, padding: 10, background: 'var(--cream)', borderRadius: 12 }}>
                  <div className="tiny">{r.author}{r.visitVerified ? ' · 방문 인증' : ''}</div>
                  <div style={{ fontSize: 14 }}>{r.text}</div>
                </div>
              ))}
            </>
          ) : <p className="muted">아직 보호자 확인이 없어요. 다녀오셨다면 첫 확인을 남겨 주세요.</p>}
          <p className="tiny">보호자가 남긴 확인이에요. 공식 동반 정보로 바꾸어 표시하지 않아요.</p>
          <button className="btn secondary" onClick={() => nav.push({ name: 'mungchelin', placeId: id })}><Icon name="check" />{mine ? '내 멍슐랭 고치기' : '멍슐랭 남기기'}</button>
        </div>

        <div className="btn-row" style={{ marginTop: 14 }}>
          <a className="btn secondary" href={p.phone ? `tel:${p.phone}` : undefined} style={{ textDecoration: 'none', opacity: p.phone ? 1 : 0.5 }}><Icon name="phone" />전화</a>
          <button className="btn secondary" onClick={() => setState((st) => ({ ...st, favorites: fav ? st.favorites.filter((f) => f !== id) : [...st.favorites, id] }))}>
            <Icon name={fav ? 'heartFill' : 'heart'} color={fav ? 'var(--orange)' : undefined} />{fav ? '즐겨찾기됨' : '즐겨찾기'}
          </button>
        </div>
        <button className="btn primary" style={{ marginTop: 10 }} onClick={() => nav.push({ name: 'tripBuilder', placeId: id })}><Icon name="plus" />여행에 추가</button>
        <div className="card soft row" style={{ marginTop: 12 }}>
          <Icon name="car" color="var(--orange)" />
          <span className="muted">길 안내는 차에서 — 여행에 추가하면 차량 앱에서 바로 출발할 수 있어요.</span>
        </div>
      </div>
    </>
  )
}
