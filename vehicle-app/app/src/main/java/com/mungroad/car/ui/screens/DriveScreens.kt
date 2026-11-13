package com.mungroad.car.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mungroad.car.data.Place
import com.mungroad.car.data.SampleData
import com.mungroad.car.domain.DriveState
import com.mungroad.car.ui.AppViewModel
import com.mungroad.car.ui.DriveCard
import com.mungroad.car.ui.UiState
import com.mungroad.car.ui.components.Badge
import com.mungroad.car.ui.components.BadgeKind
import com.mungroad.car.ui.components.Card
import com.mungroad.car.ui.components.KeyValue
import com.mungroad.car.ui.components.PlaceBadges
import com.mungroad.car.ui.components.PrimaryButton
import com.mungroad.car.ui.components.Scene
import com.mungroad.car.ui.components.SecondaryButton
import com.mungroad.car.ui.theme.C
import com.mungroad.car.ui.theme.T

// ───────────────────────────── 드라이브 모드 (WF-C08~C12 / D-C03·C04) ─────────────────────────────
@Composable
fun DriveScreen(u: UiState, vm: AppViewModel, onMic: () -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            MapArt(Modifier.fillMaxSize(), pins = 1, label = "지도 SDK 영역 · 길 안내는 분할 화면의 내장 내비")
            Row(
                Modifier.padding(28.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(C.Card).padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Pets, null, tint = C.Orange, modifier = Modifier.size(28.dp))
                Text("  드라이브 모드", style = T.Title)
                Spacer(Modifier.width(14.dp))
                val (dot, label) = when (u.driveState) {
                    DriveState.DRIVING -> C.Verified to "주행"
                    DriveState.STOPPED -> C.Check to "정차"
                    DriveState.PARKED -> C.Sub to "주차"
                    DriveState.UNKNOWN -> C.Tertiary to "확인 중"
                }
                Box(Modifier.size(14.dp).clip(CircleShape).background(dot))
                Text(" $label", style = T.Caption)
            }
            u.waitingReason?.let {
                Text("알림 대기 · $it", style = T.Caption.copy(color = C.Check, fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.BottomStart).padding(28.dp).clip(RoundedCornerShape(16.dp)).background(C.CheckSoft).padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        Column(Modifier.width(620.dp).fillMaxHeight().background(C.Card).padding(28.dp)) {
            val r = u.route
            Text("목적지", style = T.Body.copy(color = C.Sub))
            Text(r?.destination?.name ?: "-", style = T.H1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Row {
                KeyValue("남은 거리", u.remainingKm?.let { "${it.toInt()}km" } ?: "-", Modifier.weight(1f), big = true)
                KeyValue("도착 예정", u.etaText ?: "-", Modifier.weight(1f), big = true)
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val c = u.card) {
                    null -> NextRestPanel(u)
                    is DriveCard.PreNotice -> NoticeCard(c.text, "예고 · 휴식 주기 ${u.restMinutes}분") {}
                    is DriveCard.RestDue -> TwoChoiceCard(c.title, c.sub, "주행 ${u.sinceRestMinutes / 60}시간 ${u.sinceRestMinutes % 60}분 · ${u.restReason}", "찾아볼게요", "나중에", vm::onRestYes, vm::onRestLater)
                    is DriveCard.Suggestions -> SuggestionsCard(c, vm)
                    is DriveCard.ReplaceWaypoint -> TwoChoiceCard("경유지 2곳이 모두 있어요", "‘${c.replace.name}’을 ‘${c.newPlace.name}’로 바꿀까요?", "NaviHelper 경유지는 최대 2곳", "바꾸기", "취소", { vm.replaceWaypoint(c.newPlace, c.replace) }, vm::dismissCard)
                    is DriveCard.AirCon -> TwoChoiceCard("바깥이 ${c.tempC}도예요.", "${u.pet.name}를 위해 에어컨을 켤까요?", "동의할 때만 켜요 · 멍로드의 유일한 차량 제어", "켜 주세요", "괜찮아요", vm::acceptAirCon, vm::dismissCard, Icons.Outlined.AcUnit)
                    is DriveCard.Window -> NoticeCard("뒷좌석 창문이 많이 열려 있어요.", "열림 ${c.pct}% · 직접 닫지 않아요") { vm.dismissCard() }
                    is DriveCard.VoiceResults -> VoiceResultsCard(c, vm)
                    is DriveCard.Info -> NoticeCard(c.text, "") { vm.dismissCard() }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().height(76.dp).clip(CircleShape).background(C.Sunken).clickable(onClick = onMic).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(C.Orange), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Mic, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Text("  “근처 강아지 동반 카페 찾아줘” 처럼 말해 보세요", style = T.Caption.copy(fontWeight = FontWeight.Medium))
            }
        }
    }
}

@Composable
private fun NextRestPanel(u: UiState) {
    Column {
        val nr = u.nextRest
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(C.Cream).padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Chair, null, tint = C.Orange, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("다음 휴식까지", style = T.Body.copy(color = C.Sub))
                val left = (u.restMinutes - u.sinceRestMinutes).coerceAtLeast(0)
                Text("${left}분", style = T.Number.copy(color = C.Orange))
                Text("${u.pet.name} · ${u.restMinutes}분 주기 (실제 주행 시간 기준)", style = T.Caption)
            }
        }
        Spacer(Modifier.height(14.dp))
        if (nr != null) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Cream).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("다음 휴식 후보 · ${nr.aheadKm.toInt()}km", style = T.Caption)
                Text(nr.place.name, style = T.Title)
            }
            if (nr.place.hasDogPlayground) Badge("반려견 놀이터", BadgeKind.VERIFIED)
            if (nr.place.hasFuel) { Spacer(Modifier.width(8.dp)); Badge("주유", BadgeKind.INFO) }
        }
        u.snapshot?.let { s ->
            Spacer(Modifier.height(10.dp))
            Text(
                listOfNotNull(
                    s.speedKmh?.let { "${it.toInt()}km/h" },
                    s.fuelPercent?.let { "연료 ${it.toInt()}%" },
                    s.outsideTempC?.let { "바깥 ${it.toInt()}℃" },
                    "주행 ${u.drivingMinutes}분",
                ).joinToString(" · "),
                style = T.Caption,
            )
        }
    }
}

