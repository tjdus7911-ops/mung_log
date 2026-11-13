package com.mungroad.car.sdk

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.HvacAcStateListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.content.Context
import android.util.Log
import com.mungroad.car.data.Place
import com.mungroad.car.vehicle.CabinCapabilities
import com.mungroad.car.vehicle.CabinSource
import com.mungroad.car.vehicle.CabinState
import com.mungroad.car.vehicle.IntegrationLog
import com.mungroad.car.vehicle.NavEvent
import com.mungroad.car.vehicle.NavResult
import com.mungroad.car.vehicle.NavigationProvider
import com.mungroad.car.vehicle.Speaker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Pleos Connect SDK 연동 (mungroad.usePleosSdk=true 일 때만 컴파일).
 * 시그니처는 공개 Maven(nexus-playground.pleos.ai)의 AAR public API 로 확인한 것만 씁니다
 * (Vehicle 2.3.2 · NaviHelper 3.0.5 · TextToSpeech 3.3.1, docs/PLEOS_SDK.md).
 * 실제 콜백 값·단위·권한 승인은 Pleos Connect Emulator·차량에서 확인이 필요합니다.
 */
object PleosBindings {
    const val ENABLED = true
    private const val TAG = "MungRoadPleos"

    fun navigation(context: Context): NavigationProvider? =
        runCatching { PleosNavigation(context.applicationContext) }.onFailure { Log.w(TAG, "NaviHelper 실패", it) }.getOrNull()

    fun cabin(context: Context): CabinSource? =
        runCatching { PleosCabin(context.applicationContext) }.onFailure { Log.w(TAG, "Vehicle SDK 실패", it) }.getOrNull()

    fun speaker(context: Context): Speaker? =
        runCatching { PleosSpeaker(context.applicationContext) }.onFailure { Log.w(TAG, "TTS 실패", it) }.getOrNull()

    fun status(): String = "Pleos SDK 포함 빌드 · NaviHelper · Vehicle · TextToSpeech"
}

// ─────────────── NaviHelper: 목적지·경유지(최대 2곳)·편한 길·도착 ───────────────
private class PleosNavigation(context: Context) : NavigationProvider {
    override val name = "Pleos NaviHelper (내장 내비)"
    override val isReal = true
    private val navi = NaviHelper(context)
    private val events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 16)
    @Volatile private var lastName: String? = null

    private val listener = object : NaviHelperEventListener {
        override fun onDrivingInfo(drivingInfo: DrivingInfo) {
            val d = drivingInfo.destination
            // DrivingState.distance(m)·duration(초) — 단위는 에뮬레이터에서 확인
            events.tryEmit(NavEvent.Driving(d.distance / 1000.0, (d.duration / 60).toInt()))
        }
        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            IntegrationLog.event("NaviHelper.onDestinationArrived($lastName)")
            events.tryEmit(NavEvent.DestinationArrived(lastName))
        }
        override fun onRouteCancelled() {
            IntegrationLog.event("NaviHelper.onRouteCancelled")
            events.tryEmit(NavEvent.RouteCancelled)
        }
        override fun onError(errorCode: NaviErrorCode) {
            IntegrationLog.event("NaviHelper.onError($errorCode)")
        }
    }

    init {
        navi.initialize({
            navi.addListener(listener)
            IntegrationLog.api("NaviHelper.initialize → addListener")
        }, { e -> IntegrationLog.info("NaviHelper 초기화 실패: ${e.message} → 주소·좌표 안내로 대체") })
    }

    override fun events(): Flow<NavEvent> = events

    private fun poiId(p: Place) = "mungroad-${p.id}"

    override suspend fun requestRoute(place: Place, comfortable: Boolean): NavResult = runCatching {
        lastName = place.name
        val option = if (comfortable) RouteOption.COMFORTABLE else RouteOption.RECOMMENDED
        navi.requestRoute(RouteInfo(
            latitude = place.lat, longitude = place.lng, poiName = place.name, poiId = poiId(place),
            poiSubId = "0", address = place.address, routeOption = option,
        ))
        IntegrationLog.api("NaviHelper.requestRoute(${place.name}, $option)")
        events.tryEmit(NavEvent.RouteStarted(place.name))
        NavResult.RouteRequested
    }.getOrElse { NavResult.Failed("내비 요청 실패: ${it.message}") }

    override suspend fun addWaypoint(place: Place, slot: Int): NavResult = runCatching {
        val idx = if (slot == 0) WaypointIndex.FIRST else WaypointIndex.SECOND
        navi.addWaypoint(RequestWaypointInfo(
            latitude = place.lat, longitude = place.lng, poiName = place.name,
            waypointIndex = idx, poiId = poiId(place), address = place.address, poiSubId = "0",
        ))
        IntegrationLog.api("NaviHelper.addWaypoint(${place.name}, $idx)")
        NavResult.WaypointAdded
    }.getOrElse { NavResult.Failed("경유지 추가 실패: ${it.message}") }

    override suspend fun removeWaypoint(slot: Int): Boolean = runCatching {
        val idx = if (slot == 0) WaypointIndex.FIRST else WaypointIndex.SECOND
        navi.removeWaypoint(idx)
        IntegrationLog.api("NaviHelper.removeWaypoint($idx)")
        true
    }.getOrDefault(false)

    override suspend fun cancelRoute() { runCatching { navi.cancelRoute(); IntegrationLog.api("NaviHelper.cancelRoute()") } }

    override fun release() { runCatching { navi.removeListener(listener); navi.release() } }
}

