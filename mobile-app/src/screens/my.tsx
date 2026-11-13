import { useState } from 'react'
import { HEALTH_LABEL, type Health, type Pet } from '../data/models'
import { ageYears, recommend, sizeOf } from '../domain/restGuideline'
import { useNav } from '../nav'
import { getState, resetState, setState, uid, useStore } from '../store'
import { syncConfigured, syncPet } from '../sync'
import { Icon, Progress, TopBar, Toggle } from '../ui'
import { petFace, readPhoto } from '../photos'

const HEALTHS: Health[] = ['NONE', 'JOINT', 'HEART', 'RESPIRATORY', 'KIDNEY', 'DIABETES']

function PetForm({ pet, onChange }: { pet: Pet; onChange: (p: Pet) => void }) {
  return (
    <>
      <label className="field"><span>이름</span><input value={pet.name} onChange={(e) => onChange({ ...pet, name: e.target.value })} placeholder="예: 사슴이" /></label>
      <label className="field"><span>견종</span><input value={pet.breed} onChange={(e) => onChange({ ...pet, breed: e.target.value })} placeholder="예: 믹스" /></label>
      <div className="row">
        <label className="field grow"><span>태어난 해</span><input type="number" inputMode="numeric" value={pet.birthYear} onChange={(e) => onChange({ ...pet, birthYear: Number(e.target.value) })} /></label>
        <label className="field grow"><span>월</span><input type="number" inputMode="numeric" min={1} max={12} value={pet.birthMonth} onChange={(e) => onChange({ ...pet, birthMonth: Number(e.target.value) })} /></label>
        <label className="field grow"><span>체중(kg)</span><input type="number" inputMode="decimal" step="0.1" value={pet.weightKg} onChange={(e) => onChange({ ...pet, weightKg: Number(e.target.value) })} /></label>
      </div>
      <p className="tiny">{ageYears(pet).toFixed(1)}살 · 체급 {sizeOf(pet.weightKg)} (체급별로 노령 기준 나이가 달라요)</p>
      <div className="field"><span>활동 성향</span>
        <div className="chips">{(['차분함', '보통', '활발함'] as const).map((a) => <button key={a} className={`chip ${pet.activity === a ? 'on' : ''}`} onClick={() => onChange({ ...pet, activity: a })}>{a}</button>)}</div>
      </div>
      <Toggle label="얼굴이 납작한 견종(퍼그·불독·프렌치 불독 등)" on={pet.brachycephalic} onChange={(v) => onChange({ ...pet, brachycephalic: v })} />
      <label className="field"><span>동반 시 주의사항</span><textarea value={pet.caution} onChange={(e) => onChange({ ...pet, caution: e.target.value })} placeholder="예: 차멀미가 있어요" /></label>
    </>
  )
}

function HealthPicker({ pet, onChange }: { pet: Pet; onChange: (p: Pet) => void }) {
  const toggle = (h: Health) => {
    if (h === 'NONE') return onChange({ ...pet, health: ['NONE'] })
    const rest = pet.health.filter((x) => x !== 'NONE')
    const next = rest.includes(h) ? rest.filter((x) => x !== h) : [...rest, h]
    onChange({ ...pet, health: next.length ? next : ['NONE'] })
  }
  return (
    <>
      <div className="chips wrap" style={{ flexWrap: 'wrap' }}>
        {HEALTHS.map((h) => <button key={h} className={`chip ${pet.health.includes(h) ? 'orange' : ''}`} onClick={() => toggle(h)}>{HEALTH_LABEL[h]}</button>)}
      </div>
      <p className="tiny row" style={{ marginTop: 10 }}><Icon name="lock" size={14} />이 기기에만 저장되고 휴식 간격 계산에만 써요. 차량에는 계산된 간격만 보내요.</p>
    </>
  )
}

function RestCard({ pet }: { pet: Pet }) {
  const r = recommend(pet)
  return (
    <div className="card soft">
      <div className="tiny" style={{ color: 'var(--orange)', fontWeight: 700 }}>{pet.name || '반려견'}에게 맞는 휴식 간격</div>
      <div className="big-num">{r.minutes}분마다</div>
      <div style={{ fontSize: 14 }}>{r.reason} · {r.tip}</div>
      <p className="tiny" style={{ marginBottom: 0 }}>농림축산식품부·AKC·PetMD 등 자료로 만든 기준표 초안이에요. 출시 전 수의사 자문으로 확정해요.</p>
    </div>
  )
}

