import { useState } from 'react'
import type { FeedPost } from '../data/models'
import { placeMap } from '../data/samples'
import { factsOf, localDraft } from '../domain/diary'
import { useNav } from '../nav'
import { setState, uid, useStore } from '../store'
import { postFeed, postMungchelin, requestDiary, syncConfigured } from '../sync'
import { Icon, PlaceBadges, Scene, TopBar } from '../ui'

// ───────────────────────────── 커뮤니티 탭 (MC-01 · MC-05 / D-M06) ─────────────────────────────
export function CommunityScreen() {
  const s = useStore()
  const nav = useNav()
  const [seg, setSeg] = useState<'feed' | 'mung' | 'records'>('feed')
  const [theme, setTheme] = useState<string | null>(null)
  const feed = s.feed.filter((f) => f.visibility === 'public' && (!theme || f.theme === theme))
  return (
    <>
      <TopBar title="커뮤니티" />
      <div className="page">
        <div className="chips">
          {([['feed', '여행 피드'], ['mung', '멍슐랭'], ['records', '내 기록']] as const).map(([k, l]) => (
            <button key={k} className={`chip ${seg === k ? 'on' : ''}`} onClick={() => setSeg(k)}>{l}</button>
          ))}
        </div>
        {seg === 'feed' && (
          <>
            <div className="chips" style={{ marginTop: 10 }}>
              {['바다', '자연'].map((t) => <button key={t} className={`chip ${theme === t ? 'soft' : ''}`} onClick={() => setTheme(theme === t ? null : t)}>{t}</button>)}
            </div>
            <div className="stack" style={{ marginTop: 12 }}>{feed.map((f) => <FeedCard key={f.id} f={f} />)}</div>
          </>
        )}
        {seg === 'mung' && (
          <div className="stack" style={{ marginTop: 12 }}>
            <p className="muted">다녀온 장소의 동반 조건을 확인해 주세요. 같은 항목을 여러 보호자가 확인할수록 차 안 추천이 정확해져요.</p>
            {s.mungchelin.length === 0 && <div className="card soft"><h3>아직 남긴 멍슐랭이 없어요</h3><p className="muted">장소 상세나 여행 기록에서 남길 수 있어요.</p></div>}
            {s.mungchelin.map((m) => placeMap[m.placeId] && (
              <button key={m.placeId} className="list-item" onClick={() => nav.push({ name: 'place', id: m.placeId })}>
                <Scene img={placeMap[m.placeId].image} w={56} h={56} />
                <div className="grow"><h3 style={{ fontSize: 15 }}>{placeMap[m.placeId].name}</h3><div className="tiny">{m.at}{m.visitVerified ? ' · 방문 인증' : ''}</div></div>
                <span className="badge user">내 확인</span>
              </button>
            ))}
          </div>
        )}
        {seg === 'records' && (
          <div className="stack" style={{ marginTop: 12 }}>
            {s.records.map((r) => (
              <button key={r.id} className="card flush press" onClick={() => nav.push({ name: 'record', id: r.id })}>
                <Scene img={r.timeline.find((t) => t.photos.length)?.photos[0] ?? 'sea'} h={130} radius={0} />
                <div style={{ padding: 14 }}>
                  <h3>{r.tripName}</h3>
                  <div className="muted">{r.dateLabel} · 주행 {Math.floor(r.drivingMinutes / 60)}시간 {r.drivingMinutes % 60}분 · 휴식 {r.timeline.filter((t) => t.kind === 'rest').length}회</div>
                  {r.diary ? <span className="badge brand" style={{ marginTop: 6 }}>AI 여행 일기 있음</span> : <span className="badge info" style={{ marginTop: 6 }}>일기 쓰기 전</span>}
                </div>
              </button>
            ))}
            <p className="tiny">차량 앱에서 ‘여행 마무리’를 누르면 주행·휴식·방문 기록이 여기로 와요(동기화 서버 연결 시). 지금은 샘플 기록이에요.</p>
          </div>
        )}
      </div>
    </>
  )
}

