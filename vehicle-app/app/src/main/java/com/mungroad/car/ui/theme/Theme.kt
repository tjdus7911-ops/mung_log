package com.mungroad.car.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mungroad.car.R

/**
 * 최종 시안(2026-11-13) — 프리미엄 블루 글래스모피즘.
 * 기존 토큰 이름(C.Orange 등)은 다른 화면 코드가 그대로 쓰도록 유지하고 값만 블루 브랜드로 바꿉니다.
 */
object C {
    // 바탕·면
    val Cream = Color(0xFFEAF2FC)          // 앱 바탕(아이스 블루)
    val Card = Color(0xFFFFFFFF)
    val Sunken = Color(0xFFE3ECF8)
    // 브랜드(블루)
    val Orange = Color(0xFF2F6FED)          // 이름은 호환용 — 브랜드 블루
    val OrangePressed = Color(0xFF2259CC)
    val OrangeSoft = Color(0xFFE4EEFD)
    val BlueLight = Color(0xFF5FA4FF)
    // 글자
    val Ink = Color(0xFF14203A)
    val Sub = Color(0xFF4B5873)
    val Tertiary = Color(0xFF7C88A3)
    val Divider = Color(0xFFD9E4F3)
    // 의미 색(시안 배지 톤)
    val Verified = Color(0xFF1F8A5B)
    val VerifiedSoft = Color(0xFFE2F4EA)
    val Check = Color(0xFFA16207)
    val CheckSoft = Color(0xFFFDF0D5)
    val User = Color(0xFF2F6FC9)
    val UserSoft = Color(0xFFE7F0FC)
    val Alert = Color(0xFFD64545)
    val AlertSoft = Color(0xFFFDECEC)
    val DDay = Color(0xFFF0691F)            // 시안의 D-day 강조색
    val MapLand = Color(0xFFE6EEF8)
    val MapWater = Color(0xFFD3E4F6)
}

/** 유리 패널 단계 — 시안의 카드별 투명도·깊이 차이 */
enum class Glass(val fill: Color, val fill2: Color, val border: Color, val shadow: Dp) {
    /** 강조 카드(다음 여행) — 가장 밝고 불투명 */
    Strong(Color(0xF2FFFFFF), Color(0xD9F3F8FF), Color(0xF2FFFFFF), 18.dp),
    /** 일반 카드·목록 줄 */
    Regular(Color(0xB8FFFFFF), Color(0x8CEAF2FD), Color(0xCCFFFFFF), 10.dp),
    /** 옅은 블루 유리(반려견 카드·레일) */
    Tint(Color(0x99F4F8FF), Color(0x80DCE8FA), Color(0xB3FFFFFF), 10.dp),
}

val GlassShadow = Color(0x332E5FB8)

/** 유리 질감: 위·왼쪽에서 들어오는 하이라이트 + 밝은 1px 테두리 + 확산 그림자 */
fun Modifier.glass(level: Glass = Glass.Regular, radius: Dp = 24.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(level.shadow, shape, ambientColor = GlassShadow, spotColor = GlassShadow)
        .clip(shape)
        .background(Brush.linearGradient(listOf(level.fill, level.fill2), start = Offset.Zero, end = Offset.Infinite))
        .background(Brush.verticalGradient(0f to Color(0x66FFFFFF), 0.35f to Color(0x00FFFFFF)))
        .border(1.dp, Brush.linearGradient(listOf(level.border, Color(0x66FFFFFF), level.border), start = Offset.Zero, end = Offset.Infinite), shape)
}

/** 주요 버튼·선택 상태: 블루 그라데이션 */
val BlueCta = Brush.horizontalGradient(listOf(Color(0xFF5DA6FF), Color(0xFF3A7CF5), Color(0xFF2F6FED)))

