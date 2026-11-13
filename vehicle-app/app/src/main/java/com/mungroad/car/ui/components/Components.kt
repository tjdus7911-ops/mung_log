package com.mungroad.car.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mungroad.car.data.Place
import com.mungroad.car.data.Verification
import com.mungroad.car.ui.Menu
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import com.mungroad.car.ui.theme.BlueCta
import com.mungroad.car.ui.theme.C
import com.mungroad.car.ui.theme.Glass
import com.mungroad.car.ui.theme.GlassShadow
import com.mungroad.car.ui.theme.glass
import com.mungroad.car.ui.theme.T
import com.mungroad.car.ui.theme.sceneBrush

@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 28.dp, color: Color = C.Card, border: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .then(if (color == C.Card) Modifier.glass(Glass.Regular, 24.dp) else Modifier.shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = GlassShadow, spotColor = GlassShadow).clip(RoundedCornerShape(24.dp)).background(color))
            .then(if (border != null) Modifier.border(2.dp, border, RoundedCornerShape(24.dp)) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(icon: ImageVector, title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = C.Orange, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = T.H2, modifier = Modifier.weight(1f))
        if (action != null) Text("$action  →", style = T.Caption.copy(fontWeight = FontWeight.Medium), modifier = Modifier.clickable { onAction?.invoke() }.padding(8.dp))
    }
}

/** 버튼: 최소 터치 영역 64dp, 주행 중 버튼은 96dp 이상 */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 72.dp, fontSize: Int = 22, onClick: () -> Unit) {
    Row(
        modifier
            .height(height)
            .shadow(12.dp, RoundedCornerShape(height / 2), ambientColor = C.Orange.copy(alpha = 0.35f), spotColor = C.Orange.copy(alpha = 0.45f))
            .clip(RoundedCornerShape(height / 2))
            .background(BlueCta)
            .background(Brush.verticalGradient(0f to Color(0x40FFFFFF), 0.5f to Color(0x00FFFFFF)))
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size((fontSize + 6).dp)); Spacer(Modifier.width(12.dp)) }
        Text(text, color = Color.White, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 72.dp, fontSize: Int = 22, onClick: () -> Unit) {
    Row(
        modifier
            .height(height)
            .glass(Glass.Regular, height / 2)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = C.Ink, modifier = Modifier.size((fontSize + 4).dp)); Spacer(Modifier.width(10.dp)) }
        Text(text, color = C.Ink, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(52.dp)
            .then(if (selected) Modifier.shadow(8.dp, CircleShape, ambientColor = C.Orange.copy(alpha = 0.35f), spotColor = C.Orange.copy(alpha = 0.4f)).clip(CircleShape).background(BlueCta) else Modifier.glass(Glass.Regular, 26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (selected) Color.White else C.Ink, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 20.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) Color.White else C.Ink)
    }
}

enum class BadgeKind { VERIFIED, CHECK, USER, INFO }

@Composable
fun Badge(text: String, kind: BadgeKind) {
    val (bg, fg) = when (kind) {
        BadgeKind.VERIFIED -> C.VerifiedSoft to C.Verified
        BadgeKind.CHECK -> C.CheckSoft to C.Check
        BadgeKind.USER -> C.UserSoft to C.User
        BadgeKind.INFO -> C.Sunken to C.Sub
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(if (kind == BadgeKind.CHECK) Modifier.border(1.dp, fg.copy(alpha = 0.6f), RoundedCornerShape(10.dp)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1) }
}

/** 장소 확인 상태 배지 — 확인된 동반 조건만 확정 표시, 이용자 확인(멍슐랭)은 공식과 분리 */
@Composable
fun PlaceBadges(p: Place, max: Int = 2, showUser: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (p.verification == Verification.VERIFIED) p.conditions.take(max).forEach { Badge(it, BadgeKind.VERIFIED) }
        else Badge("방문 전 확인 필요", BadgeKind.CHECK)
        if (showUser && p.userChecks > 0) Badge("이용자 확인 ${p.userChecks}건 · ${p.userCheckedAt}", BadgeKind.USER)
    }
}

