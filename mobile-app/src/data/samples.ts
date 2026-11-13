import type { FeedPost, Pet, Place, Trip, TripRecord } from './models'

// 시연용 샘플 데이터(차량 앱 SampleData.kt 와 같은 장소). 장소명·거리·운영 시간은 예시입니다.
const TOUR = '한국관광공사 반려동물 동반여행'
const EX = '한국도로공사 휴게시설 정보'

export const SAMPLE_PET: Pet = {
  id: 'pet-1', name: '사슴이', breed: '믹스', birthYear: 2017, birthMonth: 5, weightKg: 12,
  brachycephalic: false, activity: '활발함', caution: '차멀미가 있어요 · 물은 조금씩', health: ['NONE'],
}

export const PLACES: Place[] = [
  {
    id: 'p-cafe', name: '바다숲 카페', category: 'CAFE', region: '강릉', lat: 37.779, lng: 128.908, verification: 'VERIFIED',
    conditions: ['중형견 가능', '실내 동반 가능', '목줄 필수'], restrictions: ['대형견 불가'], sizes: ['소형견', '중형견'], indoor: true, leash: true,
    source: TOUR, verifiedAt: '2026.11.02', hours: '10:00 – 21:00', closedDays: '매주 화요일', phone: '033-000-0000', image: 'cafe',
    mungchelin: {
      count: 12, lastAt: '11.08',
      items: [{ label: '목줄 필수', yes: 11, no: 0 }, { label: '실내 동반', yes: 9, no: 1 }, { label: '물그릇 있음', yes: 7, no: 2 }],
      reviews: [
        { author: '보리 보호자', text: '테라스 그늘이 있어 여름에도 좋아요. 물그릇 있어요.', visitVerified: true },
        { author: '초코 보호자', text: '실내는 중형견까지만 돼요. 주말엔 붐벼요.', visitVerified: false },
      ],
    },
  },
  {
    id: 'p-dogpark', name: '해피독 파크', category: 'PARK', region: '강릉', lat: 37.77, lng: 128.89, verification: 'CHECK_NEEDED',
    conditions: [], restrictions: [], sizes: [], indoor: false, leash: false, phone: '033-000-0001', image: 'dogpark',
    mungchelin: { count: 3, lastAt: '10.30', items: [{ label: '대형견 가능', yes: 2, no: 1 }], reviews: [] },
  },
  {
    id: 'p-pine', name: '솔밭 공원', category: 'PARK', region: '강릉', lat: 37.785, lng: 128.9, verification: 'VERIFIED',
    conditions: ['대형견 가능', '야외 공간', '목줄 필수'], restrictions: [], sizes: ['소형견', '중형견', '대형견'], indoor: false, leash: true,
    source: TOUR, verifiedAt: '2026.10.21', hours: '상시 개방', image: 'park',
  },
  {
    id: 'p-bakery', name: '파도 베이커리', category: 'CAFE', region: '강릉', lat: 37.791, lng: 128.915, verification: 'CHECK_NEEDED',
    conditions: [], restrictions: [], sizes: [], indoor: false, leash: false, phone: '033-000-0002', image: 'cafe',
  },
  {
    id: 'p-food', name: '바다 식당', category: 'RESTAURANT', region: '강릉', lat: 37.796, lng: 128.918, verification: 'VERIFIED',
    conditions: ['중형견 가능', '야외 테라스'], restrictions: [], sizes: ['소형견', '중형견'], indoor: false, leash: true,
    source: TOUR, verifiedAt: '2026.10.30', hours: '11:00 – 20:30', image: 'cafe',
  },
  {
    id: 'p-stay', name: '솔향 펜션', category: 'STAY', region: '강릉', lat: 37.8, lng: 128.9, verification: 'VERIFIED',
    conditions: ['소·중형견 가능', '객실 동반'], restrictions: [], sizes: ['소형견', '중형견'], indoor: true, leash: false,
    source: TOUR, verifiedAt: '2026.09.14', image: 'park',
  },
  {
    id: 'p-beach', name: '해변 산책로', category: 'PARK', region: '강릉', lat: 37.805, lng: 128.908, verification: 'VERIFIED',
    conditions: ['야외', '목줄 필수'], restrictions: [], sizes: ['소형견', '중형견', '대형견'], indoor: false, leash: true,
    source: TOUR, verifiedAt: '2026.10.12', image: 'sea',
  },
  {
    id: 'p-dest', name: '강릉 경포해변', category: 'PARK', region: '강릉', lat: 37.8056, lng: 128.9086, verification: 'VERIFIED',
    conditions: ['야외', '목줄 필수'], restrictions: [], sizes: ['소형견', '중형견', '대형견'], indoor: false, leash: true,
    source: TOUR, verifiedAt: '2026.10.12', image: 'sea',
  },
  {
    id: 'p-yp-cafe', name: '숲길 카페', category: 'CAFE', region: '양평', lat: 37.49, lng: 127.49, verification: 'VERIFIED',
    conditions: ['소형견 가능', '야외 테라스'], restrictions: ['실내 동반 불가'], sizes: ['소형견'], indoor: false, leash: true,
    source: TOUR, verifiedAt: '2026.10.02', image: 'park',
  },
  {
    id: 'r-okgye', name: '옥계휴게소', category: 'REST_AREA', region: '동해고속도로', lat: 37.62, lng: 129.04, verification: 'VERIFIED',
    conditions: ['반려견 놀이터', '주유 가능'], restrictions: [], sizes: ['소형견', '중형견', '대형견'], indoor: false, leash: true, source: EX, verifiedAt: '2025.06', image: 'dogpark',
  },
  {
    id: 'r-gangneung', name: '강릉휴게소', category: 'REST_AREA', region: '영동고속도로', lat: 37.723, lng: 128.838, verification: 'VERIFIED',
    conditions: ['반려견 놀이터', '전기차 충전'], restrictions: [], sizes: ['소형견', '중형견', '대형견'], indoor: false, leash: true, source: EX, verifiedAt: '2025.06', image: 'dogpark',
  },
]

