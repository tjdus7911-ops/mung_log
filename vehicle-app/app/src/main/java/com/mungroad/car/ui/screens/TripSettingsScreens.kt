package com.mungroad.car.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mungroad.car.data.SampleData
import com.mungroad.car.data.Trip
import com.mungroad.car.ui.AppViewModel
import com.mungroad.car.ui.UiState
import com.mungroad.car.ui.components.Badge
import com.mungroad.car.ui.components.BadgeKind
import com.mungroad.car.ui.components.Card
import com.mungroad.car.ui.components.Chip
import com.mungroad.car.ui.components.PrimaryButton
import com.mungroad.car.ui.components.Scene
import com.mungroad.car.ui.components.SecondaryButton
import com.mungroad.car.ui.theme.C
import com.mungroad.car.ui.theme.T

// ───────────────────────────── 내 여행 (WF-C06) ─────────────────────────────
@Composable
fun TripsScreen(vm: AppViewModel) {
    var tab by remember { mutableStateOf(0) }
    Row(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f)) {
            Text("내 여행", style = T.H1)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("예정된 여행", "담아 온 코스", "지난 여행").forEachIndexed { i, s -> Chip(s, null, tab == i) { tab = i } }
            }
            Spacer(Modifier.height(18.dp))
            when (tab) {
                0 -> SampleData.trips.forEach { t -> TripRow(t) { vm.openTrip(t) }; Spacer(Modifier.height(12.dp)) }
                1 -> {
                    val c = SampleData.feedCourse
                    Card(Modifier.fillMaxWidth()) {
                        Badge("여행 피드에서 담음 · 3차", BadgeKind.USER)
                        Spacer(Modifier.height(8.dp))
                        Text(c.name, style = T.H2)
                        Text("${c.totalKm}km · 방문 ${c.stops.size}곳 · 공유자 출발지 대신 현재 위치에서 출발", style = T.Caption)
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton("코스 보기", icon = Icons.Outlined.Navigation) { vm.openCourse(c) }
                    }
                }
                else -> Card(Modifier.fillMaxWidth()) {
                    Text("지난 여행 (3차)", style = T.Title)
                    Text("날짜별 경로·주행 시간·휴식 횟수·방문지 요약을 보여 줘요. 사진과 AI 여행 일기는 휴대폰에서 볼 수 있어요.", style = T.Caption)
                    Spacer(Modifier.height(14.dp))
                    SecondaryButton("집 도착 · 여행 마무리 (시연)", icon = Icons.Outlined.Home) { vm.openWrapUp() }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("여행은 휴대폰 멍로드 앱에서 만들고 차에서 불러와요.", style = T.Caption)
        }
    }
}

