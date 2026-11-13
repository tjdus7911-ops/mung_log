# 차량 앱 ↔ 모바일 앱 동기화 계약 (2차 · 서버 미구현)

모바일 앱은 Pleos SDK를 쓰지 않고, 같은 계정의 서버를 거쳐 차량 앱과 데이터를 주고받습니다.
서버 주소가 없으면(현재) 모바일은 보낼 항목을 기기 안 보낼함(outbox)에 쌓고 화면에 ‘동기화 서버 연결 전’을 표시합니다.

| 방향 | 엔드포인트 | 내용 | 비고 |
|---|---|---|---|
| 휴대폰 → 서버 → 차 | `POST /api/v1/pets` | 반려견 프로필 + 계산된 휴식 간격 | **건강 상태는 보내지 않음**(기기에만 저장) |
| 휴대폰 → 차 | `POST /api/v1/trips` | 여행(목적지·경유지 최대 2곳·반려견·휴식 장소) | 차량 홈 ‘다음 여행’ |
| 차 → 휴대폰 | `POST /api/v1/records` | 여행 마무리 때 DriveSession·RestSession·방문지 | 정확한 경로선은 기기에 둠 |
| 휴대폰 → 서버 | `POST /api/v1/places/{id}/mungchelin` | 멍슐랭 체크·후기 | 공식 정보로 승격하지 않음 |
| 휴대폰 → 서버 | `POST /api/v1/feed` | 코스 공유(공개 범위) | 집 주변 출발지·EXIF 제거 |
| 휴대폰 → 서버 | `POST /api/v1/diary` | 기록된 사실 목록 → AI 일기 초안 | 사실 밖 내용 생성 금지, 결과 검사 |

설정: 모바일 `VITE_MUNGROAD_API_BASE_URL`, 차량 `MUNGROAD_API_BASE_URL`(local.properties).
