import type { ReactNode } from 'react'
import type { Place } from './data/models'

// 24px stroke 아이콘 — Figma 'icon/*' 와 같은 모양
const P = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const }
const paths: Record<string, ReactNode> = {
  home: <path {...P} d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z" />,
  search: <><circle {...P} cx="11" cy="11" r="7" /><path {...P} d="m20 20-3.5-3.5" /></>,
  calendar: <><rect {...P} x="3" y="5" width="18" height="16" rx="2" /><path {...P} d="M16 3v4M8 3v4M3 10h18" /></>,
  heart: <path {...P} d="M12 20s-7.5-4.6-9.3-9.2C1.5 7.6 3.6 4.5 7 4.5c2 0 3.4 1.1 5 3 1.6-1.9 3-3 5-3 3.4 0 5.5 3.1 4.3 6.3C19.5 15.4 12 20 12 20z" />,
  heartFill: <path fill="currentColor" d="M12 20s-7.5-4.6-9.3-9.2C1.5 7.6 3.6 4.5 7 4.5c2 0 3.4 1.1 5 3 1.6-1.9 3-3 5-3 3.4 0 5.5 3.1 4.3 6.3C19.5 15.4 12 20 12 20z" />,
  paw: <g fill="currentColor"><circle cx="6.5" cy="9.5" r="2" /><circle cx="10" cy="5.5" r="2" /><circle cx="14" cy="5.5" r="2" /><circle cx="17.5" cy="9.5" r="2" /><path d="M12 11c-3 0-6 4-6 6.6C6 19 7.2 20 8.7 20c1.2 0 2-.6 3.3-.6s2.1.6 3.3.6c1.5 0 2.7-1 2.7-2.4C18 15 15 11 12 11z" /></g>,
  users: <><circle {...P} cx="9" cy="8" r="3.5" /><path {...P} d="M2.5 20a6.5 6.5 0 0 1 13 0M16 4.5a3.5 3.5 0 0 1 0 7M18 14a6 6 0 0 1 3.5 6" /></>,
  back: <path {...P} d="m15 6-6 6 6 6" />,
  chevron: <path {...P} d="m9 6 6 6-6 6" />,
  pin: <><path {...P} d="M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z" /><circle {...P} cx="12" cy="9.5" r="2.5" /></>,
  car: <><path {...P} d="M5 11l1.8-4.5A2 2 0 0 1 8.7 5h6.6a2 2 0 0 1 1.9 1.5L19 11" /><rect {...P} x="3" y="11" width="18" height="6" rx="2" /><path {...P} d="M6 17v2M18 17v2" /></>,
  clock: <><circle {...P} cx="12" cy="12" r="9" /><path {...P} d="M12 7v5l3 2" /></>,
  check: <><circle {...P} cx="12" cy="12" r="9" /><path {...P} d="m8 12 3 3 5-6" /></>,
  plus: <path {...P} d="M12 5v14M5 12h14" />,
  minus: <path {...P} d="M5 12h14" />,
  phone: <path {...P} d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z" />,
  route: <><circle {...P} cx="6" cy="19" r="2" /><circle {...P} cx="18" cy="5" r="2" /><path {...P} d="M8 19h8.5a3.5 3.5 0 0 0 0-7h-9a3.5 3.5 0 0 1 0-7H16" /></>,
  lock: <><rect {...P} x="5" y="11" width="14" height="10" rx="2" /><path {...P} d="M8 11V7a4 4 0 0 1 8 0v4" /></>,
  info: <><circle {...P} cx="12" cy="12" r="9" /><path {...P} d="M12 8v5M12 16h.01" /></>,
  settings: <><circle {...P} cx="12" cy="12" r="2.8" /><path {...P} d="M19.4 13.5a7.6 7.6 0 0 0 0-3l1.8-1.4-1.8-3.1-2.2.8a7.5 7.5 0 0 0-2.6-1.5L14.2 3h-4.4l-.4 2.3a7.5 7.5 0 0 0-2.6 1.5l-2.2-.8-1.8 3.1 1.8 1.4a7.6 7.6 0 0 0 0 3l-1.8 1.4 1.8 3.1 2.2-.8a7.5 7.5 0 0 0 2.6 1.5l.4 2.3h4.4l.4-2.3a7.5 7.5 0 0 0 2.6-1.5l2.2.8 1.8-3.1z" /></>,
  camera: <><path {...P} d="M4 7h3l2-2.5h6L17 7h3a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1z" /><circle {...P} cx="12" cy="13" r="3.5" /></>,
  sparkle: <path {...P} d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5 18 18M6 18l2.5-2.5M15.5 8.5 18 6" />,
  sliders: <><path {...P} d="M4 7h10M18 7h2M4 17h4M12 17h8" /><circle {...P} cx="16" cy="7" r="2" /><circle {...P} cx="10" cy="17" r="2" /></>,
  mapPin: <><path {...P} d="M12 21s-6.5-5.8-6.5-11a6.5 6.5 0 0 1 13 0c0 5.2-6.5 11-6.5 11z" /><circle {...P} cx="12" cy="10" r="2.3" /></>,
  nodes: <><circle {...P} cx="6" cy="17" r="2.3" /><circle {...P} cx="18" cy="7" r="2.3" /><path {...P} d="M8 15.5 16 8.5" /></>,
  pawLine: <><ellipse {...P} cx="6.6" cy="10" rx="1.6" ry="2" /><ellipse {...P} cx="10" cy="6.3" rx="1.6" ry="2.1" /><ellipse {...P} cx="14" cy="6.3" rx="1.6" ry="2.1" /><ellipse {...P} cx="17.4" cy="10" rx="1.6" ry="2" /><path {...P} d="M12 11.5c-2.6 0-5 3.5-5 5.7 0 1.2.9 2 2.1 2 1 0 1.8-.5 2.9-.5s1.9.5 2.9.5c1.2 0 2.1-.8 2.1-2 0-2.2-2.4-5.7-5-5.7z" /></>,
  share: <><circle {...P} cx="18" cy="5" r="2.5" /><circle {...P} cx="6" cy="12" r="2.5" /><circle {...P} cx="18" cy="19" r="2.5" /><path {...P} d="m8.2 10.8 7.6-4.4M8.2 13.2l7.6 4.4" /></>,
}

