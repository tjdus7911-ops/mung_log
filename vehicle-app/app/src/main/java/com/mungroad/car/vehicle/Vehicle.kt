package com.mungroad.car.vehicle

import com.mungroad.car.data.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

/** 차량 데이터 한 순간. 지원하지 않는 값은 null */
data class VehicleSnapshot(
    val timeMillis: Long,
    val speedKmh: Float? = null,
    val gear: Gear? = null,
    val ignitionOn: Boolean? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracyM: Float? = null,
    /** 연료 잔량 % (FUEL_LEVEL / 용량) */
    val fuelPercent: Float? = null,
    val fuelLow: Boolean? = null,
    /** 주행 가능 거리 km (RANGE_REMAINING) */
    val rangeKm: Float? = null,
    /** 전기차 배터리 % */
    val evBatteryPercent: Float? = null,
    /** 바깥 기온 ℃ (ENV_OUTSIDE_TEMPERATURE) */
    val outsideTempC: Float? = null,
)

/** 차량 데이터 출처. 한 주행 세션에는 하나만 연결해 실제·시뮬레이션 데이터가 섞이지 않게 합니다 */
interface VehicleDataProvider {
    val name: String
    val isSimulated: Boolean
    fun snapshots(): Flow<VehicleSnapshot>
    fun now(): Long = System.currentTimeMillis()
    /** 데모: 휴식 시작/종료를 알림(가상 차량을 세움) */
    fun onRest(resting: Boolean) {}
    fun release() {}
}

/** 주행 제한 상태 (CarUxRestrictionsManager · CarDrivingStateManager) */
enum class UxRestriction { NONE, DRIVING }

interface DrivingRestrictionSource {
    fun restrictions(): Flow<UxRestriction>
}

/** 뒷좌석·공조 등 차 안 상태 (Vehicle SDK Door·Window·HVAC·TurnSignal) */
data class CabinState(
    val turnSignalOn: Boolean? = null,
    val rearDoorOpen: Boolean? = null,
    val rearWindowOpenPct: Int? = null,
    val acOn: Boolean? = null,
)

data class CabinCapabilities(
    val turnSignal: Boolean = false,
    val door: Boolean = false,
    val window: Boolean = false,
    val hvac: Boolean = false,
    val hvacControl: Boolean = false,
)

interface CabinSource {
    val capabilities: CabinCapabilities
    fun states(): Flow<CabinState>
    /** 차량 제어는 이것 하나: 보호자가 동의했을 때만 에어컨 켜기 */
    suspend fun setAc(on: Boolean): Boolean
    fun release() {}
}

sealed class NavEvent {
    data class RouteStarted(val name: String?) : NavEvent()
    data class DestinationArrived(val name: String?) : NavEvent()
    data class WaypointArrived(val name: String?) : NavEvent()
    object RouteCancelled : NavEvent()
    object Rerouted : NavEvent()
    data class Turn(val meters: Int?) : NavEvent()
    data class Driving(val remainingKm: Double?, val remainingMinutes: Int?) : NavEvent()
}

sealed class NavResult {
    object RouteRequested : NavResult()
    object WaypointAdded : NavResult()
    object WaypointsFull : NavResult()
    data class Failed(val why: String) : NavResult()
}

/** 내장 내비게이션 연결 (NaviHelper). 멍로드는 자체 길 안내를 하지 않습니다(단일 경로·단일 내비) */
interface NavigationProvider {
    val name: String
    val isReal: Boolean
    fun events(): Flow<NavEvent>
    suspend fun requestRoute(place: Place, comfortable: Boolean): NavResult
    /** slot: 0 = FIRST, 1 = SECOND */
    suspend fun addWaypoint(place: Place, slot: Int): NavResult
    suspend fun removeWaypoint(slot: Int): Boolean
    suspend fun cancelRoute() {}
    fun release() {}
}

/** 음성 안내 (Gleo AI TextToSpeech → 미지원 시 Android TTS) */
interface Speaker {
    val name: String
    fun speak(text: String)
    fun stop() {}
    fun release() {}
}

/** 시연 화면 한쪽에 띄우는 '연동 로그' — 받은 이벤트와 호출한 API (서비스 화면에는 넣지 않음) */
object IntegrationLog {
    data class Entry(val time: String, val kind: String, val text: String)
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun api(text: String) = add("API", text)
    fun event(text: String) = add("EVENT", text)
    fun info(text: String) = add("INFO", text)

    @Synchronized
    private fun add(kind: String, text: String) {
        _entries.value = (listOf(Entry(fmt.format(Date()), kind, text)) + _entries.value).take(60)
    }
}