@Composable
private fun TripRow(t: Trip, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Card).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Scene(t.destination.image, Modifier.width(170.dp).height(110.dp), 16.dp)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, style = T.H2)
            Text("${t.dateLabel} · 경유 ${t.waypoints.size}곳" + if (t.sample) " · 샘플 데이터" else "", style = T.Caption)
        }
        Icon(Icons.Outlined.Navigation, null, tint = C.Orange, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun TripConfirmScreen(t: Trip, u: UiState, vm: AppViewModel) {
    Row(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(Modifier.weight(1.2f).fillMaxHeight()) {
            Text(t.name, style = T.Display)
            Text("출발 전에 확인해 주세요", style = T.Body.copy(color = C.Sub))
            Spacer(Modifier.height(20.dp))
            val rows = listOf(
                "출발지" to "${t.origin} (내비 즐겨찾기)",
                "목적지" to t.destination.name,
                "경유지" to (t.waypoints.joinToString(" · ") { it.name }.ifBlank { "없음" } + " (${t.waypoints.size}/2)"),
                "함께할 반려견" to "${u.pet.name} · 휴식 주기 ${u.restMinutes}분",
                "경로 옵션" to if (u.comfortableRoute) "편한 길 (반려견 탑승 추천)" else "추천 경로",
            )
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                    Text(k, style = T.BodyL.copy(color = C.Sub), modifier = Modifier.width(220.dp))
                    Text(v, style = T.Title)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton("드라이브 시작", Modifier.fillMaxWidth(), Icons.Outlined.Navigation, height = 112.dp, fontSize = 28) {
                vm.startRoute(t.destination, trip = t)
            }
        }
        Card(Modifier.weight(0.8f).fillMaxHeight(), padding = 0.dp) {
            Scene(t.destination.image, Modifier.fillMaxWidth().height(260.dp), 0.dp, "여행 대표 사진")
            Column(Modifier.padding(28.dp)) {
                Text("편한 길로 안내해요", style = T.Title)
                Text("반려견이 탄 날은 내장 내비 경로 옵션 COMFORTABLE(편한 길)을 먼저 제안해요. 경유지는 최대 2곳까지 넣을 수 있어요.", style = T.Caption)
                Spacer(Modifier.height(18.dp))
                ToggleRow("편한 길로 안내", u.comfortableRoute) { vm.setNotify(comfortable = it) }
            }
        }
    }
}

// ───────────────────────────── 여행 마무리 (WF-C19 / D-C10 · 3차) ─────────────────────────────
@Composable
fun WrapUpScreen(u: UiState, vm: AppViewModel) {
    Card(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), padding = 44.dp) {
        Row(Modifier.clip(RoundedCornerShape(20.dp)).background(C.OrangeSoft).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Home, null, tint = C.Orange, modifier = Modifier.size(24.dp))
            Text("  집에 도착했어요", style = T.Caption.copy(color = C.Orange, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        Text("${u.pet.name}와의 여행을 마무리할까요?", style = T.Display)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Card(Modifier.weight(1f).fillMaxHeight(), padding = 0.dp, color = C.MapLand) { MapArt(Modifier.fillMaxSize(), 4, "여행 경로 요약") }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Stat("실제 주행", "${u.drivingMinutes / 60}시간 ${u.drivingMinutes % 60}분", Icons.Outlined.DirectionsCar, Modifier.weight(1f))
                    Stat("이동 거리", "${u.distanceKm.toInt()}km", Icons.Outlined.Place, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Stat("휴식", "${u.restsDone}회", Icons.Outlined.Chair, Modifier.weight(1f))
                    Stat("방문", "${u.visited.distinct().size}곳", Icons.Outlined.CalendarMonth, Modifier.weight(1f))
                }
                Text("기록된 값만 보여 줘요. 저장하면 휴대폰에서 사진을 고르고 AI 여행 일기를 만들 수 있어요(동기화 서버 연결 시).", style = T.Caption)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PrimaryButton("마무리하고 기록 저장", Modifier.weight(2f), Icons.Outlined.CheckCircle, height = 112.dp, fontSize = 26) { vm.saveTrip(true) }
            SecondaryButton("아직 여행 중", Modifier.weight(1f), height = 112.dp) { vm.goHome() }
            SecondaryButton("기록 안 함", Modifier.weight(1f), height = 112.dp) { vm.saveTrip(false) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(C.Cream).padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = C.Orange, modifier = Modifier.size(26.dp))
            Text("  $label", style = T.Body.copy(color = C.Sub))
        }
        Text(value, style = T.H1)
    }
}

// ───────────────────────────── 설정 (WF-C07) ─────────────────────────────
@Composable
fun SettingsScreen(u: UiState, vm: AppViewModel) {
    Row(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(Modifier.weight(1.2f).fillMaxHeight()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("휴식 주기", style = T.H1)
                Text("반려견 정보로 추천한 간격을 쓰거나 직접 바꿀 수 있어요. 참고 안내이며 차량이 반려견의 건강 상태를 측정하지 않아요.", style = T.Caption)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.OrangeSoft).padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${u.pet.name} · ${u.restReason}", style = T.Body.copy(color = C.Sub))
                        Text("${u.restMinutes}분마다", style = T.Number.copy(color = C.Orange))
                    }
                    SecondaryButton("", icon = Icons.Outlined.Remove, height = 80.dp) { vm.adjustRest(-10) }
                    Spacer(Modifier.width(12.dp))
                    SecondaryButton("", icon = Icons.Outlined.Add, height = 80.dp) { vm.adjustRest(10) }
                }
                Spacer(Modifier.height(10.dp))
                ToggleRow("추천값 사용 (기준표 · 출시 전 수의사 자문으로 확정)", u.useRecommended) { vm.setUseRecommended(it) }
                Text("1차: 기본 120분을 보호자가 바꿔 써요 · 2차: 휴대폰에서 등록한 나이·견종·체중·건강 상태로 맞춤 주기, 수의사 간격 최우선 · 여러 마리면 가장 짧은 간격", style = T.Caption)
                Spacer(Modifier.height(22.dp))
                Text("알림 · 음성", style = T.H2)
                ToggleRow("반려견 휴식 안내 (10분 전 예고 + 도달 안내)", u.notifyRest) { vm.setNotify(rest = it) }
                ToggleRow("안전 안내 (더위·뒷좌석 창문)", u.notifySafety) { vm.setNotify(safety = it) }
                ToggleRow("음성 안내", u.voiceOn) { vm.setNotify(voice = it) }
            }
        }
        Card(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("차량 연동", style = T.H2)
                Spacer(Modifier.height(8.dp))
                InfoLine("빌드", u.sdkStatus)
                InfoLine("차량 데이터", u.providerName)
                InfoLine("내비", u.navName)
                InfoLine("음성", u.speakerName)
                Spacer(Modifier.height(10.dp))
                Text("지원 기능 (Vehicle SDK checkXXXCapabilities)", style = T.Title)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CapBadge("방향지시등", u.cabinCaps.turnSignal); CapBadge("뒷문", u.cabinCaps.door); CapBadge("창문", u.cabinCaps.window); CapBadge("에어컨", u.cabinCaps.hvac)
                }
                Text("지원하지 않는 기능은 해당 안내만 숨겨요.", style = T.Caption)
                Spacer(Modifier.height(18.dp))
                Text("데모 모드", style = T.H2)
                ToggleRow("가상 주행(서울 → 강릉)으로 시연", u.demoMode) { vm.setDemoMode(it) }
                if (u.demoMode) {
                    Text("배속 ${u.demoSpeed}×", style = T.Body)
                    Slider(value = u.demoSpeed.toFloat(), onValueChange = { vm.setDemoSpeed(it.toInt()) }, valueRange = 10f..240f)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("더운 날 31℃", height = 64.dp, fontSize = 18) { vm.setDemoTemp(31f) }
                        SecondaryButton("방향지시등", height = 64.dp, fontSize = 18) { vm.toggleDemoTurnSignal() }
                    }
                }
                ToggleRow("연동 로그 표시 (시연 전용)", u.showLog) { vm.toggleLog() }
                Spacer(Modifier.height(12.dp))
                Text("모바일 연동 (2차) · 위치·데이터 동의 · 약관", style = T.Title)
                Text("같은 계정으로 반려견·여행·즐겨찾기를 휴대폰과 동기화해요. " + if (vm.apiBaseConfigured) "서버 연결됨" else "서버 주소 미설정 → 샘플 데이터", style = T.Caption)
            }
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.BodyL, modifier = Modifier.weight(1f))
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.Orange, checkedThumbColor = Color.White))
    }
}

@Composable
private fun InfoLine(k: String, v: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(k, style = T.Caption, modifier = Modifier.width(130.dp))
        Text(v, style = T.Body.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun CapBadge(name: String, ok: Boolean) = Badge("$name ${if (ok) "✓" else "—"}", if (ok) BadgeKind.VERIFIED else BadgeKind.INFO)