function FeedCard({ f }: { f: FeedPost }) {
  const nav = useNav()
  return (
    <button className="card flush press" onClick={() => nav.push({ name: 'feed', id: f.id })}>
      <Scene img={f.image} h={140} radius={0} shade>
        <span className="badge" style={{ position: 'absolute', top: 12, left: 12, background: '#fff' }}><Icon name="route" size={14} color="var(--orange)" />&nbsp;코스</span>
      </Scene>
      <div style={{ padding: 14 }}>
        <div className="row between"><span className="tiny">{f.author} · {f.dogSize} · {f.createdAt}</span><span className="tiny row"><Icon name="heart" size={14} />{f.likes}</span></div>
        <h3 style={{ marginTop: 4 }}>{f.title}</h3>
        <div className="muted">{f.totalKm}km · 방문 {f.stopIds.length}곳 · 휴식 {f.restIds.length}곳</div>
      </div>
    </button>
  )
}

// ───────────────────────────── 피드 상세 · 코스 담기 (MC-02 / WF-M11) ─────────────────────────────
export function FeedDetailScreen({ id }: { id: string }) {
  const s = useStore()
  const nav = useNav()
  const f = s.feed.find((x) => x.id === id)
  if (!f) return <TopBar title="게시물을 찾을 수 없어요" onBack={nav.back} />
  return (
    <>
      <div style={{ position: 'relative' }}>
        <Scene img={f.image} h={220} radius={0} />
        <button className="icon-btn light" style={{ position: 'absolute', top: 14, left: 14 }} onClick={nav.back} aria-label="뒤로"><Icon name="back" /></button>
      </div>
      <div className="page no-tabs">
        <h1 style={{ fontSize: 22, margin: '6px 0 2px' }}>{f.title}</h1>
        <div className="muted">{f.author} · {f.dogSize} · {f.createdAt}</div>
        <div className="map" style={{ marginTop: 14 }}><span className="tiny" style={{ position: 'absolute', left: 12, top: 10 }}>코스 지도 · 공유자 출발지 비공개</span></div>
        <div className="card timeline" style={{ marginTop: 12 }}>
          {[...f.stopIds.slice(0, 1), ...f.restIds, ...f.stopIds.slice(1)].map((pid, i) => {
            const p = placeMap[pid]
            const rest = f.restIds.includes(pid)
            return p && (
              <div key={pid + i} className="tl-item">
                <div className={`tl-dot ${rest ? '' : 'on'}`}>{rest ? <Icon name="clock" size={18} color="var(--orange)" /> : <b style={{ color: '#fff', fontSize: 13 }}>{f.stopIds.indexOf(pid) + 1}</b>}</div>
                <div><h3 style={{ fontSize: 15 }}>{p.name}</h3><PlaceBadges p={p} max={1} showUser /></div>
              </div>
            )
          })}
        </div>
        <p className="tiny">방문지마다 공식 확인 상태와 보호자 확인(멍슐랭)을 구분해 보여 드려요.</p>
      </div>
      <div className="bottom-cta">
        <button className="btn primary" onClick={() => nav.push({ name: 'tripBuilder', fromPostId: f.id })}><Icon name="plus" />내 여행에 담기</button>
        <p className="tiny" style={{ textAlign: 'center', margin: '6px 0 0' }}>담으면 차량 앱 ‘다음 여행’에서 바로 출발할 수 있어요</p>
      </div>
    </>
  )
}

// ───────────────────────────── 멍슐랭 남기기 (MC-04 / WF-M12 · D-M07) ─────────────────────────────
const CHECK_ITEMS = ['중형견 동반 가능', '실내 동반 가능', '목줄 필수', '물그릇 있음', '그늘 · 테라스', '주차 쉬움']