@Composable
private fun NoticeCard(text: String, sub: String, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(C.OrangeSoft).border(2.5.dp, C.Orange, RoundedCornerShape(32.dp)).clickable(onClick = onClose).padding(32.dp)) {
        VoiceTag()
        Spacer(Modifier.height(14.dp))
        Text(text, style = T.H1)
        if (sub.isNotBlank()) Text(sub, style = T.Caption)
    }
}

@Composable
private fun VoiceTag() {
    Row(Modifier.clip(CircleShape).background(C.Card).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Mic, null, tint = C.Orange, modifier = Modifier.size(22.dp))
        Text(" 음성 안내", style = T.Caption.copy(color = C.Orange, fontWeight = FontWeight.Bold))
    }
}

/** 주행 중 카드: 음성 한 문장 + 큰 버튼 2개 */
@Composable
private fun TwoChoiceCard(
    title: String, sub: String, meta: String, yes: String, no: String, onYes: () -> Unit, onNo: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(C.OrangeSoft).border(2.5.dp, C.Orange, RoundedCornerShape(32.dp)).padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoiceTag()
            if (icon != null) { Spacer(Modifier.width(12.dp)); Icon(icon, null, tint = C.Orange, modifier = Modifier.size(30.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = T.H1)
        Text(sub, style = T.BodyL)
        Spacer(Modifier.height(8.dp))
        Text(meta, style = T.Caption)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PrimaryButton(yes, Modifier.weight(1f), height = 120.dp, fontSize = 26, onClick = onYes)
            SecondaryButton(no, Modifier.weight(1f), height = 120.dp, fontSize = 26, onClick = onNo)
        }
    }
}

@Composable
private fun SuggestionsCard(c: DriveCard.Suggestions, vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("경로 앞 휴식 장소", style = T.H2)
        if (c.fuelLow) Row(Modifier.padding(vertical = 8.dp).clip(CircleShape).background(C.CheckSoft).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocalGasStation, null, tint = C.Check, modifier = Modifier.size(22.dp))
            Text(" 연료가 부족해 주유 가능한 곳을 먼저 보여 드려요", style = T.Caption.copy(color = C.Check, fontWeight = FontWeight.Bold))
        }
        if (c.items.isEmpty()) Text("경로 앞에 확인된 휴식 장소가 없어요.", style = T.Body)
        c.items.forEachIndexed { i, rc ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(24.dp))
                    .background(if (i == 0) C.OrangeSoft else C.Cream).clickable { vm.chooseRestPlace(rc.place) }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(if (i == 0) C.Orange else C.Card), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (i == 0) Color.White else C.Ink)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(rc.place.name, style = T.Title)
                    Text("${rc.aheadKm.toInt()}km · 약 ${rc.minutes}분" + if (rc.reason.isNotBlank()) " · ${rc.reason}" else "", style = T.Caption, maxLines = 1)
                }
                Icon(Icons.Outlined.AltRoute, null, tint = C.Orange, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text("“1번”처럼 번호로 말해도 돼요 · 고른 곳은 내장 내비 경유지로 추가", style = T.Caption)
    }
}

