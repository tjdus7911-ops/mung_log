package com.mungroad.car.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mungroad.car.BuildConfig
import com.mungroad.car.data.Category
import com.mungroad.car.data.Course
import com.mungroad.car.data.Pet
import com.mungroad.car.data.Place
import com.mungroad.car.data.RestStartMethod
import com.mungroad.car.data.SampleData
import com.mungroad.car.data.Trip
import com.mungroad.car.domain.DriveState
import com.mungroad.car.domain.DriveTracker
import com.mungroad.car.domain.DriverContext
import com.mungroad.car.domain.GateDecision
import com.mungroad.car.domain.Notice
import com.mungroad.car.domain.NotificationGate
import com.mungroad.car.domain.Priority
import com.mungroad.car.domain.RestCandidate
import com.mungroad.car.domain.RestCandidates
import com.mungroad.car.domain.RestEngine
import com.mungroad.car.domain.RestGuideline
import com.mungroad.car.sdk.PleosBindings
import com.mungroad.car.vehicle.AaosRestrictionSource
import com.mungroad.car.vehicle.AaosVehicleDataProvider
import com.mungroad.car.vehicle.AndroidSpeaker
import com.mungroad.car.vehicle.CabinCapabilities
import com.mungroad.car.vehicle.CabinSource
import com.mungroad.car.vehicle.CabinState
import com.mungroad.car.vehicle.DemoRoute
import com.mungroad.car.vehicle.DemoVehicleDataProvider
import com.mungroad.car.vehicle.FallbackNavigationProvider
import com.mungroad.car.vehicle.IntegrationLog
import com.mungroad.car.vehicle.NavEvent
import com.mungroad.car.vehicle.NavResult
import com.mungroad.car.vehicle.NavigationProvider
import com.mungroad.car.vehicle.Speaker
import com.mungroad.car.vehicle.UxRestriction
import com.mungroad.car.vehicle.VehicleDataProvider
import com.mungroad.car.vehicle.VehicleSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Menu(val label: String) { HOME("홈"), COURSE("코스 탐색"), SPOT("멍스팟"), TRIPS("내 여행"), SETTINGS("설정") }

sealed class Screen {
    object Home : Screen()
    object Spots : Screen()
    data class PlaceDetail(val place: Place) : Screen()
    object Courses : Screen()
    data class CourseDetail(val course: Course) : Screen()
    object Trips : Screen()
    data class TripConfirm(val trip: Trip) : Screen()
    object Settings : Screen()
    object Drive : Screen()
    object Resting : Screen()
    data class Arrived(val name: String) : Screen()
    object WrapUp : Screen()
    data class QuickCheck(val place: Place) : Screen()
}

/** 드라이브 모드 카드 — 주행 중에는 음성 한 문장 + 큰 버튼 2개, 전체 화면 팝업 없음 */
sealed class DriveCard {
    data class PreNotice(val text: String) : DriveCard()
    data class RestDue(val title: String, val sub: String) : DriveCard()
    data class Suggestions(val items: List<RestCandidate>, val fuelLow: Boolean) : DriveCard()
    data class ReplaceWaypoint(val newPlace: Place, val replace: Place) : DriveCard()
    data class AirCon(val tempC: Int) : DriveCard()
    data class Window(val pct: Int) : DriveCard()
    data class VoiceResults(val query: String, val items: List<Place>) : DriveCard()
    data class Info(val text: String) : DriveCard()
}

data class ActiveRoute(
    val destination: Place,
    val waypoints: List<Place?> = listOf(null, null),
    val course: Course? = null,
    val courseLeg: Int = 0,
    val tripName: String? = null,
)

