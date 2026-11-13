package com.mungroad.car.vehicle

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import com.mungroad.car.data.Place
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.util.Locale

/**
 * 데모 모드 가상 주행 — 차량이 연결되지 않아도 같은 흐름을 시연합니다(VehicleDataProvider 와 같은 인터페이스).
 * 서울 → 강릉 약 225km, 배속 조절. 연료는 주행할수록 줄어들어 '주유 가능한 휴게소 먼저' 를 보여 줍니다.
 */
class DemoVehicleDataProvider(startMillis: Long = System.currentTimeMillis()) : VehicleDataProvider {
    override val name = "데모 모드 (가상 주행)"
    override val isSimulated = true

    val speedMultiplier = MutableStateFlow(60)
    val outsideTemp = MutableStateFlow(24f)
    val forceTurnSignal = MutableStateFlow(false)

    @Volatile private var simTime = startMillis
    @Volatile private var resting = false
    @Volatile var hold = false
    private var km = 0.0
    private var speed = 0.0
    private var fuel = 34.0

    override fun now() = simTime
    override fun onRest(resting: Boolean) { this.resting = resting }

    override fun snapshots(): Flow<VehicleSnapshot> = flow {
        emit(snapshot(Gear.PARK))
        while (currentCoroutineContext().isActive) {
            delay(250)
            val steps = maxOf(1, speedMultiplier.value / 4)
            repeat(steps) {
                simTime += 1000
                val target = if (resting || hold || km >= DemoRoute.totalKm) 0.0 else DemoRoute.targetSpeed(km)
                speed += (target - speed).coerceIn(-6.0, 6.0)
                km = (km + speed / 3600).coerceAtMost(DemoRoute.totalKm)
                fuel = (fuel - speed / 3600 * 0.12).coerceAtLeast(5.0)
            }
            val gear = when {
                resting || km >= DemoRoute.totalKm -> Gear.PARK
                else -> Gear.DRIVE
            }
            emit(snapshot(gear))
        }
    }

    private fun snapshot(gear: Gear): VehicleSnapshot {
        val (lat, lng) = DemoRoute.pointAt(km)
        return VehicleSnapshot(
            timeMillis = simTime, speedKmh = speed.toFloat(), gear = gear, ignitionOn = true,
            lat = lat, lng = lng, accuracyM = 8f,
            fuelPercent = fuel.toFloat(), fuelLow = fuel < 20, rangeKm = (fuel * 9).toFloat(),
            outsideTempC = outsideTemp.value,
        )
    }

    val progressKm: Double get() = km
    val remainingKm: Double get() = (DemoRoute.totalKm - km).coerceAtLeast(0.0)
}

object DemoRoute {
    val points: List<Pair<Double, Double>> = listOf(
        37.5665 to 126.978, 37.5172 to 127.0473, 37.4685 to 127.039, 37.4 to 127.1, 37.285 to 127.106,
        37.24 to 127.2, 37.23 to 127.28, 37.25 to 127.46, 37.28 to 127.63, 37.31 to 127.82, 37.38 to 127.92,
        37.46 to 128.12, 37.54 to 128.33, 37.63 to 128.56, 37.68 to 128.72, 37.71 to 128.8, 37.7519 to 128.8761,
        37.8056 to 128.9086,
    )
    private val cum: List<Double> = buildList {
        add(0.0)
        for (i in 1 until points.size) add(last() + com.mungroad.car.domain.DriveTracker.haversineKm(points[i - 1].first, points[i - 1].second, points[i].first, points[i].second))
    }
    val totalKm = cum.last()

    fun targetSpeed(km: Double) = when {
        km < 15 -> 40.0
        km < totalKm - 25 -> 98.0
        km < totalKm - 8 -> 70.0
        else -> 35.0
    }

    fun pointAt(km: Double): Pair<Double, Double> {
        val d = km.coerceIn(0.0, totalKm)
        var i = 1
        while (i < cum.size - 1 && cum[i] < d) i++
        val t = (d - cum[i - 1]) / (cum[i] - cum[i - 1]).coerceAtLeast(1e-9)
        val a = points[i - 1]; val b = points[i]
        return (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
    }

    /** 데모 경로 위 휴게소 위치(km 지점) */
    val restAreaKm = mapOf("r-pyeongchang" to 150.0, "r-gangneung" to 205.0, "r-okgye" to 190.0, "r-shade" to 120.0)
}

/** 데모·일반 기기용 내비: 내장 내비 대신 이벤트만 흉내 내고, 실제 기기에서는 지도 앱 Intent 로 넘깁니다 */
class FallbackNavigationProvider(private val context: Context, private val demo: DemoVehicleDataProvider?) : NavigationProvider {
    override val name = if (demo != null) "데모 내비 (NaviHelper 시뮬레이션)" else "지도 앱 연결 (내비 미탑재 대체)"
    override val isReal = false
    private val events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 16)
    private val waypoints = arrayOfNulls<Place>(2)
    override fun events(): Flow<NavEvent> = events

    override suspend fun requestRoute(place: Place, comfortable: Boolean): NavResult {
        IntegrationLog.api("NaviHelper.requestRoute(${place.name}, ${if (comfortable) "COMFORTABLE" else "RECOMMENDED"}) [시뮬레이션]")
        if (demo == null) {
            runCatching {
                val uri = Uri.parse("geo:${place.lat},${place.lng}?q=${place.lat},${place.lng}(${Uri.encode(place.name)})")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        events.tryEmit(NavEvent.RouteStarted(place.name))
        IntegrationLog.event("onRouteStarted(${place.name})")
        return NavResult.RouteRequested
    }

    override suspend fun addWaypoint(place: Place, slot: Int): NavResult {
        if (slot !in 0..1) return NavResult.WaypointsFull
        waypoints[slot] = place
        IntegrationLog.api("NaviHelper.addWaypoint(${place.name}, ${if (slot == 0) "FIRST" else "SECOND"}) [시뮬레이션]")
        IntegrationLog.event("onWaypointChanged")
        return NavResult.WaypointAdded
    }

    override suspend fun removeWaypoint(slot: Int): Boolean {
        waypoints[slot.coerceIn(0, 1)] = null
        IntegrationLog.api("NaviHelper.removeWaypoint(${if (slot == 0) "FIRST" else "SECOND"}) [시뮬레이션]")
        return true
    }

    override suspend fun cancelRoute() { IntegrationLog.api("NaviHelper.cancelRoute() [시뮬레이션]"); events.tryEmit(NavEvent.RouteCancelled) }

    /** 데모 엔진이 호출: 도착 이벤트 */
    fun emit(e: NavEvent) { events.tryEmit(e) }
}

/** Gleo AI TTS 를 쓸 수 없을 때 Android 표준 TTS */
class AndroidSpeaker(context: Context) : Speaker {
    override val name = "Android TTS (대체)"
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }.also { it.language = Locale.KOREAN }

    override fun speak(text: String) {
        IntegrationLog.api("TTS.speak(\"$text\")")
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mungroad")
    }
    override fun stop() { tts.stop() }
    override fun release() { tts.shutdown() }
}