/** 앱 바탕 — 페일 블루 그라데이션 + 곡선 광원 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(Brush.linearGradient(listOf(Color(0xFFF1F6FE), Color(0xFFE2EDFB), Color(0xFFD5E5FA)), start = Offset.Zero, end = Offset.Infinite))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0x80FFFFFF), Color(0x00FFFFFF)), center = Offset(size.width * 0.18f, size.height * 0.05f), radius = size.width * 0.45f), radius = size.width * 0.45f, center = Offset(size.width * 0.18f, size.height * 0.05f))
            drawCircle(Brush.radialGradient(listOf(Color(0x4D9CC4FF), Color(0x009CC4FF)), center = Offset(size.width * 0.92f, size.height * 0.95f), radius = size.width * 0.4f), radius = size.width * 0.4f, center = Offset(size.width * 0.92f, size.height * 0.95f))
            val p = Path().apply {
                moveTo(size.width * 0.35f, -20f)
                cubicTo(size.width * 0.55f, size.height * 0.25f, size.width * 0.75f, size.height * 0.05f, size.width + 40f, size.height * 0.30f)
            }
            drawPath(p, Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x99FFFFFF), Color(0x00FFFFFF))), style = Stroke(width = 60f))
            val p2 = Path().apply {
                moveTo(-20f, size.height * 0.80f)
                cubicTo(size.width * 0.25f, size.height * 0.62f, size.width * 0.45f, size.height * 1.0f, size.width * 0.75f, size.height + 30f)
            }
            drawPath(p2, Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x66FFFFFF), Color(0x00FFFFFF))), style = Stroke(width = 46f))
        }
        content()
    }
}

/** Pretendard(가변) — 시안과 같은 계열의 한글 산세리프 */
val Pretendard = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.pretendard_variable, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** 차량 타이포 — 최소 18sp, 주행 중 핵심 수치 56sp */
object T {
    val Display = TextStyle(fontFamily = Pretendard, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 56.sp, letterSpacing = (-1).sp, color = C.Ink)
    val H1 = TextStyle(fontFamily = Pretendard, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp, letterSpacing = (-0.6).sp, color = C.Ink)
    val H2 = TextStyle(fontFamily = Pretendard, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 35.sp, letterSpacing = (-0.5).sp, color = C.Ink)
    val Title = TextStyle(fontFamily = Pretendard, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp, letterSpacing = (-0.3).sp, color = C.Ink)
    val BodyL = TextStyle(fontFamily = Pretendard, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp, color = C.Ink)
    val Body = TextStyle(fontFamily = Pretendard, fontSize = 20.sp, fontWeight = FontWeight.Normal, lineHeight = 30.sp, color = C.Ink)
    val Caption = TextStyle(fontFamily = Pretendard, fontSize = 18.sp, fontWeight = FontWeight.Normal, lineHeight = 26.sp, color = C.Sub)
    val Number = TextStyle(fontFamily = Pretendard, fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 62.sp, color = C.Ink)
}

/** 사진 대신 쓰는 장면 그라데이션(확인된 장소 사진이 없을 때) */
fun sceneBrush(key: String): Brush = when (key) {
    "sea" -> Brush.verticalGradient(listOf(Color(0xFFB9D7F5), Color(0xFF6FA6DB), Color(0xFF5A8FB8)))
    "park" -> Brush.verticalGradient(listOf(Color(0xFFD5E9D2), Color(0xFF9CC79A), Color(0xFF6E9E78)))
    "cafe" -> Brush.verticalGradient(listOf(Color(0xFFEFE3D6), Color(0xFFD7BFA6), Color(0xFFAE8E72)))
    "dogpark" -> Brush.verticalGradient(listOf(Color(0xFFE0F0DC), Color(0xFFA7D09A), Color(0xFF6FA06A)))
    "hero" -> Brush.horizontalGradient(listOf(Color(0xFFEAF2FD), Color(0xFFC9DDF8), Color(0xFF8DB6F0)))
    else -> Brush.verticalGradient(listOf(Color(0xFFE9EFF8), Color(0xFFD3DEEE)))
}

@Composable
fun MungRoadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = C.Orange, background = C.Cream, surface = C.Card, onPrimary = Color.White),
    ) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Pretendard)) { content() }
    }
}