export function MungchelinScreen({ placeId }: { placeId: string }) {
  const s = useStore()
  const nav = useNav()
  const p = placeMap[placeId]
  const prev = s.mungchelin.find((m) => m.placeId === placeId)
  const [answers, setAnswers] = useState<Record<string, 'yes' | 'no' | 'unknown'>>(prev?.answers ?? {})
  const [review, setReview] = useState(prev?.review ?? '')
  const [photos, setPhotos] = useState<number>(0)
  // 방문 인증: 차량 앱 도착 기록(DriveSession 방문지)에 이 장소가 있으면
  const visitVerified = s.records.some((r) => r.timeline.some((t) => t.placeId === placeId))
  if (!p) return <TopBar title="장소를 찾을 수 없어요" onBack={nav.back} />
  const answered = Object.values(answers).filter((v) => v !== 'unknown').length
  const save = async () => {
    const at = new Date().toLocaleDateString('ko-KR')
    setState((st) => ({ ...st, mungchelin: [...st.mungchelin.filter((m) => m.placeId !== placeId), { placeId, answers, review, at, visitVerified }] }))
    const r = await postMungchelin(placeId, { answers, review, visitVerified })
    nav.back()
    nav.toast(r.ok && !r.queued ? '멍슐랭을 남겼어요' : '저장했어요 · 동기화 서버 연결 전이라 휴대폰에 보관')
  }
  return (
    <>
      <TopBar title="멍슐랭 남기기" onBack={nav.back} />
      <div className="page no-tabs">
        <div className="list-item">
          <Scene img={p.image} w={56} h={56} />
          <div className="grow"><h3>{p.name}</h3>
            {visitVerified ? <span className="badge ok">방문 인증 · 차량 도착 기록</span> : <span className="badge info">방문 기록 없음</span>}
          </div>
        </div>
        <h2>{s.pets[0]?.name ?? '반려견'}와 가 보니 어땠나요?</h2>
        <div className="stack">
          {CHECK_ITEMS.map((it) => (
            <div key={it} className="card row between" style={{ padding: '10px 14px' }}>
              <span style={{ fontSize: 15 }}>{it}</span>
              <span className="seg">
                {(['yes', 'no'] as const).map((v) => (
                  <button key={v} className={`yn ${answers[it] === v ? v : ''}`} onClick={() => setAnswers({ ...answers, [it]: answers[it] === v ? 'unknown' : v })}>{v === 'yes' ? '맞아요' : '아니에요'}</button>
                ))}
              </span>
            </div>
          ))}
        </div>
        <label className="field"><span>짧은 후기 (선택)</span><textarea maxLength={200} value={review} onChange={(e) => setReview(e.target.value)} placeholder="예: 테라스 그늘이 있어 여름에도 좋아요" /></label>
        <div className="row" style={{ marginTop: 12 }}>
          <label className="thumb" style={{ border: '1.5px dashed var(--divider)', display: 'grid', placeItems: 'center', background: 'var(--card)', cursor: 'pointer' }}>
            <Icon name="camera" color="var(--tertiary)" />
            <input type="file" accept="image/*" multiple hidden onChange={(e) => setPhotos(e.target.files?.length ?? 0)} />
          </label>
          <span className="tiny row"><Icon name="lock" size={14} />{photos ? `사진 ${photos}장 · ` : ''}사진 위치 정보(EXIF)는 올리기 전에 지워져요</span>
        </div>
        <p className="tiny">보호자 확인은 공식 동반 정보로 바꾸어 표시하지 않아요. 한 장소에 한 번, 다시 가면 고칠 수 있어요.</p>
      </div>
      <div className="bottom-cta"><button className="btn primary" disabled={answered === 0} onClick={save}>남기기{answered ? ` (${answered}개 확인)` : ''}</button></div>
    </>
  )
}

// ───────────────────────────── 코스 공유하기 (MC-03 / WF-M13) ─────────────────────────────
export function ShareScreen({ recordId }: { recordId: string }) {
  const s = useStore()
  const nav = useNav()
  const r = s.records.find((x) => x.id === recordId)
  const [vis, setVis] = useState<FeedPost['visibility']>('public')
  const [title, setTitle] = useState(r ? `${r.tripName} 코스` : '')
  if (!r) return <TopBar title="기록을 찾을 수 없어요" onBack={nav.back} />
  const stops = r.timeline.filter((t) => t.kind === 'visit' && t.placeId).map((t) => t.placeId!) // 집 출발·도착은 제외
  const rests = r.timeline.filter((t) => t.kind === 'rest' && t.placeId).map((t) => t.placeId!)
  const share = async () => {
    const id = uid('f')
    const post: FeedPost = {
      id, author: '나', dogSize: '중형견', title, region: placeMap[stops[0]]?.region ?? '', theme: '바다', totalKm: r.distanceKm,
      stopIds: stops, restIds: rests, likes: 0, image: r.timeline.find((t) => t.photos.length)?.photos[0] ?? 'sea',
      createdAt: new Date().toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit' }), visibility: vis,
    }
    setState((st) => ({ ...st, feed: [post, ...st.feed] }))
    const res = await postFeed(id, post)
    nav.back()
    nav.toast(res.ok && !res.queued ? '코스를 공유했어요' : '저장했어요 · 서버 연결 전이라 휴대폰에만 보여요')
  }
  return (
    <>
      <TopBar title="코스 공유하기" onBack={nav.back} />
      <div className="page no-tabs">
        <Scene img={r.timeline.find((t) => t.photos.length)?.photos[0] ?? 'sea'} h={140} />
        <label className="field"><span>제목</span><input value={title} onChange={(e) => setTitle(e.target.value)} /></label>
        <div className="muted" style={{ marginTop: 8 }}>방문 {stops.length}곳 · 휴식 {rests.length}회 · {r.distanceKm}km</div>
        <h2>공개 범위</h2>
        <div className="chips">
          {([['public', '전체'], ['followers', '팔로워'], ['private', '나만 보기']] as const).map(([k, l]) => <button key={k} className={`chip ${vis === k ? 'on' : ''}`} onClick={() => setVis(k)}>{l}</button>)}
        </div>
        <div className="card stack" style={{ marginTop: 16 }}>
          <h3>개인정보 보호 (끌 수 없어요)</h3>
          <div className="row"><Icon name="check" size={18} color="var(--ok)" /><span>집 주변 출발지·도착지 숨김 — 첫 방문지부터 공개</span></div>
          <div className="row"><Icon name="check" size={18} color="var(--ok)" /><span>사진 위치 정보(EXIF) 삭제</span></div>
          <div className="row"><Icon name="check" size={18} color="var(--ok)" /><span>정확한 방문 시각 대신 날짜만</span></div>
        </div>
        <p className="tiny">공식 동반 정보가 아닌 내 경험으로 표시돼요. 신고가 쌓인 게시물은 숨겨져요.</p>
      </div>
      <div className="bottom-cta"><button className="btn primary" disabled={!title.trim() || stops.length === 0} onClick={share}><Icon name="share" />공유하기</button></div>
    </>
  )
}

