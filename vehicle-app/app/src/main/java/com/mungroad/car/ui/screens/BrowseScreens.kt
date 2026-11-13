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
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mungroad.car.data.Category
import com.mungroad.car.data.Course
import com.mungroad.car.data.Place
import com.mungroad.car.data.SampleData
import com.mungroad.car.data.Verification
import com.mungroad.car.ui.AppViewModel
import com.mungroad.car.ui.UiState
import com.mungroad.car.ui.components.Badge
import com.mungroad.car.ui.components.BadgeKind
import com.mungroad.car.ui.components.Card
import com.mungroad.car.ui.components.Chip
import com.mungroad.car.ui.components.PlaceBadges
import com.mungroad.car.ui.components.PrimaryButton
import com.mungroad.car.ui.components.RoundIconButton
import com.mungroad.car.ui.components.Scene
import com.mungroad.car.ui.components.SecondaryButton
import com.mungroad.car.ui.components.SectionTitle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mungroad.car.R
import com.mungroad.car.ui.theme.BlueCta
import com.mungroad.car.ui.theme.Glass
import com.mungroad.car.ui.theme.GlassShadow
import com.mungroad.car.ui.theme.glass
import com.mungroad.car.ui.theme.C
import com.mungroad.car.ui.theme.T
import kotlin.math.roundToInt

fun categoryIcon(c: Category): ImageVector = when (c) {
    Category.ALL -> Icons.Outlined.Pets
    Category.CAFE -> Icons.Outlined.LocalCafe
    Category.RESTAURANT -> Icons.Outlined.Restaurant
    Category.PARK -> Icons.Outlined.Park
    Category.STAY -> Icons.Outlined.Hotel
    Category.FACILITY -> Icons.Outlined.Home
    Category.REST_AREA -> Icons.Outlined.DirectionsCar
}

/** 시연용 거리: 서버 연동 전 샘플 기준점(강릉 시내)과의 거리 */
fun distanceText(p: Place): String {
    val km = com.mungroad.car.domain.DriveTracker.haversineKm(37.7700, 128.8980, p.lat, p.lng)
    val min = (km / 30 * 60).roundToInt().coerceAtLeast(3)
    return "${"%.1f".format(km)}km · ${min}분"
}