export function Icon({ name, size = 22, color }: { name: keyof typeof paths | string; size?: number; color?: string }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" style={{ color, flex: 'none' }} aria-hidden>{paths[name] ?? null}</svg>
}

export function Scene({ img, h = 64, w, radius = 14, children, shade }: { img: string; h?: number; w?: number | string; radius?: number; children?: ReactNode; shade?: boolean }) {
  return (
    <div className={`scene ${img}`} style={{ height: h, width: w ?? '100%', borderRadius: radius, flex: w ? 'none' : undefined }}>
      {shade && <div className="shade" />}
      {children}
    </div>
  )
}

/** 확인된 정보만 확정 — 공식(확인됨)과 이용자 확인(멍슐랭)을 분리해 표시 */
export function PlaceBadges({ p, max = 2, showUser }: { p: Place; max?: number; showUser?: boolean }) {
  return (
    <div className="badges">
      {p.verification === 'VERIFIED'
        ? p.conditions.slice(0, max).map((c) => <span key={c} className="badge ok">{c}</span>)
        : <span className="badge chk">방문 전 확인 필요</span>}
      {showUser && p.mungchelin && p.mungchelin.count > 0 && <span className="badge user">이용자 확인 {p.mungchelin.count}건 · {p.mungchelin.lastAt}</span>}
    </div>
  )
}

export function TopBar({ title, onBack, right }: { title: ReactNode; onBack?: () => void; right?: ReactNode }) {
  return (
    <div className="topbar">
      {onBack && <button className="icon-btn" onClick={onBack} aria-label="뒤로"><Icon name="back" size={24} /></button>}
      <h1>{title}</h1>
      {right}
    </div>
  )
}

export function Toggle({ on, onChange, label }: { on: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <button className="row between" style={{ width: '100%', padding: '12px 0' }} onClick={() => onChange(!on)} role="switch" aria-checked={on}>
      <span style={{ fontSize: 15, textAlign: 'left' }}>{label}</span>
      <span className={`toggle ${on ? 'on' : ''}`} />
    </button>
  )
}

export function Progress({ step, total }: { step: number; total: number }) {
  return <div className="progress"><div style={{ width: `${(step / total) * 100}%` }} /></div>
}