// ───────────────────────────── 여행 기록 (MC-07 / WF-M14 · D-M08) ─────────────────────────────
export function RecordScreen({ id }: { id: string }) {
  const s = useStore()
  const nav = useNav()
  const r = s.records.find((x) => x.id === id)
  if (!r) return <TopBar title="기록을 찾을 수 없어요" onBack={nav.back} />
  return (
    <>
      <TopBar title="여행 기록" onBack={nav.back} />
      <div className="page">
        <div className="map"><span className="tiny" style={{ position: 'absolute', left: 12, top: 10 }}>경로 지도 · 집 주변은 흐리게</span></div>
        <h1 style={{ fontSize: 22, margin: '14px 0 2px' }}>{r.tripName}</h1>
        <div className="muted">{r.dateLabel}</div>
        <div className="chips" style={{ marginTop: 10 }}>
          <span className="chip soft"><Icon name="car" size={16} />주행 {Math.floor(r.drivingMinutes / 60)}시간 {r.drivingMinutes % 60}분</span>
          <span className="chip soft"><Icon name="clock" size={16} />휴식 {r.timeline.filter((t) => t.kind === 'rest').length}회</span>
          <span className="chip soft"><Icon name="pin" size={16} />방문 {r.timeline.filter((t) => t.kind === 'visit').length}곳</span>
        </div>
        <div className="timeline" style={{ marginTop: 16 }}>
          {r.timeline.map((t, i) => (
            <div key={i} className="tl-item">
              <div className={`tl-dot ${t.kind === 'rest' ? 'on' : ''}`}><Icon name={t.kind === 'rest' ? 'clock' : t.kind === 'visit' ? 'pin' : 'home'} size={18} color={t.kind === 'rest' ? '#fff' : 'var(--orange)'} /></div>
              <div className="grow">
                <div className="row"><span className="tiny">{t.time}</span><b style={{ fontSize: 15 }}>{t.placeId ? placeMap[t.placeId]?.name ?? t.label : t.label}{t.minutes ? ` · 휴식 ${t.minutes}분` : ''}</b></div>
                {t.photos.length > 0 && <div className="row" style={{ marginTop: 6 }}>{t.photos.map((ph, j) => <Scene key={j} img={ph} w={72} h={72} radius={12} />)}</div>}
                {t.kind === 'visit' && t.placeId && <button className="chip" style={{ marginTop: 6, height: 30 }} onClick={() => nav.push({ name: 'mungchelin', placeId: t.placeId! })}>멍슐랭 남기기</button>}
              </div>
            </div>
          ))}
        </div>
        <button className="btn primary" onClick={() => nav.push({ name: 'diary', id: r.id })}><Icon name="sparkle" />{r.diary ? 'AI 여행 일기 보기' : 'AI 여행 일기 만들기'}</button>
        <div className="btn-row" style={{ marginTop: 10 }}>
          <button className="btn secondary" onClick={() => nav.push({ name: 'share', recordId: r.id })}><Icon name="share" />코스 공유</button>
          <label className="btn secondary" style={{ cursor: 'pointer' }}><Icon name="camera" />사진 추가<input type="file" accept="image/*" multiple hidden onChange={(e) => e.target.files?.length && nav.toast(`사진 ${e.target.files.length}장을 촬영 시각에 맞춰 놓았어요(시연)`)} /></label>
        </div>
      </div>
    </>
  )
}