// ───────────────────────────── 홈 (WF-C01 / 최종 시안 2026-11-13 · 블루 글래스) ─────────────────────────────
@Composable
fun HomeScreen(u: UiState, vm: AppViewModel) {
    Row(Modifier.fillMaxSize().padding(start = 30.dp, end = 36.dp, bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(21.dp)) {
        // 왼쪽: 히어로(425) → 주변 장소
        Column(Modifier.weight(887f).fillMaxHeight()) {
            HomeHero(u, onSearch = { vm.select(com.mungroad.car.ui.Menu.COURSE) })
            Spacer(Modifier.height(25.dp))
            HomeSectionTitle("지금, 반려견과 가기 좋은 곳", "멍스팟 전체") { vm.select(com.mungroad.car.ui.Menu.SPOT) }
            Spacer(Modifier.height(17.dp))
            CategoryRow(u.category) { vm.setCategory(it) }
            Spacer(Modifier.height(12.dp))
            val list = SampleData.nearby.filter { u.category == Category.ALL || it.category == u.category }.take(2)
            if (list.isEmpty()) Text("이 카테고리는 주변에 없어요. 다른 카테고리를 골라 보세요.", style = T.Body.copy(color = C.Sub), modifier = Modifier.padding(16.dp))
            list.forEachIndexed { i, p ->
                if (i > 0) Spacer(Modifier.height(9.dp))
                HomePlaceRow(p, onOpen = { vm.openPlace(p) }, onGo = { vm.startRoute(p) })
            }
        }
        // 오른쪽: 다음 여행(강조 유리) → 반려견 → 추천 코스
        Column(Modifier.weight(477f).fillMaxHeight()) {
            HomeNextTrip(vm)
            Spacer(Modifier.height(19.dp))
            HomePetCard(u) { vm.select(com.mungroad.car.ui.Menu.SETTINGS) }
            Spacer(Modifier.height(25.dp))
            HomeSectionTitle("추천 드라이브 코스", "전체") { vm.select(com.mungroad.car.ui.Menu.COURSE) }
            Spacer(Modifier.height(17.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                SampleData.courses.forEach { c -> CourseTile(c, Modifier.weight(1f).fillMaxHeight()) { vm.openCourse(c) } }
            }
        }
    }
}

@Composable
private fun HomeHero(u: UiState, onSearch: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(425.dp)
            .shadow(22.dp, shape, ambientColor = GlassShadow, spotColor = GlassShadow)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF4F86D9), Color(0xFF7FA9E6), Color(0xFFB9D0EE)))),
    ) {
        // 사슴이 실제 사진 — 오른쪽 영역에 cover로 배치, 왼쪽은 블루 톤으로 자연스럽게 이어짐
        Image(
            painterResource(R.drawable.saseumi_hero), contentDescription = "${u.pet.name} 사진",
            contentScale = ContentScale.Crop, alignment = BiasAlignment(0.1f, -0.2f),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(maxWidth * 0.66f),
        )
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            0f to Color(0xF23F78D6), 0.30f to Color(0xD94B84DB), 0.46f to Color(0x664B84DB), 0.60f to Color(0x004B84DB),
        )))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x33FFFFFF), 0.25f to Color(0x00FFFFFF), 0.75f to Color(0x00102850), 1f to Color(0x59102850))))
        Box(Modifier.fillMaxSize().border(1.5.dp, Brush.linearGradient(listOf(Color(0xCCFFFFFF), Color(0x40FFFFFF), Color(0x99FFFFFF))), shape))
        Column(Modifier.padding(start = 57.dp, top = 55.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.saseumi_face), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(CircleShape).border(2.dp, Color(0xCCFFFFFF), CircleShape))
                Spacer(Modifier.width(10.dp))
                Text("${u.pet.name}와 함께", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Text("오늘은 어디로\n떠나볼까요?", style = T.Display.copy(color = Color.White, fontSize = 46.sp, lineHeight = 60.sp))
            Spacer(Modifier.height(30.dp))
            Row(
                Modifier.height(74.dp).glass(Glass.Strong, 37.dp).clickable(onClick = onSearch).padding(start = 26.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = C.Ink, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Text("반려견 동반 코스 찾기", style = T.Title.copy(fontSize = 23.sp))
                Spacer(Modifier.width(26.dp))
                Box(Modifier.size(46.dp).shadow(8.dp, CircleShape, spotColor = C.Orange.copy(alpha = 0.5f)).clip(CircleShape).background(BlueCta), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ArrowForward, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
        Row(Modifier.align(Alignment.BottomStart).padding(start = 57.dp, bottom = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Image, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("배경 · ${u.pet.name} 사진", fontSize = 18.sp, color = Color.White.copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun HomeSectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.H2.copy(fontSize = 25.sp), modifier = Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(action, style = T.Caption.copy(fontSize = 19.sp, color = C.Ink, fontWeight = FontWeight.Medium))
            Icon(Icons.Outlined.ChevronRight, null, tint = C.Ink, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun HomeNextTrip(vm: AppViewModel) {
    val t = SampleData.trips.firstOrNull()
    Column(Modifier.fillMaxWidth().height(266.dp).glass(Glass.Strong, 24.dp).padding(horizontal = 26.dp, vertical = 24.dp)) {
        if (t == null) {
            Text("다음 여행", style = T.Caption.copy(fontSize = 19.sp))
            Spacer(Modifier.height(12.dp))
            Text("예정된 여행이 없어요. 휴대폰 멍로드 앱에서 새 여행을 만들어 보세요.", style = T.Body.copy(color = C.Sub))
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("다음 여행 · ${t.dateLabel}", style = T.Caption.copy(fontSize = 19.sp), modifier = Modifier.weight(1f))
            dDay(t.startDate)?.let { Text(it, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.DDay) }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(78.dp).glass(Glass.Tint, 20.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AltRoute, null, tint = C.Orange, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = T.H2.copy(fontSize = 28.sp), maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text("${t.origin} → ${t.destination.name} · 경유 ${t.waypoints.size}곳" + if (t.sample) " · 샘플" else "", style = T.Caption.copy(fontSize = 19.sp), maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("경로 안내", Modifier.fillMaxWidth(), icon = Icons.Outlined.Navigation, height = 80.dp, fontSize = 26) { vm.openTrip(t) }
    }
}

private fun dDay(date: String?): String? = runCatching {
    val d = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), java.time.LocalDate.parse(date))
    when { d > 0 -> "D-$d"; d == 0L -> "D-DAY"; else -> null }
}.getOrNull()

@Composable
private fun HomePetCard(u: UiState, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(140.dp).glass(Glass.Tint, 24.dp).clickable(onClick = onClick).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.saseumi_face), "${u.pet.name} 사진", contentScale = ContentScale.Crop,
            modifier = Modifier.size(98.dp).shadow(10.dp, CircleShape, ambientColor = GlassShadow, spotColor = GlassShadow).clip(CircleShape).border(3.dp, Color(0xE6FFFFFF), CircleShape))
        Spacer(Modifier.width(22.dp))
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(u.pet.name, style = T.H2.copy(fontSize = 28.sp))
                Spacer(Modifier.width(10.dp))
                Text("${u.pet.breed} · ${u.pet.ageYears.toInt()}살 · ${u.pet.weightKg.toInt()}kg", style = T.Caption.copy(color = C.Sub), modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = C.Ink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("${u.restMinutes}분마다 쉬어 가요", style = T.Body.copy(fontWeight = FontWeight.SemiBold))
            }
            Text(u.restReason, style = T.Caption.copy(color = C.Tertiary))
        }
    }
}

@Composable
private fun HomePlaceRow(p: Place, onOpen: () -> Unit, onGo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(91.dp).glass(Glass.Regular, 20.dp).clickable(onClick = onOpen).padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 확인된 장소 사진이 없어 다른 곳 사진 대신 카테고리 아이콘
        Box(Modifier.width(106.dp).height(66.dp).glass(Glass.Tint, 16.dp), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(p.category), null, tint = C.Ink.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = T.Title.copy(fontSize = 23.sp), maxLines = 1)
            Spacer(Modifier.height(6.dp))
            PlaceBadges(p)
        }
        val d = distanceText(p).split(" · ")
        Column(horizontalAlignment = Alignment.End) {
            Text(d[0], style = T.Body.copy(fontWeight = FontWeight.SemiBold))
            Text(d.getOrElse(1) { "" }, style = T.Caption.copy(color = C.Sub))
        }
        Spacer(Modifier.width(18.dp))
        RoundIconButton(Icons.Outlined.Navigation, size = 64.dp, onClick = onGo)
    }
}

@Composable
fun CategoryRow(selected: Category, onSelect: (Category) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Category.ALL, Category.CAFE, Category.RESTAURANT, Category.PARK, Category.STAY, Category.FACILITY).forEach { c ->
            Chip(c.label, categoryIcon(c), c == selected) { onSelect(c) }
        }
    }
}

@Composable
fun PlaceRow(p: Place, onOpen: () -> Unit, onGo: () -> Unit, showUser: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Cream).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Scene(p.image, Modifier.width(140.dp).height(84.dp), 16.dp)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = T.Title, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            PlaceBadges(p, showUser = showUser)
        }
        Text(distanceText(p), style = T.Caption.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.width(16.dp))
        RoundIconButton(Icons.Outlined.Navigation, onClick = onGo)
    }
}

