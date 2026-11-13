import { useEffect, useState } from 'react'
import type { FeedPost, Pet, Trip, TripRecord } from './data/models'
import { FEED, SAMPLE_PET, SAMPLE_RECORDS, SAMPLE_TRIPS } from './data/samples'

/** 휴대폰에 저장되는 상태. 건강 정보(pet.health)는 기기에만 두고 동기화하지 않습니다 */
export interface AppData {
  onboarded: boolean
  pets: Pet[]
  trips: Trip[]
  favorites: string[]
  records: TripRecord[]
  feed: FeedPost[]
  mungchelin: { placeId: string; answers: Record<string, 'yes' | 'no' | 'unknown'>; review: string; at: string; visitVerified: boolean }[]
  car: { linked: boolean; name: string; lastSync?: string }
  consent: { location: boolean; sync: boolean }
  outbox: { kind: string; id: string; at: number }[]
}

const KEY = 'mungroad.v1'

const initial: AppData = {
  onboarded: false,
  pets: [SAMPLE_PET],
  trips: SAMPLE_TRIPS,
  favorites: ['p-cafe'],
  records: SAMPLE_RECORDS,
  feed: FEED,
  mungchelin: [],
  car: { linked: false, name: '' },
  consent: { location: false, sync: false },
  outbox: [],
}

function load(): AppData {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? { ...initial, ...JSON.parse(raw) } : initial
  } catch {
    return initial
  }
}

let state: AppData = typeof localStorage === 'undefined' ? initial : load()
const subs = new Set<(s: AppData) => void>()

export function getState() { return state }

export function setState(fn: (s: AppData) => AppData) {
  state = fn(state)
  try { localStorage.setItem(KEY, JSON.stringify(state)) } catch { /* 저장 공간 없음 */ }
  subs.forEach((f) => f(state))
}

export function resetState() { setState(() => initial) }

export function useStore(): AppData {
  const [s, set] = useState(state)
  useEffect(() => { subs.add(set); return () => { subs.delete(set) } }, [])
  return s
}

export const uid = (p: string) => `${p}-${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`
