# Pleos Connect SDK·API 사용 정리 (차량용 앱)

정책: Pleos 공식 문서의 **AAOS Alignment** — 표준 데이터는 AAOS 표준 API, 제조사 특화 기능만 Pleos SDK.
SDK 시그니처는 공개 Maven(`https://nexus-playground.pleos.ai/repository/maven-releases/`) AAR의 public API로 확인된 것만 씁니다
(출처: puppy_log `docs/PLEOS_SDK_API.md`, 2026-11-12 `javap` 확인). CI `build-pleos` 잡이 실제 SDK와 함께 컴파일합니다.
**실제 콜백 값·단위·권한 승인은 Pleos Connect Emulator·차량에서 확인이 필요합니다.**

| 기능 | 사용하는 API | 코드 | 단계 |
|---|---|---|---|
| 목적지 길 안내 · 편한 길 | NaviHelper `requestRoute(RouteInfo(routeOption = COMFORTABLE))` | `src/pleos/.../PleosBindings.kt` | 1차 |
| 휴식 장소 경유지(최대 2곳)·교체 | NaviHelper `addWaypoint(RequestWaypointInfo, FIRST/SECOND)` · `removeWaypoint` | 〃 | 1차 |
| 남은 거리·도착 예정 | NaviHelper `onDrivingInfo(DrivingInfo.destination)` | 〃 | 1차 |
| 도착 → 다음 구간·여행 마무리 | NaviHelper `onDestinationArrived` | 〃 | 1차 |
| 음성 안내 | Gleo AI TextToSpeech `TextToSpeech(ctx, Mode.HYBRID).speak()` | 〃 | 1차 |
| 방향지시등 중 알림 대기 | Vehicle `getTurnSignal().registerTurnSignalState` | 〃 | 1차 |
| 뒷문 열림 → 휴식 시작 확인 | Vehicle `getDoor().registerDoorPosition(ROW_2_*)` | 〃 | 2차 |
| 뒷좌석 창문 안내(읽기만) | Vehicle `getWindow().getCurrentWindowPosition` (5초 조회) | 〃 | 2차 |
| 더운 날 에어컨 켜기(유일한 제어, 동의 시) | Vehicle `getHVAC().registerHvacAcState` · `setHvacAc` | 〃 | 2차 |
| 지원 여부 | Vehicle `checkXXXCapabilities` → 미지원 기능만 숨김 | 〃 | 1차 |
| 주행 시간·주행/정차/주차 | AAOS `PERF_VEHICLE_SPEED` · `GEAR_SELECTION` · `IGNITION_STATE` | `vehicle/AaosSources.kt` | 1차 |
| 연료 부족 → 주유 가능한 휴게소 먼저 | AAOS `FUEL_LEVEL` · `FUEL_LEVEL_LOW` · `RANGE_REMAINING` · `INFO_FUEL_CAPACITY` | 〃 | 1차 |
| 바깥 기온 | AAOS `ENV_OUTSIDE_TEMPERATURE` | 〃 | 1차 |
| 주행 중 화면 잠금 | `CarUxRestrictionsManager` (`isRequiresDistractionOptimization`) | 〃 | 1차 |

## 아직 연결하지 않은 것 (시그니처 미검증 → 대체 동작)
| 항목 | 지금 동작 |
|---|---|
| Fused Location SDK | Android 표준 위치(LocationManager) |
| NaviHelper TBT·단속 구간(`onTBTInfo`·`onCameraAlert`) | 알림 판단에서 해당 조건만 건너뜀(`nextTurnMeters = null`) |
| NaviHelper `onWayPointArrived` | 데모에서 시뮬레이션, 실차는 도착 후 주차(GEAR=P)로 휴식 화면 |
| Gleo AI SpeechToText | Android `RecognizerIntent`(데모 모드는 시연 문장) |
| Vehicle Odometer·Safety·EvBattery | 앱이 속도·기어로 주행 시간·거리 계산, 경고 조건은 건너뜀 |
| LLM SDK | 미사용(모바일 AI 일기는 서버 LLM 예정) |

## 빌드
```bash
cd vehicle-app
./gradlew :app:assembleDebug -Pmungroad.usePleosSdk=false   # 일반 에뮬레이터·휴대폰(데모 모드)
./gradlew :app:assembleRelease -Pmungroad.usePleosSdk=true  # Pleos Connect Emulator·차량
```
