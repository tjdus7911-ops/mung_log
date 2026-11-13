// 차량 앱(vehicle-app/.../data/Models.kt)과 같은 구조 — 계정 동기화 단위
export type Health = 'NONE' | 'JOINT' | 'HEART' | 'RESPIRATORY' | 'KIDNEY' | 'DIABETES'
export const HEALTH_LABEL: Record<Health, string> = {
  NONE: '특이사항 없음', JOINT: '관절', HEART: '심장', RESPIRATORY: '호흡기', KIDNEY: '신장 질환', DIABETES: '당뇨',
}

export interface Pet {
  id: string
  name: string
  breed: string
  birthYear: number
  birthMonth: number
  weightKg: number
  brachycephalic: boolean
  activity: '차분함' | '보통' | '활발함'
  caution: string
  /** 기기에만 저장 — 휴식 간격 계산에만 사용, 동기화하지 않음 */
  health: Health[]
  customRestMinutes?: number
  vetRestMinutes?: number
  /** 프로필 사진(dataURL, 휴대폰에 저장). 없으면 기본 사진 또는 발바닥 아이콘 */
  photo?: string
}

export type Category = 'CAFE' | 'RESTAURANT' | 'PARK' | 'STAY' | 'FACILITY' | 'REST_AREA'
export const CATEGORY_LABEL: Record<Category, string> = {
  CAFE: '카페', RESTAURANT: '식당', PARK: '공원', STAY: '숙소', FACILITY: '동반 시설', REST_AREA: '휴게소',
}

export type Verification = 'VERIFIED' | 'CHECK_NEEDED'

export interface MungchelinSummary {
  count: number
  lastAt: string
  items: { label: string; yes: number; no: number }[]
  reviews: { author: string; text: string; visitVerified: boolean }[]
}

export interface Place {
  id: string
  name: string
  category: Category
  region: string
  lat: number
  lng: number
  verification: Verification
  conditions: string[]
  restrictions: string[]
  sizes: ('소형견' | '중형견' | '대형견')[]
  indoor: boolean
  leash: boolean
  source?: string
  verifiedAt?: string
  hours?: string
  closedDays?: string
  phone?: string
  image: string
  mungchelin?: MungchelinSummary
}

export interface Trip {
  id: string
  name: string
  startDate: string
  endDate: string
  origin: string
  destinationId: string
  waypointIds: string[]
  petIds: string[]
  restPlaceIds: string[]
  sentToCar: boolean
  fromFeedPostId?: string
  createdAt: number
}

/** 차량 앱이 만든 주행·휴식·방문 기록(DriveSession·RestSession) + 휴대폰 사진 */
export interface TripRecord {
  id: string
  tripName: string
  dateLabel: string
  drivingMinutes: number
  distanceKm: number
  timeline: { time: string; kind: 'start' | 'rest' | 'visit' | 'end'; placeId?: string; label: string; minutes?: number; photos: string[] }[]
  memo: string[]
  diary?: { pov: 'pet' | 'owner'; title: string; body: string; savedAt: number }
}

export interface FeedPost {
  id: string
  author: string
  dogSize: '소형견' | '중형견' | '대형견'
  title: string
  region: string
  theme: string
  totalKm: number
  stopIds: string[]
  restIds: string[]
  likes: number
  image: string
  createdAt: string
  visibility: 'public' | 'followers' | 'private'
}