// ───────────────────────────── 온보딩 (MON-01~05 / WF-M01·M02) ─────────────────────────────
export function Onboarding() {
  const nav = useNav()
  const [step, setStep] = useState(0)
  const [pet, setPet] = useState<Pet>(() => ({ ...getState().pets[0] }))
  const [consent, setConsent] = useState({ location: false, sync: false })
  const finish = () => {
    setState((s) => ({ ...s, onboarded: true, pets: [pet, ...s.pets.slice(1)], consent }))
    nav.toast(`${pet.name}의 휴식 간격은 ${recommend(pet).minutes}분이에요`)
  }
  return (
    <>
      <TopBar title={['멍로드 시작하기', '반려견 등록', '건강 상태', '휴식 간격', '차량 앱 연결'][step]} onBack={step ? () => setStep(step - 1) : undefined} />
      <div className="page no-tabs">
        <div className="tiny">{step + 1} / 5</div>
        <div style={{ margin: '8px 0 12px' }}><Progress step={step + 1} total={5} /></div>
        {step === 0 && (
          <>
            <div className="scene hero" style={{ height: 180, borderRadius: 24, display: 'grid', placeItems: 'center' }}><Icon name="paw" size={88} color="rgba(255,255,255,.8)" /></div>
            <h2 style={{ fontSize: 22 }}>반려견과 떠나는 모든 길</h2>
            <p className="muted">휴대폰에서 반려견과 여행을 자세히 준비하고, Pleos Connect 차량의 멍로드 앱에서 바로 출발해요.</p>
            <div className="card">
              <Toggle label="위치 사용 동의 (주변 반려견 동반 장소)" on={consent.location} onChange={(v) => setConsent({ ...consent, location: v })} />
              <Toggle label="차량 앱과 동기화 동의 (반려견·여행·즐겨찾기)" on={consent.sync} onChange={(v) => setConsent({ ...consent, sync: v })} />
              <p className="tiny" style={{ margin: 0 }}>동의하지 않아도 지역을 골라 쓸 수 있어요. 마이에서 언제든 바꿀 수 있어요.</p>
            </div>
          </>
        )}
        {step === 1 && <PetForm pet={pet} onChange={setPet} />}
        {step === 2 && (<><h2>{pet.name}의 건강 상태를 알려 주세요 (선택)</h2><HealthPicker pet={pet} onChange={setPet} /></>)}
        {step === 3 && (<><RestCard pet={pet} /><p className="muted" style={{ marginTop: 12 }}>차가 실제로 달린 시간(신호 대기·정체 제외)으로 쉬어 갈 때를 알려 드려요. 참고 안내이며 차량이 반려견의 건강 상태를 측정하지 않아요.</p></>)}
        {step === 4 && (
          <div className="card">
            <h3>차량 앱 연결</h3>
            <p className="muted">차량 멍로드 앱 설정 › 모바일 연동에 나오는 6자리 코드를 입력하면 같은 계정으로 연결돼요. 나중에 해도 돼요.</p>
            <button className="btn secondary" onClick={() => nav.toast('마이 › 차량 연동에서 연결할 수 있어요')}><Icon name="car" />나중에 연결</button>
          </div>
        )}
      </div>
      <div className="bottom-cta">
        <button className="btn primary" disabled={step === 1 && !pet.name.trim()} onClick={() => (step < 4 ? setStep(step + 1) : finish())}>{step < 4 ? '다음' : '시작하기'}</button>
      </div>
    </>
  )
}