@Composable
fun Scene(key: String, modifier: Modifier = Modifier, radius: Dp = 18.dp, label: String? = null) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(sceneBrush(key)), contentAlignment = Alignment.Center) {
        if (label != null) Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, size: Dp = 64.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).glass(Glass.Strong, size / 2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = C.Ink, modifier = Modifier.size(size * 0.45f)) }
}

private fun menuIcon(m: Menu): ImageVector = when (m) {
    Menu.HOME -> Icons.Outlined.Home
    Menu.COURSE -> Icons.Outlined.Explore
    Menu.SPOT -> Icons.Outlined.Pets
    Menu.TRIPS -> Icons.Outlined.CalendarMonth
    Menu.SETTINGS -> Icons.Outlined.Settings
}

@Composable
fun Sidebar(active: Menu, locked: Boolean, onSelect: (Menu) -> Unit) {
    // 시안: 왼쪽 세로 유리 레일 · 위쪽 로고 · 선택 메뉴만 유리 필 강조
    Column(
        Modifier.width(149.dp).fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(Color(0x8CFFFFFF), Color(0x59F3F8FF))))
            .background(Brush.verticalGradient(0f to Color(0x4DFFFFFF), 0.3f to Color(0x00FFFFFF)))
            .drawBehind { drawLine(Color(0xB3FFFFFF), androidx.compose.ui.geometry.Offset(size.width - 1f, 0f), androidx.compose.ui.geometry.Offset(size.width - 1f, size.height), 2f) }
            .padding(top = 42.dp, start = 21.dp, end = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).shadow(14.dp, RoundedCornerShape(18.dp), ambientColor = C.Orange.copy(alpha = 0.4f), spotColor = C.Orange.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF6CB0FF), Color(0xFF2F6FED))))
                .border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Pets, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(53.dp))
        Menu.entries.forEach { m ->
            val on = m == active
            val dim = locked && m != Menu.HOME
            Column(
                Modifier
                    .width(118.dp).height(92.dp)
                    .then(if (on) Modifier.glass(Glass.Strong, 22.dp) else Modifier.clip(RoundedCornerShape(22.dp)))
                    .clickable { onSelect(m) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(menuIcon(m), null, tint = if (on) C.Orange else if (dim) C.Divider else C.Ink.copy(alpha = 0.85f), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(6.dp))
                Text(m.label, fontSize = 18.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) C.Orange else if (dim) C.Tertiary else C.Ink.copy(alpha = 0.85f))
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
fun AppHeader(outsideTemp: Float?, status: String? = null, onBack: (() -> Unit)? = null, backLabel: String? = null) {
    Row(Modifier.fillMaxWidth().height(112.dp).padding(start = 38.dp, end = 50.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("멍로드", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp, color = C.Ink)
        Spacer(Modifier.width(16.dp))
        Text("반려견과 떠나는 모든 길", style = T.Caption.copy(fontSize = 19.sp, color = C.Sub))
        if (onBack != null) {
            Spacer(Modifier.width(32.dp))
            Row(Modifier.clip(CircleShape).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ArrowBack, null, tint = C.Sub, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(6.dp))
                Text(backLabel ?: "뒤로", style = T.BodyL.copy(color = C.Sub))
            }
        }
        Spacer(Modifier.weight(1f))
        if (status != null) {
            Text(status, style = T.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 24.dp))
        }
        // 시간·통신 상태는 Pleos 시스템 상태바가 표시 → 앱은 바깥 기온만(ENV_OUTSIDE_TEMPERATURE)
        if (outsideTemp != null) {
            Icon(Icons.Outlined.Cloud, null, tint = C.Ink, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Text("${outsideTemp.toInt()}°", style = T.Title.copy(fontSize = 24.sp))
            Spacer(Modifier.width(8.dp))
            Text("바깥", style = T.Caption.copy(color = C.Sub))
        }
    }
}

@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier, big: Boolean = false) {
    Column(modifier) {
        Text(label, style = T.Body.copy(color = C.Sub))
        Text(value, style = if (big) T.Number else T.H1, maxLines = 1)
    }
}

@Composable
fun EmptyNote(text: String) {
    Text(text, style = T.Body.copy(color = C.Sub), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
}