// ───────────────────────────── AI 여행 일기 (MC-08 / WF-M15 · D-M09) ─────────────────────────────
export function DiaryScreen({ id }: { id: string }) {
  const s = useStore()
  const nav = useNav()
  const r = s.records.find((x) => x.id === id)
  const petName = s.pets[0]?.name ?? '반려견'
  const [pov, setPov] = useState<'pet' | 'owner'>(r?.diary?.pov ?? 'pet')
  const [draft, setDraft] = useState<{ title: string; body: string } | null>(r?.diary ? { title: r.diary.title, body: r.diary.body } : null)
  const [loading, setLoading] = useState(false)
  if (!r) return <TopBar title="기록을 찾을 수 없어요" onBack={nav.back} />
  const facts = factsOf(r, placeMap)
  const generate = async () => {
    setLoading(true)
    const server = await requestDiary(facts.map((f) => f.text), pov, petName)
    setDraft(server ?? localDraft(r, placeMap, petName, pov))
    setLoading(false)
  }
  const save = () => {
    if (!draft) return
    setState((st) => ({ ...st, records: st.records.map((x) => (x.id === r.id ? { ...x, diary: { pov, ...draft, savedAt: Date.now() } } : x)) }))
    nav.back()
    nav.toast('일기를 저장했어요')
  }
  return (
    <>
      <TopBar title="AI 여행 일기" onBack={nav.back} />
      <div className="page no-tabs">
        <div className="card">
          <div className="tiny" style={{ fontWeight: 700 }}>이 기록으로만 일기를 써요</div>
          <div className="stack" style={{ marginTop: 8 }}>
            {facts.map((f, i) => <div key={i} className="row"><Icon name="check" size={16} color="var(--ok)" /><span style={{ fontSize: 14 }}>{f.text}</span></div>)}
          </div>
        </div>
        <div className="chips" style={{ marginTop: 12 }}>
          <button className={`chip ${pov === 'pet' ? 'on' : ''}`} onClick={() => setPov('pet')}>{petName} 시점</button>
          <button className={`chip ${pov === 'owner' ? 'on' : ''}`} onClick={() => setPov('owner')}>보호자 시점</button>
        </div>
        {draft ? (
          <div className="card flush" style={{ marginTop: 12 }}>
            <Scene img={r.timeline.find((t) => t.photos.length)?.photos[0] ?? 'sea'} h={130} radius={0} />
            <div style={{ padding: 16 }}>
              <input value={draft.title} onChange={(e) => setDraft({ ...draft, title: e.target.value })} style={{ fontWeight: 700, fontSize: 17, border: 0, width: '100%', outline: 0 }} aria-label="일기 제목" />
              <textarea value={draft.body} onChange={(e) => setDraft({ ...draft, body: e.target.value })} style={{ width: '100%', minHeight: 160, border: 0, resize: 'vertical', fontSize: 15, lineHeight: 1.6, outline: 0, marginTop: 6 }} aria-label="일기 본문" />
              <span className="tiny">✎ 바로 고칠 수 있어요</span>
            </div>
          </div>
        ) : (
          <div className="card soft" style={{ marginTop: 12 }}>
            <h3>일기 초안을 만들어 볼까요?</h3>
            <p className="muted">{syncConfigured ? '서버 AI가 위 기록만 보고 초안을 써요.' : '서버 연결 전이라 기록 문장을 이어 붙인 초안을 써요.'}</p>
          </div>
        )}
        <p className="tiny row" style={{ marginTop: 10 }}><Icon name="info" size={14} />기록에 없는 일이나 반려견의 건강·기분은 지어내지 않아요.</p>
      </div>
      <div className="bottom-cta">
        <div className="btn-row">
          <button className="btn secondary" disabled={loading} onClick={generate}>{draft ? '다시 쓰기' : loading ? '쓰는 중…' : '초안 만들기'}</button>
          <button className="btn primary" disabled={!draft} onClick={save}>저장</button>
        </div>
      </div>
    </>
  )
}