// ───────────────────────────── 마이 (MM / WF-M09) ─────────────────────────────
export function MyScreen() {
  const s = useStore()
  const nav = useNav()
  return (
    <>
      <TopBar title="마이" />
      <div className="page">
        <div className="stack">
          {s.pets.map((p) => (
            <button key={p.id} className="list-item" onClick={() => nav.push({ name: 'pet', id: p.id })}>
              {petFace(p)
                ? <img className="thumb" src={petFace(p)!} alt="" style={{ borderRadius: 32, objectFit: 'cover' }} />
                : <div className="thumb" style={{ borderRadius: 32, background: 'var(--orange-soft)', display: 'grid', placeItems: 'center' }}><Icon name="paw" color="var(--orange)" size={30} /></div>}
              <div className="grow"><h3>{p.name}</h3><div className="muted">{p.breed} · {Math.floor(ageYears(p))}살 · {p.weightKg}kg</div></div>
              <span className="badge brand">휴식 {recommend(p).minutes}분</span>
            </button>
          ))}
          <button className="btn secondary" onClick={() => nav.push({ name: 'pet' })}><Icon name="plus" />반려견 추가</button>
        </div>
        <div className="card" style={{ marginTop: 16, padding: '4px 16px' }}>
          {([['휴식 설정', () => nav.push({ name: 'rest', petId: s.pets[0]?.id ?? '' })], ['차량 연동 관리', () => nav.push({ name: 'carLink' })]] as const).map(([l, f]) => (
            <button key={l} className="row between" style={{ width: '100%', padding: '14px 0', borderBottom: '1px solid var(--divider)' }} onClick={f}><span style={{ fontSize: 15 }}>{l}</span><Icon name="chevron" color="var(--tertiary)" /></button>
          ))}
          <Toggle label="위치 사용 동의" on={s.consent.location} onChange={(v) => setState((st) => ({ ...st, consent: { ...st.consent, location: v } }))} />
          <Toggle label="차량 앱 동기화 동의" on={s.consent.sync} onChange={(v) => setState((st) => ({ ...st, consent: { ...st.consent, sync: v } }))} />
        </div>
        <HomeBgSetting />
        <h2>개인정보 · 데이터</h2>
        <div className="card stack">
          <p className="tiny" style={{ margin: 0 }}>보낼 항목 {s.outbox.length}개가 휴대폰에 보관돼 있어요{syncConfigured ? '' : ' (동기화 서버 연결 전)'}.</p>
          <button className="btn secondary" onClick={() => { if (window.confirm('여행·기록·즐겨찾기·멍슐랭을 모두 지우고 처음 상태로 돌릴까요? 되돌릴 수 없어요.')) { resetState(); nav.toast('모든 데이터를 지웠어요') } }}>내 데이터 모두 지우기</button>
        </div>
        <p className="tiny" style={{ textAlign: 'center', marginTop: 20 }}>멍로드 모바일 · 설치형 웹앱(PWA) · Pleos SDK 미사용(차량 연동은 차량 앱 담당)</p>
      </div>
    </>
  )
}

export function PetScreen({ id }: { id?: string }) {
  const s = useStore()
  const nav = useNav()
  const existing = s.pets.find((p) => p.id === id)
  const [pet, setPet] = useState<Pet>(existing ?? { id: uid('pet'), name: '', breed: '', birthYear: new Date().getFullYear() - 3, birthMonth: 1, weightKg: 8, brachycephalic: false, activity: '보통', caution: '', health: ['NONE'] })
  const save = async () => {
    setState((st) => ({ ...st, pets: existing ? st.pets.map((p) => (p.id === pet.id ? pet : p)) : [...st.pets, pet] }))
    if (s.consent.sync) await syncPet(pet.id, recommend(pet).minutes)
    nav.back(); nav.toast('저장했어요')
  }
  return (
    <>
      <TopBar title={existing ? `${existing.name} 정보` : '반려견 추가'} onBack={nav.back} />
      <div className="page no-tabs">
        <PetPhoto pet={pet} onChange={setPet} />
        <PetForm pet={pet} onChange={setPet} />
        <h2>건강 상태 (선택)</h2>
        <HealthPicker pet={pet} onChange={setPet} />
        <div style={{ marginTop: 16 }}><RestCard pet={pet} /></div>
        {existing && s.pets.length > 1 && <button className="btn secondary" style={{ marginTop: 12 }} onClick={() => { if (window.confirm(`${existing.name}을(를) 지울까요?`)) { setState((st) => ({ ...st, pets: st.pets.filter((p) => p.id !== existing.id) })); nav.back() } }}>삭제</button>}
      </div>
      <div className="bottom-cta"><button className="btn primary" disabled={!pet.name.trim()} onClick={save}>저장</button></div>
    </>
  )
}