export const placeMap: Record<string, Place> = Object.fromEntries(PLACES.map((p) => [p.id, p]))

export const SAMPLE_TRIPS: Trip[] = [
  {
    id: 't-1', name: '사슴이와 강릉 바다', startDate: '2026-11-15', endDate: '2026-11-16', origin: '집', destinationId: 'p-dest',
    waypointIds: ['p-bakery'], petIds: ['pet-1'], restPlaceIds: ['r-okgye'], sentToCar: true, createdAt: 1,
  },
]

export const FEED: FeedPost[] = [
  { id: 'f-1', author: '보리 보호자', dogSize: '중형견', title: '강릉 바다 반나절 코스', region: '강원', theme: '바다', totalKm: 128, stopIds: ['p-beach', 'p-bakery', 'p-food'], restIds: ['r-okgye'], likes: 24, image: 'sea', createdAt: '11.08', visibility: 'public' },
  { id: 'f-2', author: '초코 보호자', dogSize: '소형견', title: '양평 숲길 카페 코스', region: '경기', theme: '자연', totalKm: 87, stopIds: ['p-pine', 'p-yp-cafe'], restIds: [], likes: 18, image: 'park', createdAt: '11.02', visibility: 'public' },
]

/** 차량 앱의 '여행 마무리'로 저장된 기록(샘플) */
export const SAMPLE_RECORDS: TripRecord[] = [
  {
    id: 'rec-1', tripName: '사슴이와 강릉 바다', dateLabel: '11월 15일 – 16일', drivingMinutes: 348, distanceKm: 452,
    timeline: [
      { time: '08:40', kind: 'start', label: '집 출발', photos: [] },
      { time: '10:42', kind: 'rest', placeId: 'r-okgye', label: '옥계휴게소', minutes: 12, photos: ['dogpark'] },
      { time: '11:30', kind: 'visit', placeId: 'p-beach', label: '해변 산책로', photos: ['sea', 'sea', 'park'] },
      { time: '13:05', kind: 'visit', placeId: 'p-cafe', label: '바다숲 카페', photos: ['cafe'] },
      { time: '18:20', kind: 'end', label: '집 도착 · 여행 마무리', photos: [] },
    ],
    memo: ['카페에서 물을 많이 마심'],
  },
]