@Composable
fun CourseTile(c: Course, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.shadow(14.dp, shape, ambientColor = GlassShadow, spotColor = GlassShadow).clip(shape).clickable(onClick = onClick)) {
        val img = courseImage(c.image)
        if (img != null) Image(painterResource(img), "${c.name} 분위기 예시", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Scene(c.image, Modifier.fillMaxSize(), 20.dp)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color(0x00102850), 1f to Color(0xB3102850))))
        Box(Modifier.fillMaxSize().border(1.5.dp, Brush.linearGradient(listOf(Color(0xCCFFFFFF), Color(0x33FFFFFF), Color(0x99FFFFFF))), shape))
        if (img != null) {
            Box(Modifier.padding(14.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x40FFFFFF)).border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 3.dp)) {
                Text("예시", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 12.dp, bottom = 20.dp)) {
            if (c.fromFeed) { Badge("여행 피드에서 담음", BadgeKind.USER); Spacer(Modifier.height(6.dp)) }
            Text(c.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, letterSpacing = (-0.4).sp)
            Spacer(Modifier.height(4.dp))
            Text("${c.totalKm}km · ${c.minutes / 60}시간 ${c.minutes % 60}분", fontSize = 19.sp, color = Color.White, maxLines = 1)
        }
    }
}

/** 코스 사진: 지역 분위기 예시(실제 코스 장소 사진 아님 → '예시' 표시) */
private fun courseImage(key: String): Int? = when (key) {
    "sea" -> R.drawable.course_sea
    "park" -> R.drawable.course_forest
    else -> null
}