@Composable
private fun VoiceResultsCard(c: DriveCard.VoiceResults, vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("“${c.query}”", style = T.Body.copy(color = C.Sub))
        Text("가까운 ${c.items.size}곳이에요", style = T.H2)
        Spacer(Modifier.height(8.dp))
        c.items.forEachIndexed { i, p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(24.dp)).background(C.Cream).clickable { vm.pickVoiceResult(p) }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = C.Orange, modifier = Modifier.width(36.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = T.Title)
                    PlaceBadges(p, max = 1)
                }
                Icon(Icons.Outlined.Navigation, null, tint = C.Orange, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton("닫기", Modifier.fillMaxWidth(), height = 72.dp) { vm.dismissCard() }
    }
}

// ───────────────────────────── 휴식 중 (WF-C13 / D-C07) ─────────────────────────────
@Composable
fun RestingScreen(u: UiState, vm: AppViewModel) {
    val place = u.route?.waypoints?.filterNotNull()?.firstOrNull()
    Row(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(Modifier.weight(1.4f).fillMaxHeight(), padding = 40.dp) {
            Text("${place?.name ?: "휴식 장소"}에 도착했어요", style = T.BodyL.copy(color = C.Sub))
            Text("${u.pet.name}와 쉬어 가요", style = T.Display)
            Spacer(Modifier.height(20.dp))
            if (u.restStartedAt == null) {
                Text("휴식을 시작할까요?", style = T.H2)
                Text("주차 상태(GEAR_SELECTION=P)에서만 물어봐요 · 뒷문을 열면 자동으로 묻기(2차, Vehicle SDK Door)", style = T.Caption)
                Spacer(Modifier.height(16.dp))
                PrimaryButton("휴식 시작", Modifier.fillMaxWidth(), Icons.Outlined.CheckCircle, height = 112.dp, fontSize = 28) { vm.startRest() }
            } else {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(C.OrangeSoft).padding(32.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("휴식 시간", style = T.Body.copy(color = C.Sub))
                        Text("${vm.restElapsedMinutes()}분", style = T.Number.copy(color = C.Orange))
                        Text("지금까지 주행 ${u.drivingMinutes / 60}시간 ${u.drivingMinutes % 60}분", style = T.Body)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("다음 휴식까지", style = T.Body.copy(color = C.Sub))
                        Text("${u.restMinutes}분", style = T.H1)
                        Text("다시 출발하면 처음부터 세요", style = T.Caption)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Cream).padding(26.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = C.Orange, modifier = Modifier.size(28.dp))
                        Text("  내리기 전에 확인해 주세요", style = T.Title)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("✓ ${u.pet.name}를 차에 두고 내리지 않기", style = T.BodyL)
                    Text("✓ 잠시 두고 내린다면 차량의 ‘펫 모드’를 켜 주세요", style = T.BodyL)
                    Text("펫 모드는 차량 기능이에요 · 멍로드가 직접 켜지 않아요", style = T.Caption)
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton("다시 출발", Modifier.fillMaxWidth(), Icons.Outlined.Navigation, height = 112.dp, fontSize = 28) { vm.resumeDriving() }
            }
        }
        Card(Modifier.weight(1f).fillMaxHeight(), padding = 0.dp) {
            Scene("dogpark", Modifier.fillMaxWidth().height(300.dp), 0.dp, "휴게소 반려견 시설")
            Column(Modifier.padding(30.dp)) {
                Text(if (place?.hasDogPlayground == true) "반려견 놀이터" else "주변 산책", style = T.H2)
                place?.source?.let { Badge("확인됨 · $it", BadgeKind.VERIFIED) }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Cream).padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Pets, null, tint = C.Orange, modifier = Modifier.size(26.dp))
                        Text("  ${u.pet.name} 주의사항", style = T.Title)
                    }
                    Text(u.pet.caution.ifBlank { "등록된 주의사항이 없어요" }, style = T.Body)
                }
            }
        }
    }
}