data class UiState(
    val menu: Menu = Menu.HOME,
    val screen: Screen = Screen.Home,
    val category: Category = Category.ALL,
    val pet: Pet = SampleData.saseumi,
    val restMinutes: Int = RestGuideline.DEFAULT_MINUTES,
    val restReason: String = "기본 주기",
    val useRecommended: Boolean = true,
    val notifyRest: Boolean = true,
    val notifySafety: Boolean = true,
    val voiceOn: Boolean = true,
    val comfortableRoute: Boolean = true,
    // 차량·연동 상태
    val demoMode: Boolean = true,
    val showLog: Boolean = true,
    val providerName: String = "",
    val navName: String = "",
    val speakerName: String = "",
    val sdkStatus: String = PleosBindings.status(),
    val cabinCaps: CabinCapabilities = CabinCapabilities(),
    val restriction: UxRestriction = UxRestriction.NONE,
    val lockedMessage: String? = null,
    // 주행
    val driveState: DriveState = DriveState.UNKNOWN,
    val snapshot: VehicleSnapshot? = null,
    val cabin: CabinState = CabinState(),
    val route: ActiveRoute? = null,
    val remainingKm: Double? = null,
    val etaText: String? = null,
    val drivingMinutes: Int = 0,
    val sinceRestMinutes: Int = 0,
    val distanceKm: Double = 0.0,
    val nextRest: RestCandidate? = null,
    val card: DriveCard? = null,
    val waitingReason: String? = null,
    val restStartedAt: Long? = null,
    val restsDone: Int = 0,
    val visited: List<String> = emptyList(),
    val pendingRestPrompt: Boolean = false,
    val demoSpeed: Int = 60,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    private val prefs = app.getSharedPreferences("mungroad", Context.MODE_PRIVATE)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var provider: VehicleDataProvider? = null
    private var demo: DemoVehicleDataProvider? = null
    private var nav: NavigationProvider? = null
    private var cabinSource: CabinSource? = null
    private var speaker: Speaker? = null
    private var vehicleJob: Job? = null
    private var navJob: Job? = null
    private var cabinJob: Job? = null

    private val tracker = DriveTracker()
    private val restEngine = RestEngine(RestGuideline.DEFAULT_MINUTES)
    private val gate = NotificationGate()
    private var turnSignalOffAt: Long? = null
    private var acSuggested = false
    private var windowWarned = false

    init {
        val demoDefault = !AaosVehicleDataProvider.isAutomotive(ctx)
        _ui.update {
            it.copy(
                demoMode = prefs.getBoolean("demo", demoDefault),
                notifyRest = prefs.getBoolean("notifyRest", true),
                notifySafety = prefs.getBoolean("notifySafety", true),
                voiceOn = prefs.getBoolean("voice", true),
                useRecommended = prefs.getBoolean("useRecommended", true),
                comfortableRoute = prefs.getBoolean("comfortable", true),
                pet = it.pet.copy(customRestMinutes = prefs.getInt("customRest", -1).takeIf { m -> m > 0 }),
            )
        }
        applyRestInterval()
        connect()
        viewModelScope.launch { restrictionLoop() }
        viewModelScope.launch { tickLoop() }
        IntegrationLog.info("멍로드 시작 · ${PleosBindings.status()}")
    }

    // ───────────────────────── 연결 (한 세션에 데이터 출처 하나) ─────────────────────────
    private fun connect() {
        vehicleJob?.cancel(); navJob?.cancel(); cabinJob?.cancel()
        provider?.release(); nav?.release(); cabinSource?.release()
        val demoMode = _ui.value.demoMode
        demo = if (demoMode) DemoVehicleDataProvider() else null
        provider = demo ?: AaosVehicleDataProvider(ctx)
        nav = (if (!demoMode) PleosBindings.navigation(ctx) else null) ?: FallbackNavigationProvider(ctx, demo)
        cabinSource = if (!demoMode) PleosBindings.cabin(ctx) else null
        if (speaker == null) speaker = PleosBindings.speaker(ctx) ?: AndroidSpeaker(ctx)
        _ui.update {
            it.copy(
                providerName = provider!!.name, navName = nav!!.name, speakerName = speaker!!.name,
                cabinCaps = cabinSource?.capabilities ?: CabinCapabilities(),
            )
        }
        IntegrationLog.info("데이터 출처: ${provider!!.name} / 내비: ${nav!!.name} / 음성: ${speaker!!.name}")
        vehicleJob = viewModelScope.launch {
            runCatching { provider!!.snapshots().collect { onSnapshot(it) } }
                .onFailure { e -> IntegrationLog.info("차량 데이터 연결 실패: ${e.message} → 데모 모드로 전환할 수 있어요") }
        }
        navJob = viewModelScope.launch { nav!!.events().collect { onNavEvent(it) } }
        cabinSource?.let { c ->
            cabinJob = viewModelScope.launch { c.states().collect { onCabin(it) } }
        }
    }

    fun setDemoMode(on: Boolean) {
        prefs.edit().putBoolean("demo", on).apply()
        _ui.update { UiState(demoMode = on, pet = it.pet, restMinutes = it.restMinutes, restReason = it.restReason) }
        tracker.restore(0, 0, 0.0); restEngine.reset()
        connect()
    }

    fun setDemoSpeed(x: Int) { demo?.speedMultiplier?.value = x; _ui.update { it.copy(demoSpeed = x) } }
    fun setDemoTemp(t: Float) { demo?.outsideTemp?.value = t; acSuggested = false }
    fun toggleDemoTurnSignal() {
        val d = demo ?: return
        d.forceTurnSignal.value = !d.forceTurnSignal.value
        if (!d.forceTurnSignal.value) turnSignalOffAt = d.now()
        IntegrationLog.event("TurnSignal(데모) → ${if (d.forceTurnSignal.value) "LEFT" else "OFF"}")
    }
    fun toggleLog() = _ui.update { it.copy(showLog = !it.showLog) }

    // ───────────────────────── 차량 신호 ─────────────────────────
    private fun onSnapshot(s: VehicleSnapshot) {
        val prev = tracker.state
        val st = tracker.update(s)
        _ui.update {
            it.copy(
                snapshot = s, driveState = st,
                drivingMinutes = (tracker.drivingMillis / 60_000).toInt(),
                sinceRestMinutes = (tracker.sinceRestMillis / 60_000).toInt(),
                distanceKm = tracker.distanceKm,
            )
        }
        // 주행 시작 감지 → 드라이브 모드 자동 전환(경로 안내 중일 때)
        if (st == DriveState.DRIVING && prev != DriveState.DRIVING && _ui.value.route != null) {
            if (_ui.value.screen !is Screen.Drive) {
                IntegrationLog.event("주행 시작 감지(GEAR_SELECTION·PERF_VEHICLE_SPEED) → 드라이브 모드")
                _ui.update { it.copy(screen = Screen.Drive) }
            }
        }
        val d = demo
        val route = _ui.value.route
        if (d != null && route != null) {
            // 데모: 경유지(휴게소) 도착
            route.waypoints.filterNotNull().firstOrNull()?.let { w ->
                val atKm = DemoRoute.restAreaKm[w.id]
                if (atKm != null && d.progressKm >= atKm && !d.hold && _ui.value.screen is Screen.Drive) {
                    d.hold = true
                    IntegrationLog.event("NaviHelper.onWayPointArrived(${w.name}) [시뮬레이션]")
                    viewModelScope.launch { delay(1500); onWaypointArrivedParked(w) }
                }
            }
            val remain = d.remainingKm
            _ui.update { it.copy(remainingKm = remain, etaText = eta(remain)) }
            if (remain <= 0.05 && _ui.value.screen is Screen.Drive) {
                (nav as? FallbackNavigationProvider)?.emit(NavEvent.DestinationArrived(route.destination.name))
                IntegrationLog.event("NaviHelper.onDestinationArrived(${route.destination.name}) [시뮬레이션]")
            }
        }
        updateNextRest()
    }

    private fun onCabin(c: CabinState) {
        val prev = _ui.value.cabin
        if (prev.turnSignalOn == true && c.turnSignalOn == false) turnSignalOffAt = System.currentTimeMillis()
        _ui.update { it.copy(cabin = c, cabinCaps = cabinSource?.capabilities ?: it.cabinCaps) }
        // 2차: 휴식 장소에 주차한 뒤 뒷문이 열리면 '휴식 시작할까요?'
        if (c.rearDoorOpen == true && prev.rearDoorOpen != true && _ui.value.pendingRestPrompt) {
            startRest(RestStartMethod.DOOR)
        }
    }

    private fun onNavEvent(e: NavEvent) {
        when (e) {
            is NavEvent.Driving -> _ui.update { it.copy(remainingKm = e.remainingKm, etaText = e.remainingMinutes?.let { m -> eta(minutes = m) }) }
            is NavEvent.DestinationArrived -> arrive(e.name ?: _ui.value.route?.destination?.name ?: "목적지")
            is NavEvent.RouteCancelled -> _ui.update { it.copy(route = null) }
            else -> Unit
        }
    }

    private fun eta(remainKm: Double? = null, minutes: Int? = null): String {
        val m = minutes ?: ((remainKm ?: 0.0) / 85.0 * 60).toInt()
        val base = provider?.now() ?: System.currentTimeMillis()
        return SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(base + m * 60_000L))
    }

    // ───────────────────────── 휴식 안내 · 알림 타이밍 ─────────────────────────
    private suspend fun tickLoop() {
        while (true) {
            delay(500)
            val u = _ui.value
            if (u.screen !is Screen.Drive || u.driveState != DriveState.DRIVING) continue
            val now = provider?.now() ?: System.currentTimeMillis()
            if (u.notifyRest) {
                when (val ev = restEngine.check(tracker.sinceRestMillis)) {
                    is RestEngine.Event.PreNotice -> gate.offer(Notice("pre", Priority.REST, "${u.pet.name}는 ${ev.minutesLeft}분 뒤 쉬어 가는 게 좋아요.", now))
                    RestEngine.Event.Due, RestEngine.Event.Reminder -> gate.offer(Notice("due", Priority.REST, "${u.pet.name}의 휴식 시간이에요. 가까운 휴식 장소를 찾아볼까요?", now))
                    null -> Unit
                }
            }
            if (u.notifySafety) safetyChecks(u, now)
            val ctxDriver = DriverContext(
                turnSignalOn = demo?.forceTurnSignal?.value ?: u.cabin.turnSignalOn,
                turnSignalOffAtMillis = turnSignalOffAt,
                nextTurnMeters = null, // NaviHelper TBT 이벤트는 내비 구현마다 달라 검증 후 연결(미전달 시 건너뜀)
                inCautionZone = null,
                naviSpeaking = null,
                vehicleWarning = null,
            )
            when (val d = gate.next(ctxDriver, now)) {
                is GateDecision.Speak -> present(d.notice, speak = true)
                is GateDecision.CardOnly -> present(d.notice, speak = false)
                is GateDecision.Wait -> if (u.waitingReason != d.reason) {
                    IntegrationLog.info("알림 대기: ${d.reason}")
                    _ui.update { it.copy(waitingReason = d.reason) }
                }
                GateDecision.Idle -> if (u.waitingReason != null) _ui.update { it.copy(waitingReason = null) }
            }
        }
    }

    private fun safetyChecks(u: UiState, now: Long) {
        val t = u.snapshot?.outsideTempC
        val acOff = if (demo != null) true else u.cabin.acOn == false
        if (!acSuggested && t != null && t >= 28f && acOff && (demo != null || u.cabinCaps.hvac)) {
            acSuggested = true
            gate.offer(Notice("ac", Priority.SAFETY, "바깥이 ${t.toInt()}도예요. ${u.pet.name}를 위해 에어컨을 켤까요?", now))
        }
        val w = u.cabin.rearWindowOpenPct
        if (!windowWarned && w != null && w >= 40) {
            windowWarned = true
            gate.offer(Notice("window", Priority.SAFETY, "뒷좌석 창문이 많이 열려 있어요.", now))
        }
    }

    private fun present(n: Notice, speak: Boolean) {
        val u = _ui.value
        val card = when (n.id) {
            "pre" -> DriveCard.PreNotice(n.speech)
            "due" -> DriveCard.RestDue("${u.pet.name}의 휴식 시간이에요.", "가까운 휴식 장소를 찾아볼까요?")
            "ac" -> DriveCard.AirCon(u.snapshot?.outsideTempC?.toInt() ?: 30)
            "window" -> DriveCard.Window(u.cabin.rearWindowOpenPct ?: 50)
            else -> DriveCard.Info(n.speech)
        }
        _ui.update { it.copy(card = card, waitingReason = null) }
        if (speak && u.voiceOn) speaker?.speak(n.speech)
        if (card is DriveCard.PreNotice || card is DriveCard.Window) {
            viewModelScope.launch { delay(8_000); _ui.update { s -> if (s.card == card) s.copy(card = null) else s } }
        }
    }

    private fun aheadRestAreas(): List<Pair<Place, Double>> {
        val d = demo
        return if (d != null) SampleData.restAreas.mapNotNull { p -> DemoRoute.restAreaKm[p.id]?.let { p to (it - d.progressKm) } }
        else SampleData.restAreas.mapIndexed { i, p -> p to (8.0 + i * 9) } // 서버 연동 전: 샘플 거리
    }

    private fun fuelLow(): Boolean {
        val s = _ui.value.snapshot
        return s?.fuelLow == true || (s?.fuelPercent ?: 100f) < 25f || (s?.evBatteryPercent ?: 100f) < 20f
    }

    private fun updateNextRest() {
        val c = RestCandidates.pick(aheadRestAreas(), fuelLow(), false, max = 1).firstOrNull()
        if (c?.place?.id != _ui.value.nextRest?.place?.id || c?.aheadKm?.toInt() != _ui.value.nextRest?.aheadKm?.toInt()) {
            _ui.update { it.copy(nextRest = c) }
        }
    }

    fun onRestYes() {
        val low = fuelLow()
        val items = RestCandidates.pick(aheadRestAreas(), low, false)
        IntegrationLog.info("휴식 후보 ${items.size}곳 (연료 부족=${low})")
        _ui.update { it.copy(card = DriveCard.Suggestions(items, low)) }
        if (_ui.value.voiceOn) speaker?.speak("경로 앞 휴식 장소 ${items.size}곳이에요. ${items.firstOrNull()?.place?.name ?: ""}이 가장 좋아요.")
    }

    fun onRestLater() {
        restEngine.snooze(tracker.sinceRestMillis)
        _ui.update { it.copy(card = null) }
    }

    fun dismissCard() = _ui.update { it.copy(card = null) }

    fun chooseRestPlace(p: Place) {
        val r = _ui.value.route ?: return
        val free = r.waypoints.indexOfFirst { it == null }
        if (free < 0) {
            val replace = r.waypoints.filterNotNull().first()
            _ui.update { it.copy(card = DriveCard.ReplaceWaypoint(p, replace)) }
            return
        }
        viewModelScope.launch {
            when (nav?.addWaypoint(p, free)) {
                NavResult.WaypointAdded -> {
                    val wps = r.waypoints.toMutableList().also { it[free] = p }
                    _ui.update { it.copy(route = r.copy(waypoints = wps), card = DriveCard.Info("${p.name}를 경유지로 넣었어요.")) }
                    if (_ui.value.voiceOn) speaker?.speak("${p.name}를 경유지로 넣었어요.")
                    delay(4000); _ui.update { s -> if (s.card is DriveCard.Info) s.copy(card = null) else s }
                }
                else -> _ui.update { it.copy(card = DriveCard.Info("경유지를 넣지 못했어요. 내장 내비에서 확인해 주세요.")) }
            }
        }
    }

    fun replaceWaypoint(newPlace: Place, old: Place) {
        val r = _ui.value.route ?: return
        val slot = r.waypoints.indexOf(old).coerceAtLeast(0)
        viewModelScope.launch {
            nav?.removeWaypoint(slot)
            nav?.addWaypoint(newPlace, slot)
            val wps = r.waypoints.toMutableList().also { it[slot] = newPlace }
            _ui.update { it.copy(route = r.copy(waypoints = wps), card = null) }
        }
    }

    fun acceptAirCon() {
        viewModelScope.launch {
            val ok = cabinSource?.setAc(true) ?: run { IntegrationLog.api("HVAC.setHvacAc(true) [데모]"); true }
            _ui.update { it.copy(card = DriveCard.Info(if (ok) "에어컨을 켰어요." else "에어컨을 켜지 못했어요. 직접 확인해 주세요.")) }
            delay(3000); _ui.update { s -> if (s.card is DriveCard.Info) s.copy(card = null) else s }
        }
    }

    // ───────────────────────── 휴식 · 도착 ─────────────────────────
    private fun onWaypointArrivedParked(p: Place) {
        demo?.onRest(true)
        _ui.update { it.copy(pendingRestPrompt = true, screen = Screen.Resting, card = null) }
        if (_ui.value.voiceOn) speaker?.speak("${p.name}에 도착했어요. 휴식을 시작할까요?")
    }

    fun startRest(method: RestStartMethod = RestStartMethod.BUTTON) {
        val now = provider?.now() ?: System.currentTimeMillis()
        demo?.onRest(true)
        IntegrationLog.info("휴식 시작 (${method.name})")
        _ui.update { it.copy(restStartedAt = now, pendingRestPrompt = false, screen = Screen.Resting) }
    }

    fun resumeDriving() {
        val r = _ui.value.route
        tracker.resetRestTimer(); restEngine.reset()
        demo?.let { it.onRest(false); it.hold = false }
        val visitedRest = r?.waypoints?.filterNotNull()?.firstOrNull()
        val wps = r?.waypoints?.toMutableList()?.also { l -> val i = l.indexOfFirst { it != null }; if (i >= 0) l[i] = null }
        _ui.update {
            it.copy(
                restStartedAt = null, restsDone = it.restsDone + 1, pendingRestPrompt = false,
                route = r?.copy(waypoints = wps ?: r.waypoints),
                visited = it.visited + listOfNotNull(visitedRest?.name),
                screen = if (r != null) Screen.Drive else Screen.Home, menu = Menu.HOME,
            )
        }
        IntegrationLog.info("다시 출발 → 휴식 주기를 처음부터 셉니다")
    }

    fun restElapsedMinutes(): Int {
        val st = _ui.value.restStartedAt ?: return 0
        return (((provider?.now() ?: System.currentTimeMillis()) - st) / 60_000).toInt()
    }

    private fun arrive(name: String) {
        val u = _ui.value
        if (u.screen is Screen.Arrived) return
        demo?.hold = true
        _ui.update { it.copy(screen = Screen.Arrived(name), card = null, visited = it.visited + name) }
        if (u.voiceOn) speaker?.speak("$name에 도착했어요.")
    }

    fun nextLeg() {
        val r = _ui.value.route ?: return goHome()
        val c = r.course ?: return goHome()
        val next = c.stops.filterNot { it.isRest }.getOrNull(r.courseLeg + 1)?.place ?: return goHome()
        _ui.update { it.copy(route = r.copy(destination = next, courseLeg = r.courseLeg + 1)) }
        startRoute(next, course = c, leg = r.courseLeg + 1)
    }

    fun finishRoute() {
        _ui.update { it.copy(route = null, screen = Screen.Home, menu = Menu.HOME) }
    }

    fun openWrapUp() { _ui.update { it.copy(screen = Screen.WrapUp, menu = Menu.TRIPS) } }

    fun saveTrip(save: Boolean) {
        IntegrationLog.info(if (save) "여행 기록 저장 → 휴대폰 알림(동기화 서버 연결 시)" else "기록 안 함")
        tracker.restore(0, 0, 0.0)
        _ui.update { it.copy(route = null, screen = Screen.Home, menu = Menu.HOME, restsDone = 0, visited = emptyList(), drivingMinutes = 0, distanceKm = 0.0) }
    }

    fun openQuickCheck(p: Place) = _ui.update { it.copy(screen = Screen.QuickCheck(p)) }
    fun answerQuickCheck(answer: String) {
        IntegrationLog.info("멍슐랭 빠른 확인: $answer (방문 인증 · 서버 전송 대기)")
        goHome()
    }

    // ───────────────────────── 길 안내 ─────────────────────────
    fun startRoute(place: Place, course: Course? = null, leg: Int = 0, trip: Trip? = null) {
        if (isLocked()) return
        val wps = trip?.waypoints?.take(2)?.let { l -> listOf(l.getOrNull(0), l.getOrNull(1)) } ?: listOf(null, null)
        _ui.update { it.copy(route = ActiveRoute(place, wps, course, leg, trip?.name), card = null) }
        viewModelScope.launch {
            val r = nav?.requestRoute(place, _ui.value.comfortableRoute)
            wps.forEachIndexed { i, w -> if (w != null) nav?.addWaypoint(w, i) }
            if (r is NavResult.Failed) {
                _ui.update { it.copy(card = DriveCard.Info(r.why)) }
                return@launch
            }
            demo?.let { d -> d.hold = false; d.onRest(false) }
            // 일반 기기(내비 미탑재)는 주행 감지가 없을 수 있어 바로 드라이브 모드로
            if (demo != null || nav?.isReal == false) {
                delay(800)
                _ui.update { it.copy(screen = Screen.Drive) }
            }
        }
    }

    // ───────────────────────── 음성 요청 (STT 결과 텍스트) ─────────────────────────
    fun onVoice(text: String) {
        IntegrationLog.event("음성 인식: \"$text\"")
        val u = _ui.value
        when {
            u.card is DriveCard.RestDue && Regex("응|네|그래|찾아").containsMatchIn(text) -> onRestYes()
            u.card is DriveCard.RestDue && Regex("나중|괜찮").containsMatchIn(text) -> onRestLater()
            u.card is DriveCard.AirCon && Regex("응|네|켜").containsMatchIn(text) -> acceptAirCon()
            u.card is DriveCard.Suggestions && Regex("([1-3])번").find(text) != null -> {
                val n = Regex("([1-3])번").find(text)!!.groupValues[1].toInt()
                (u.card as DriveCard.Suggestions).items.getOrNull(n - 1)?.let { chooseRestPlace(it.place) }
            }
            text.contains("휴게소") || text.contains("쉬") -> onRestYes()
            else -> {
                val cat = when {
                    text.contains("카페") -> Category.CAFE
                    text.contains("공원") || text.contains("산책") -> Category.PARK
                    text.contains("식당") || text.contains("밥") -> Category.RESTAURANT
                    else -> null
                }
                val items = SampleData.nearby.filter { cat == null || it.category == cat }.take(3)
                _ui.update { it.copy(card = DriveCard.VoiceResults(text, items)) }
                if (u.voiceOn) speaker?.speak("가까운 곳 ${items.size}곳을 찾았어요.")
            }
        }
    }

    fun pickVoiceResult(p: Place) {
        val r = _ui.value.route
        _ui.update { it.copy(card = null) }
        if (r != null) chooseRestPlace(p) else startRoute(p)
    }

    // ───────────────────────── 화면 이동 · 주행 제한 ─────────────────────────
    private suspend fun restrictionLoop() {
        AaosRestrictionSource(ctx).restrictions().collect { r -> _ui.update { it.copy(restriction = r) } }
    }

    /** 플랫폼 제한 상태 + 데모 주행 상태로 판단 */
    fun isDriving(): Boolean = _ui.value.restriction == UxRestriction.DRIVING || _ui.value.driveState == DriveState.DRIVING

    private fun isLocked(): Boolean = false

    private fun guarded(name: String, open: () -> Unit) {
        if (isDriving()) {
            IntegrationLog.info("주행 제한: $name 잠금")
            _ui.update { it.copy(lockedMessage = "주행 중에는 $name 화면을 볼 수 없어요") }
        } else open()
    }

    fun dismissLock() = _ui.update { it.copy(lockedMessage = null) }

    fun select(menu: Menu) {
        if (isDriving() && menu != Menu.HOME) return guarded(menu.label) {}
        val screen = when (menu) {
            Menu.HOME -> if (_ui.value.route != null && isDriving()) Screen.Drive else Screen.Home
            Menu.COURSE -> Screen.Courses
            Menu.SPOT -> Screen.Spots
            Menu.TRIPS -> Screen.Trips
            Menu.SETTINGS -> Screen.Settings
        }
        _ui.update { it.copy(menu = menu, screen = screen) }
    }

    fun openPlace(p: Place) = guarded("장소 상세") { _ui.update { it.copy(screen = Screen.PlaceDetail(p), menu = Menu.SPOT) } }
    fun openCourse(c: Course) = guarded("코스 상세") { _ui.update { it.copy(screen = Screen.CourseDetail(c), menu = Menu.COURSE) } }
    fun openTrip(t: Trip) = guarded("여행 확인") { _ui.update { it.copy(screen = Screen.TripConfirm(t), menu = Menu.TRIPS) } }
    fun openDrive() = _ui.update { it.copy(screen = Screen.Drive) }
    fun goHome() = _ui.update { it.copy(screen = Screen.Home, menu = Menu.HOME) }
    fun setCategory(c: Category) = _ui.update { it.copy(category = c) }

    // ───────────────────────── 설정 ─────────────────────────
    private fun applyRestInterval() {
        val u = _ui.value
        val pet = if (u.useRecommended) u.pet.copy(customRestMinutes = null) else u.pet
        val rec = if (!u.useRecommended && pet.customRestMinutes == null) RestGuideline.Recommendation(RestGuideline.DEFAULT_MINUTES, "기본 주기", "")
        else RestGuideline.forPassengers(listOf(pet))
        restEngine.intervalMinutes = rec.minutes
        _ui.update { it.copy(restMinutes = rec.minutes, restReason = rec.reason) }
    }

    fun setUseRecommended(on: Boolean) {
        prefs.edit().putBoolean("useRecommended", on).apply()
        _ui.update { it.copy(useRecommended = on) }
        applyRestInterval()
    }

    fun adjustRest(delta: Int) {
        val cur = _ui.value.restMinutes
        val m = (cur + delta).coerceIn(30, 240)
        prefs.edit().putInt("customRest", m).putBoolean("useRecommended", false).apply()
        _ui.update { it.copy(useRecommended = false, pet = it.pet.copy(customRestMinutes = m)) }
        applyRestInterval()
    }

    fun setNotify(rest: Boolean? = null, safety: Boolean? = null, voice: Boolean? = null, comfortable: Boolean? = null) {
        prefs.edit().apply {
            rest?.let { putBoolean("notifyRest", it) }; safety?.let { putBoolean("notifySafety", it) }
            voice?.let { putBoolean("voice", it) }; comfortable?.let { putBoolean("comfortable", it) }
        }.apply()
        _ui.update {
            it.copy(
                notifyRest = rest ?: it.notifyRest, notifySafety = safety ?: it.notifySafety,
                voiceOn = voice ?: it.voiceOn, comfortableRoute = comfortable ?: it.comfortableRoute,
            )
        }
    }

    val apiBaseConfigured: Boolean get() = BuildConfig.MUNGROAD_API_BASE_URL.isNotBlank()

    override fun onCleared() {
        provider?.release(); nav?.release(); cabinSource?.release(); speaker?.release()
    }
}