// ─────────────── Vehicle SDK: 방향지시등·뒷문·뒷창·에어컨 (읽기 위주, 제어는 에어컨 하나) ───────────────
private class PleosCabin(context: Context) : CabinSource {
    private val vehicle = Vehicle(context).also { it.initialize() }
    private val rearWindows = listOf(WindowValue.ROW_2_LEFT, WindowValue.ROW_2_RIGHT)
    private val rearDoors = listOf(DoorValue.ROW_2_LEFT, DoorValue.ROW_2_RIGHT)
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)

    @Volatile override var capabilities = CabinCapabilities()
        private set

    init {
        IntegrationLog.api("Vehicle.initialize → checkXXXCapabilities")
        // 도메인별 지원 여부(차종별로 다름). 실패하면 해당 안내만 숨김
        vehicle.getWindow().checkWindowPositionCapabilities({ m ->
            capabilities = capabilities.copy(window = rearWindows.any { v -> m.keys.any { it.values.contains(v) } })
        }, { capabilities = capabilities.copy(window = false) })
        vehicle.getDoor().checkDoorPositionCapabilities({ m ->
            capabilities = capabilities.copy(door = rearDoors.any { v -> m.keys.any { it.values.contains(v) } })
        }, { capabilities = capabilities.copy(door = false) })
        vehicle.getHVAC().checkHvacAcCapabilities({ l ->
            capabilities = capabilities.copy(hvac = l.isNotEmpty(), hvacControl = l.isNotEmpty())
        }, { capabilities = capabilities.copy(hvac = false, hvacControl = false) })
        vehicle.getTurnSignal().checkTurnSignalStateCapabilities({ _ ->
            capabilities = capabilities.copy(turnSignal = true)
        }, { capabilities = capabilities.copy(turnSignal = false) })
    }

    override fun states(): Flow<CabinState> = callbackFlow {
        var s = CabinState()
        fun emit(n: CabinState) { s = n; trySend(n) }
        val doorPos = mutableMapOf<DoorValue, Int>()
        val doorListeners = rearDoors.map { v ->
            v to object : DoorPositionListener {
                override fun onPositionUpdated(p: Int?) {
                    doorPos[v] = p ?: 0
                    val open = doorPos.values.any { it > 0 }
                    if (open != s.rearDoorOpen) IntegrationLog.event("Door.onPositionUpdated → 뒷문 ${if (open) "열림" else "닫힘"}")
                    emit(s.copy(rearDoorOpen = open))
                }
                override fun onFailed(e: Exception) {}
            }
        }
        doorListeners.forEach { (v, l) -> runCatching { vehicle.getDoor().registerDoorPosition(DoorArea(v), l) } }
        val acListener = object : HvacAcStateListener {
            override fun onHvacAcStateUpdated(on: Boolean?) { emit(s.copy(acOn = on)) }
            override fun onFailed(e: Exception) {}
        }
        runCatching { vehicle.getHVAC().registerHvacAcState(driverSeat, acListener) }
        val turnListener = object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(v: TurnSignalValue?) {
                val on = v == TurnSignalValue.LEFT || v == TurnSignalValue.RIGHT
                if (on != s.turnSignalOn) IntegrationLog.event("TurnSignal → ${v ?: "OFF"}")
                emit(s.copy(turnSignalOn = on))
            }
            override fun onFailed(e: Exception) {}
        }
        runCatching { vehicle.getTurnSignal().registerTurnSignalState(turnListener) }
        // 창문 위치는 변경 리스너가 없어 5초마다 조회
        val poll = launch {
            while (true) {
                if (capabilities.window) {
                    val pos = rearWindows.map { v ->
                        suspendCancellableCoroutine<Int?> { c ->
                            vehicle.getWindow().getCurrentWindowPosition(WindowArea(v), { p -> if (c.isActive) c.resume(p) }, { if (c.isActive) c.resume(null) })
                        }
                    }
                    emit(s.copy(rearWindowOpenPct = pos.filterNotNull().maxOrNull()))
                }
                delay(5_000)
            }
        }
        awaitClose {
            poll.cancel()
            doorListeners.forEach { (v, l) -> runCatching { vehicle.getDoor().unregisterDoorPosition(DoorArea(v), l) } }
            runCatching { vehicle.getHVAC().unregisterHvacAcState(driverSeat, acListener) }
            runCatching { vehicle.getTurnSignal().unregisterTurnSignalState(turnListener) }
        }
    }

    override suspend fun setAc(on: Boolean): Boolean = suspendCancellableCoroutine { c ->
        IntegrationLog.api("HVAC.setHvacAc(ROW_1_LEFT, $on) — 보호자 동의")
        runCatching {
            vehicle.getHVAC().setHvacAc(driverSeat, on, { _, v -> if (c.isActive) c.resume(v == on) }, { if (c.isActive) c.resume(false) })
        }.onFailure { if (c.isActive) c.resume(false) }
    }

    override fun release() { runCatching { vehicle.release() } }
}

// ─────────────── Gleo AI TextToSpeech (HYBRID: 네트워크가 불안정해도 온디바이스) ───────────────
private class PleosSpeaker(context: Context) : Speaker {
    override val name = "Gleo AI TextToSpeech"
    private val tts = TextToSpeech(context, Mode.HYBRID)
    @Volatile private var ready = false

    init {
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() { ready = true; IntegrationLog.api("TextToSpeech.initialize(HYBRID) 성공") }
            override fun onFailure(errorType: ErrorType) { IntegrationLog.info("TTS 초기화 실패: $errorType") }
        })
    }

    override fun speak(text: String) {
        IntegrationLog.api("TextToSpeech.speak(\"$text\")")
        if (ready) runCatching { tts.speak(text, true) }
    }
    override fun stop() { runCatching { tts.stop() } }
    override fun release() { runCatching { tts.release() } }
}