// ───────────────────────────── 도착 · 다음 구간 (WF-C16) ─────────────────────────────
@Composable
fun ArrivedScreen(name: String, u: UiState, vm: AppViewModel) {
    val course = u.route?.course
    val next = course?.stops?.filterNot { it.isRest }?.getOrNull((u.route?.courseLeg ?: 0) + 1)?.place
    Row(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(Modifier.weight(1.3f).fillMaxHeight(), padding = 44.dp) {
            Text("${name}에 도착했어요", style = T.Display)
            if (course != null) Text("${course.name} · ${(u.route?.courseLeg ?: 0) + 1} / ${course.stops.count { !it.isRest }} 구간 완료", style = T.Body.copy(color = C.Sub))
            Spacer(Modifier.height(18.dp))
            Text("휴대폰으로 사진을 남겨 보세요 (도착 알림 · 3차)", style = T.BodyL)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SecondaryButton("멍슐랭 빠른 확인", Modifier.weight(1f), height = 96.dp) {
                    vm.openQuickCheck((SampleData.nearby + SampleData.restAreas + listOfNotNull(u.route?.destination)).firstOrNull { it.name == name } ?: SampleData.cafe)
                }
                SecondaryButton("집 도착 · 여행 마무리", Modifier.weight(1f), height = 96.dp) { vm.openWrapUp() }
            }
            Spacer(Modifier.height(16.dp))
            SecondaryButton("홈으로", Modifier.fillMaxWidth(), height = 80.dp) { vm.finishRoute() }
        }
        if (next != null) Card(Modifier.weight(1f).fillMaxHeight()) {
            Text("다음 방문지", style = T.Body.copy(color = C.Sub))
            Text(next.name, style = T.Display)
            PlaceBadges(next, showUser = true)
            Spacer(Modifier.weight(1f))
            PrimaryButton("다음 구간 안내", Modifier.fillMaxWidth(), Icons.Outlined.Navigation, height = 112.dp, fontSize = 28) { vm.nextLeg() }
        }
    }
}

// ───────────────────────────── 멍슐랭 빠른 확인 (WF-C18 · 3차 제안) ─────────────────────────────
@Composable
fun QuickCheckScreen(p: Place, u: UiState, vm: AppViewModel) {
    Box(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.width(1000.dp), padding = 44.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Scene(p.image, Modifier.width(220.dp).height(150.dp), 22.dp)
                Spacer(Modifier.width(28.dp))
                Column {
                    Text("방금 다녀온 곳", style = T.Body.copy(color = C.Sub))
                    Text(p.name, style = T.Display)
                    Badge("방문 인증 · 도착 기록", BadgeKind.VERIFIED)
                }
            }
            Spacer(Modifier.height(26.dp))
            Text("${u.pet.name}와 가 보니 동반 조건이 맞았나요?", style = T.H1)
            Spacer(Modifier.height(8.dp))
            PlaceBadges(p, max = 3)
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton("맞았어요", Modifier.weight(1f), Icons.Outlined.CheckCircle, height = 112.dp, fontSize = 26) { vm.answerQuickCheck("맞았어요") }
                SecondaryButton("달랐어요", Modifier.weight(1f), height = 112.dp, fontSize = 26) { vm.answerQuickCheck("달랐어요") }
                SecondaryButton("건너뛰기", Modifier.weight(1f), height = 112.dp, fontSize = 26) { vm.answerQuickCheck("건너뛰기") }
            }
            Spacer(Modifier.height(16.dp))
            Text("주차 상태에서만 물어요 · ‘달랐어요’는 휴대폰으로 이어서 남길 수 있어요 · 출발하면 자동으로 닫혀요", style = T.Caption)
        }
    }
}

// ───────────────────────────── 주행 제한 잠금 (WF-C15) ─────────────────────────────
@Composable
fun LockOverlay(message: String, onVoice: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x99242424)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        Card(Modifier.width(820.dp), padding = 44.dp) {
            Icon(Icons.Outlined.Lock, null, tint = C.Orange, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Text(message, style = T.H2, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("안전을 위해 정차 후 이용해 주세요. 지금은 음성으로 찾을 수 있어요.", style = T.Body.copy(color = C.Sub), modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton("음성으로 찾기", Modifier.weight(1f), Icons.Outlined.Mic, height = 96.dp) { onClose(); onVoice() }
                SecondaryButton("닫기", Modifier.weight(1f), height = 96.dp, onClick = onClose)
            }
        }
    }
}
