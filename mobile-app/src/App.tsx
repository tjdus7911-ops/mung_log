import { useCallback, useMemo, useRef, useState } from 'react'
import { NavContext, type Route, type Tab } from './nav'
import { useStore } from './store'
import { Icon } from './ui'
import { HomeScreen, ExploreScreen, PlaceScreen } from './screens/browse'
import { TripsScreen, TripBuilderScreen, TripDetailScreen } from './screens/trips'
import { CommunityScreen, FeedDetailScreen, MungchelinScreen, ShareScreen, RecordScreen, DiaryScreen } from './screens/community'
import { MyScreen, PetScreen, RestScreen, CarLinkScreen, Onboarding } from './screens/my'

const TABS: { id: Tab; label: string; icon: string }[] = [
  { id: 'home', label: '홈', icon: 'home' },
  { id: 'explore', label: '탐색', icon: 'search' },
  { id: 'trips', label: '여행', icon: 'calendar' },
  { id: 'community', label: '커뮤니티', icon: 'users' },
  { id: 'my', label: '마이', icon: 'paw' },
]

export default function App() {
  const s = useStore()
  const [tab, setTabState] = useState<Tab>('home')
  const [stack, setStack] = useState<Route[]>([])
  const [toastMsg, setToast] = useState<string | null>(null)
  const timer = useRef<number>()

  const toast = useCallback((m: string) => {
    setToast(m)
    window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => setToast(null), 2600)
  }, [])

  const nav = useMemo(() => ({
    tab,
    setTab: (t: Tab) => { setStack([]); setTabState(t); window.scrollTo(0, 0) },
    push: (r: Route) => { setStack((st) => [...st, r]); window.scrollTo(0, 0) },
    back: () => setStack((st) => st.slice(0, -1)),
    toast,
  }), [tab, toast])

  if (!s.onboarded) {
    return <NavContext.Provider value={nav}><div className="phone"><Onboarding />{toastMsg && <div className="toast">{toastMsg}</div>}</div></NavContext.Provider>
  }

  const top = stack[stack.length - 1]
  let body
  if (top) {
    switch (top.name) {
      case 'place': body = <PlaceScreen id={top.id} />; break
      case 'tripBuilder': body = <TripBuilderScreen fromPostId={top.fromPostId} placeId={top.placeId} />; break
      case 'trip': body = <TripDetailScreen id={top.id} />; break
      case 'feed': body = <FeedDetailScreen id={top.id} />; break
      case 'mungchelin': body = <MungchelinScreen placeId={top.placeId} />; break
      case 'share': body = <ShareScreen recordId={top.recordId} />; break
      case 'record': body = <RecordScreen id={top.id} />; break
      case 'diary': body = <DiaryScreen id={top.id} />; break
      case 'pet': body = <PetScreen id={top.id} />; break
      case 'rest': body = <RestScreen petId={top.petId} />; break
      case 'carLink': body = <CarLinkScreen />; break
    }
  } else {
    body = { home: <HomeScreen />, explore: <ExploreScreen />, trips: <TripsScreen />, community: <CommunityScreen />, my: <MyScreen /> }[tab]
  }
  const showTabs = !top || top.name === 'place' || top.name === 'trip' || top.name === 'record'

  return (
    <NavContext.Provider value={nav}>
      <div className="phone">
        {body}
        {showTabs && (
          <nav className="tabbar">
            {TABS.map((t) => (
              <button key={t.id} className={tab === t.id ? 'on' : ''} onClick={() => nav.setTab(t.id)}>
                <Icon name={t.icon} size={24} />
                {t.label}
              </button>
            ))}
          </nav>
        )}
        {toastMsg && <div className="toast" role="status">{toastMsg}</div>}
      </div>
    </NavContext.Provider>
  )
}