// ───────────────────────────── 멍스팟 (WF-C02) ─────────────────────────────
@Composable
fun SpotListScreen(u: UiState, vm: AppViewModel) {
    Row(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1.1f)) {
            Text("멍스팟", style = T.H1)
            Text("현재 위치 주변 · 거리순 · 샘플 데이터", style = T.Caption)
            Spacer(Modifier.height(14.dp))
            CategoryRow(u.category) { vm.setCategory(it) }
            Spacer(Modifier.height(16.dp))
            val list = SampleData.nearby.filter { u.category == Category.ALL || it.category == u.category }.take(4)
            list.forEach { p -> PlaceRow(p, { vm.openPlace(p) }, { vm.startRoute(p) }, showUser = true); Spacer(Modifier.height(10.dp)) }
        }
        Card(Modifier.weight(0.9f).fillMaxHeight(), padding = 0.dp, color = C.MapLand) {
            MapArt(Modifier.fillMaxSize(), pins = SampleData.nearby.size, label = "지도 SDK 영역 · 표시 전용(길 안내는 내장 내비)")
        }
    }
}

// ───────────────────────────── 장소 상세 (WF-C03 / D-C02 · D-C08) ─────────────────────────────
@Composable
fun PlaceDetailScreen(p: Place, u: UiState, vm: AppViewModel) {
    Row(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(Modifier.weight(1.45f).fillMaxHeight(), padding = 0.dp) {
            Scene(p.image, Modifier.fillMaxWidth().height(280.dp), 0.dp)
            Column(Modifier.padding(32.dp)) {
                Text(p.name, style = T.Display)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, null, tint = C.Sub, modifier = Modifier.size(22.dp))
                    Text(" ${p.address} · 현재 위치에서 ${distanceText(p)}", style = T.Body.copy(color = C.Sub))
                }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Cream).padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("반려견 동반 조건", style = T.Title, modifier = Modifier.weight(1f))
                        if (p.verification == Verification.VERIFIED) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = C.Verified, modifier = Modifier.size(24.dp))
                            Text(" 확인됨", style = T.Caption.copy(color = C.Verified, fontWeight = FontWeight.Bold))
                        } else Badge("방문 전 확인 필요", BadgeKind.CHECK)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (p.verification == Verification.VERIFIED) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            p.conditions.forEach { Badge(it, BadgeKind.VERIFIED) }
                            p.restrictions.forEach { Badge(it, BadgeKind.INFO) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("출처 ${p.source} · 확인일 ${p.verifiedAt}", style = T.Caption)
                    } else {
                        Text("반려견 동반 정보가 확인되지 않았어요. 방문 전에 전화로 확인해 주세요.", style = T.Body.copy(color = C.Check))
                    }
                    if (p.hours != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, null, tint = C.Ink, modifier = Modifier.size(24.dp))
                            Text("  ${p.hours}" + (p.closedDays?.let { " · $it 휴무" } ?: ""), style = T.BodyL)
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Text("여기로 갈까요?", style = T.H2)
                Text("반려견이 탄 날은 ‘편한 길’로 안내해요", style = T.Caption)
                Spacer(Modifier.height(18.dp))
                val routing = u.route != null
                PrimaryButton(if (routing) "경유지로 추가" else "길 안내 시작", Modifier.fillMaxWidth(), Icons.Outlined.Navigation, height = 96.dp, fontSize = 26) {
                    if (routing) vm.chooseRestPlace(p) else vm.startRoute(p)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SecondaryButton("경유지 추가", Modifier.weight(1f), Icons.Outlined.AltRoute) { vm.chooseRestPlace(p) }
                    SecondaryButton("즐겨찾기", Modifier.weight(1f), Icons.Outlined.FavoriteBorder) {}
                }
                Spacer(Modifier.height(14.dp))
                SecondaryButton("전화로 확인하기", Modifier.fillMaxWidth(), Icons.Outlined.Phone) {}
            }
            // 3차 멍슐랭 요약 — 공식 정보와 분리(점선·파랑)
            if (p.userChecks > 0) {
                Card(Modifier.fillMaxWidth().weight(1f), border = C.User.copy(alpha = 0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Pets, null, tint = C.User, modifier = Modifier.size(28.dp))
                        Text("  멍슐랭 · 보호자 확인", style = T.Title, modifier = Modifier.weight(1f))
                        Badge("이용자 확인 ${p.userChecks}건 · ${p.userCheckedAt}", BadgeKind.USER)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("보호자가 다녀와서 남긴 확인이에요. 공식 동반 정보와 따로 보여 드려요. 후기 작성은 휴대폰에서 할 수 있어요.", style = T.Caption)
                }
            }
        }
    }
}