// ───────────────────────────── 휴식 설정 (MM-02 / D-M02) ─────────────────────────────
export function RestScreen({ petId }: { petId: string }) {
  const s = useStore()
  const nav = useNav()
  const pet = s.pets.find((p) => p.id === petId) ?? s.pets[0]
  const [custom, setCustom] = useState<number | undefined>(pet?.customRestMinutes)
  const [vet, setVet] = useState<string>(pet?.vetRestMinutes ? String(pet.vetRestMinutes) : '')
  if (!pet) return <TopBar title="반려견이 없어요" onBack={nav.back} />
  const preview: Pet = { ...pet, customRestMinutes: custom, vetRestMinutes: vet ? Number(vet) : undefined }
  const save = async () => {
    setState((st) => ({ ...st, pets: st.pets.map((p) => (p.id === pet.id ? preview : p)) }))
    if (s.consent.sync) await syncPet(pet.id, recommend(preview).minutes)
    nav.back(); nav.toast(`휴식 간격 ${recommend(preview).minutes}분으로 저장했어요`)
  }
  return (
    <>
      <TopBar title="휴식 주기" onBack={nav.back} />
      <div className="page no-tabs">
        <RestCard pet={preview} />
        <h2>직접 바꾸기</h2>
        <div className="row">
          <button className="btn secondary" style={{ width: 56 }} onClick={() => setCustom(Math.max(30, (custom ?? recommend(pet).minutes) - 10))}><Icon name="minus" /></button>
          <div className="grow" style={{ textAlign: 'center' }}><div className="big-num">{custom ?? '추천값'}</div><div className="tiny">{custom ? '분' : '기준표 추천값 사용 중'}</div></div>
          <button className="btn secondary" style={{ width: 56 }} onClick={() => setCustom(Math.min(240, (custom ?? recommend(pet).minutes) + 10))}><Icon name="plus" /></button>
        </div>
        {custom && <button className="btn secondary" style={{ marginTop: 8 }} onClick={() => setCustom(undefined)}>추천값으로 되돌리기</button>}
        <label className="field"><span>수의사가 정한 간격(분) — 입력하면 가장 먼저 적용</span><input type="number" inputMode="numeric" value={vet} onChange={(e) => setVet(e.target.value)} placeholder="예: 60" /></label>
        <p className="tiny">여러 마리가 함께 타면 가장 짧은 간격을 따라요. 참고 안내이며 차량이 반려견의 건강 상태를 측정하지 않아요.</p>
      </div>
      <div className="bottom-cta"><button className="btn primary" onClick={save}>저장</button></div>
    </>
  )
}

// ───────────────────────────── 차량 연동 (MON-05 · MM-04) ─────────────────────────────
export function CarLinkScreen() {
  const s = useStore()
  const nav = useNav()
  const [code, setCode] = useState('')
  const link = () => {
    if (!/^\d{6}$/.test(code)) { nav.toast('6자리 숫자를 입력해 주세요'); return }
    // 서버 연결 전: 코드 형식만 확인하고 '연결 대기'로 둠(실제 확인은 동기화 서버가 함)
    setState((st) => ({ ...st, car: { linked: true, name: 'Pleos Connect 차량', lastSync: syncConfigured ? new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : undefined }, consent: { ...st.consent, sync: true } }))
    nav.back(); nav.toast(syncConfigured ? '차량 앱과 연결했어요' : '연결 정보를 저장했어요 · 동기화 서버 연결 전')
  }
  return (
    <>
      <TopBar title="차량 연동" onBack={nav.back} />
      <div className="page no-tabs">
        <div className="card soft row"><Icon name="car" color="var(--orange)" size={28} /><div><h3>{s.car.linked ? s.car.name : '아직 연결된 차량이 없어요'}</h3><div className="tiny">{s.car.linked ? (s.car.lastSync ? `마지막 동기화 ${s.car.lastSync}` : '동기화 서버 연결 전') : '같은 계정으로 반려견·여행·즐겨찾기를 공유해요'}</div></div></div>
        {!s.car.linked ? (
          <>
            <label className="field"><span>차량 멍로드 앱 › 설정 › 모바일 연동의 6자리 코드</span><input inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} placeholder="000000" style={{ letterSpacing: 6, fontSize: 22, textAlign: 'center' }} /></label>
            <button className="btn primary" style={{ marginTop: 14 }} onClick={link}>연결하기</button>
          </>
        ) : (
          <button className="btn secondary" style={{ marginTop: 14 }} onClick={() => { if (window.confirm('차량 연결을 끊을까요?')) setState((st) => ({ ...st, car: { linked: false, name: '' } })) }}>연결 해제</button>
        )}
        <div className="card stack" style={{ marginTop: 16 }}>
          <h3>무엇이 오가나요?</h3>
          <div className="tiny">휴대폰 → 차: 반려견 프로필(건강 상태 제외)·휴식 간격, 여행, 즐겨찾기</div>
          <div className="tiny">차 → 휴대폰: 여행 마무리 때 주행·휴식·방문 기록(사진·일기는 휴대폰에서)</div>
          <div className="tiny">차량 신호(속도·기어·내비)는 차량 앱이 Pleos SDK로만 읽고 휴대폰으로 보내지 않아요.</div>
        </div>
      </div>
    </>
  )
}

