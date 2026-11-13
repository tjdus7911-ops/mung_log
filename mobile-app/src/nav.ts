import { createContext, useContext } from 'react'

export type Tab = 'home' | 'explore' | 'trips' | 'community' | 'my'

export type Route =
  | { name: 'place'; id: string }
  | { name: 'tripBuilder'; fromPostId?: string; placeId?: string }
  | { name: 'trip'; id: string }
  | { name: 'feed'; id: string }
  | { name: 'mungchelin'; placeId: string }
  | { name: 'share'; recordId: string }
  | { name: 'record'; id: string }
  | { name: 'diary'; id: string }
  | { name: 'pet'; id?: string }
  | { name: 'rest'; petId: string }
  | { name: 'carLink' }

export interface Nav {
  tab: Tab
  setTab: (t: Tab) => void
  push: (r: Route) => void
  back: () => void
  toast: (msg: string) => void
}

export const NavContext = createContext<Nav | null>(null)
export function useNav(): Nav {
  const n = useContext(NavContext)
  if (!n) throw new Error('NavContext 없음')
  return n
}
