# 멍로드 (MungRoad) — 반려견과 떠나는 모든 길

현대자동차그룹 차량용 앱 개발 공모전 출품작. 기획서 v4 · Figma(IA·기능정의서·와이어프레임·디자인) 기준으로 만든 두 앱입니다.

| | 차량용 앱 `vehicle-app/` | 모바일 앱 `mobile-app/` |
|---|---|---|
| 플랫폼 | Pleos Connect (Android Automotive) · Kotlin · Jetpack Compose | 휴대폰 설치형 웹앱(PWA) · React · TypeScript |
| 역할 | 차 안에서 바로 고르고, 출발하고, 쉬어 가기 | 반려견 프로필·여행 만들기·커뮤니티·여행 기록 |
| Pleos | NaviHelper · Vehicle · TextToSpeech + AAOS Car API · CarUxRestrictions | 사용 안 함(계정 동기화로 연결) |
| 화면 | 1600×900 · 홈 · 멍스팟 · 코스 탐색 · 내 여행 · 설정 · 드라이브 모드 | 390×844 · 홈 · 탐색 · 여행 · 커뮤니티 · 마이 |

## 차량용 앱
- 1차 MVP: 주변 반려견 동반 장소(확인된 정보만 확정), 장소 상세 → 내장 내비 길 안내(편한 길), 주행 감지 → 드라이브 모드,
  실제 주행 시간 기반 휴식 예고·안내(10분 전 + 도달), 휴식 장소 2~3곳(연료 부족 시 주유 가능 먼저) → 경유지 추가,
  ‘말해도 되는 순간’ 판단(방향지시등·대기 3분 초과 시 카드만), 휴식 중(펫 모드 안내), 도착·다음 구간, 주행 중 화면 잠금
- 2차·3차 일부: 더운 날 에어컨 제안, 뒷좌석 창문 안내, 담아 온 코스, 멍슐랭 요약·빠른 확인, 여행 마무리(집 도착)
- **데모 모드**: 차량 없이 서울→강릉 가상 주행(배속·더운 날·방향지시등), 화면 오른쪽 ‘연동 로그’에 받은 이벤트·호출한 API 표시
- Pleos API 사용 내역·검증 상태: [docs/PLEOS_SDK.md](docs/PLEOS_SDK.md)

```bash
cd vehicle-app
cp local.properties.example local.properties   # sdk.dir 설정
./gradlew :app:testDebugUnitTest                  # 휴식 기준표·주행 시간·알림 타이밍·휴식 후보 테스트
./gradlew :app:assembleDebug   -Pmungroad.usePleosSdk=false   # 일반 에뮬레이터(데모)
./gradlew :app:assembleRelease -Pmungroad.usePleosSdk=true    # Pleos Connect Emulator·차량
```
GitHub Actions `vehicle-app` 가 두 APK를 아티팩트로 올립니다(`mungroad-car-demo-debug`, `mungroad-car-pleos-release`).

## 모바일 앱
- 온보딩(동의·반려견 등록·건강 상태·맞춤 휴식 간격), 홈, 탐색(크기·실내·확인된 곳 필터), 장소 상세(공식 확인 / 멍슐랭 분리)
- 여행 만들기 8단계(경유지 최대 2곳, 휴식 지점 미리보기) → 차로 보내기
- 커뮤니티: 여행 피드·코스 담기·멍슐랭 남기기(방문 인증)·코스 공유(집 주변·EXIF 제거) · 여행 기록 · AI 여행 일기(기록된 사실만)
- 동기화 서버 전에는 보낼 항목을 휴대폰에 보관(보낸 것처럼 꾸미지 않음): [docs/SYNC_API.md](docs/SYNC_API.md)

```bash
cd mobile-app && npm ci && npm test && npm run dev
```

## 데이터
장소·휴게소·코스·여행 기록은 시연용 **샘플 데이터**입니다. 서비스에서는 한국관광공사 반려동물 동반여행 OpenAPI·한국도로공사 휴게시설 정보를
서버(키는 서버에 보관)를 통해 받습니다. 휴식 간격 기준표는 초안이며 출시 전 수의사 자문으로 확정합니다.