// ───────────────────────────── 코스 탐색 (WF-C04 · C05) ─────────────────────────────
@Composable
fun CourseListScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp)) {
        Text("코스 탐색", style = T.H1)
        Text("여러 장소를 잇는 드라이브 코스 · 조건 탐색(출발지·지역·이동 시간·테마)은 2차 · 샘플 데이터", style = T.Caption)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            (SampleData.courses + SampleData.feedCourse).forEach { c -> CourseTile(c, Modifier.weight(1f).fillMaxHeight()) { vm.openCourse(c) } }
        }
    }
}

@Composable
fun CourseDetailScreen(c: Course, vm: AppViewModel) {
    Row(Modifier.fillMaxSize().padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(Modifier.weight(1.3f).fillMaxHeight(), padding = 0.dp, color = C.MapLand) {
            MapArt(Modifier.fillMaxSize(), pins = c.stops.size, label = "코스 전체 경로 · 지도 SDK 영역")
        }
        Card(Modifier.weight(1f).fillMaxHeight(), padding = 0.dp) {
            Scene(c.image, Modifier.fillMaxWidth().height(160.dp), 0.dp)
            Column(Modifier.padding(28.dp)) {
                Text(c.name, style = T.Display)
                Text("${c.totalKm}km · 약 ${c.minutes / 60}시간 ${c.minutes % 60}분 · 테마 ${c.theme}", style = T.Body.copy(color = C.Sub))
                Spacer(Modifier.height(16.dp))
                var n = 0
                c.stops.forEach { s ->
                    val label = if (s.isRest) "휴" else (++n).toString()
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(if (s.isRest) C.OrangeSoft else C.Orange), contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (s.isRest) C.Orange else Color.White)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(s.place.name, style = T.Title, modifier = Modifier.weight(1f))
                        PlaceBadges(s.place, max = 1)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("방문지 3곳 이상은 구간별로 안내해요(경유지 최대 2곳).", style = T.Caption)
                Spacer(Modifier.height(10.dp))
                PrimaryButton("경로 안내 시작", Modifier.fillMaxWidth(), Icons.Outlined.Navigation, height = 96.dp, fontSize = 26) {
                    val first = c.stops.first { !it.isRest }.place
                    vm.startRoute(first, course = c, leg = 0)
                }
            }
        }
    }
}

@Composable
fun MapArt(modifier: Modifier, pins: Int, label: String) {
    Box(modifier.background(C.MapLand)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(C.MapWater, topLeft = androidx.compose.ui.geometry.Offset(w * 0.8f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.2f, h))
            val road = androidx.compose.ui.graphics.Path().apply { moveTo(0f, h * 0.3f); cubicTo(w * 0.3f, h * 0.35f, w * 0.5f, h * 0.2f, w * 0.8f, h * 0.25f) }
            drawPath(road, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(14f))
            val route = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.42f, h); cubicTo(w * 0.45f, h * 0.7f, w * 0.52f, h * 0.55f, w * 0.56f, h * 0.42f); cubicTo(w * 0.6f, h * 0.3f, w * 0.66f, h * 0.15f, w * 0.72f, 0f) }
            drawPath(route, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(26f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawPath(route, C.Orange, style = androidx.compose.ui.graphics.drawscope.Stroke(14f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            for (i in 0 until pins) {
                val t = (i + 1f) / (pins + 1f)
                drawCircle(Color.White, 22f, androidx.compose.ui.geometry.Offset(w * (0.43f + 0.28f * t), h * (1f - t)))
                drawCircle(C.Orange, 15f, androidx.compose.ui.geometry.Offset(w * (0.43f + 0.28f * t), h * (1f - t)))
            }
        }
        Text(label, style = T.Caption, modifier = Modifier.padding(20.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