// ───────────────────────────── 반려견 프로필 사진 ─────────────────────────────
function PetPhoto({ pet, onChange }: { pet: Pet; onChange: (p: Pet) => void }) {
  const nav = useNav()
  const face = petFace(pet)
  const pick = async (f?: File) => {
    if (!f) return
    try { onChange({ ...pet, photo: await readPhoto(f, 'square') }) } catch { nav.toast('사진을 열 수 없어요. 다른 사진을 골라 주세요') }
  }
  return (
    <div className="row" style={{ gap: 16, marginTop: 6 }}>
      <label style={{ position: 'relative', cursor: 'pointer' }} aria-label="프로필 사진 고르기">
        {face
          ? <img src={face} alt="" style={{ width: 88, height: 88, borderRadius: 44, objectFit: 'cover', border: '3px solid #fff', boxShadow: 'var(--shadow)' }} />
          : <span style={{ width: 88, height: 88, borderRadius: 44, background: 'var(--orange-soft)', display: 'grid', placeItems: 'center' }}><Icon name="paw" size={36} color="var(--orange)" /></span>}
        <span style={{ position: 'absolute', right: -2, bottom: -2, width: 30, height: 30, borderRadius: 15, background: 'var(--cta)', color: '#fff', display: 'grid', placeItems: 'center', border: '2px solid #fff' }}><Icon name="camera" size={16} /></span>
        <input type="file" accept="image/*" hidden onChange={(e) => { pick(e.target.files?.[0]); e.target.value = '' }} />
      </label>
      <div className="grow">
        <h3>프로필 사진</h3>
        <p className="tiny" style={{ margin: '4px 0 0' }}>정사각형으로 맞춰 이 휴대폰에 저장돼요. 홈과 마이에 보여요.</p>
        {pet.photo && <button className="tiny" style={{ marginTop: 6, color: 'var(--alert)' }} onClick={() => onChange({ ...pet, photo: undefined })}>사진 지우기</button>}
      </div>
    </div>
  )
}

// ───────────────────────────── 홈 배경(여행 카드 사진) ─────────────────────────────
function HomeBgSetting() {
  const s = useStore()
  const nav = useNav()
  const kind = s.homeBg?.kind ?? 'pet'
  const pick = async (f?: File) => {
    if (!f) return
    try {
      const custom = await readPhoto(f, 'wide')
      setState((st) => ({ ...st, homeBg: { kind: 'custom', custom } }))
      nav.toast('홈 배경을 내 사진으로 바꿨어요')
    } catch { nav.toast('사진을 열 수 없어요. 다른 사진을 골라 주세요') }
  }
  return (
    <>
      <h2>홈 배경</h2>
      <div className="card" style={{ padding: '4px 16px' }}>
        <button className="row between" style={{ width: '100%', padding: '14px 0', borderBottom: '1px solid var(--divider)' }} onClick={() => setState((st) => ({ ...st, homeBg: { ...st.homeBg, kind: 'pet' } }))}>
          <span style={{ fontSize: 15 }}>{s.pets[0]?.name ?? '반려견'} 사진</span>{kind === 'pet' && <Icon name="check" color="var(--orange)" />}
        </button>
        {s.homeBg?.custom && (
          <button className="row between" style={{ width: '100%', padding: '14px 0', borderBottom: '1px solid var(--divider)' }} onClick={() => setState((st) => ({ ...st, homeBg: { ...st.homeBg, kind: 'custom' } }))}>
            <span style={{ fontSize: 15 }}>내가 올린 사진</span>{kind === 'custom' && <Icon name="check" color="var(--orange)" />}
          </button>
        )}
        <label className="row between" style={{ width: '100%', padding: '14px 0', cursor: 'pointer' }}>
          <span style={{ fontSize: 15 }}>{s.homeBg?.custom ? '다른 사진으로 바꾸기' : '내 사진 올리기'}</span><Icon name="camera" color="var(--tertiary)" />
          <input type="file" accept="image/*" hidden onChange={(e) => { pick(e.target.files?.[0]); e.target.value = '' }} />
        </label>
      </div>
    </>
  )
}
