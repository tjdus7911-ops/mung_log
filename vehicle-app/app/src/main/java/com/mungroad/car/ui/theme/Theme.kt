package com.mungroad.car.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Figma '06 디자인 시스템' — Premium Light Automotive UI 토큰 */
object C {
    val Cream = Color(0xFFF7F5F2)
    val Card = Color(0xFFFFFFFF)
    val Sunken = Color(0xFFF1EEEA)
    val Orange = Color(0xFFF58220)
    val OrangePressed = Color(0xFFE06F10)
    val OrangeSoft = Color(0xFFFFF1E4)
    val Ink = Color(0xFF242424)
    val Sub = Color(0xFF5F5F5F)
    val Tertiary = Color(0xFF777777)
    val Divider = Color(0xFFE9E6E2)
    val Verified = Color(0xFF1F8A5B)
    val VerifiedSoft = Color(0xFFE5F4EC)
    val Check = Color(0xFF9A6200)
    val CheckSoft = Color(0xFFFFF4D6)
    val User = Color(0xFF2F6FC9)
    val UserSoft = Color(0xFFE7F0FC)
    val Alert = Color(0xFFD64545)
    val AlertSoft = Color(0xFFFDECEC)
    val MapLand = Color(0xFFEEF0E8)
    val MapWater = Color(0xFFD6E8F2)
}

/** 차량 타이포 — 최소 18sp, 주행 중 핵심 수치 56sp */
object T {
    val Display = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 50.sp, color = C.Ink)
    val H1 = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp, color = C.Ink)
    val H2 = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 35.sp, color = C.Ink)
    val Title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp, color = C.Ink)
    val BodyL = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp, color = C.Ink)
    val Body = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, lineHeight = 30.sp, color = C.Ink)
    val Caption = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, lineHeight = 26.sp, color = C.Sub)
    val Number = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 62.sp, color = C.Ink)
}

/** 사진 대신 쓰는 장면 그라데이션(실서비스는 실제 장소·반려견 사진) */
fun sceneBrush(key: String): Brush = when (key) {
    "sea" -> Brush.verticalGradient(listOf(Color(0xFF9CC9EC), Color(0xFF4F95C9), Color(0xFF5E8F6B)))
    "park" -> Brush.verticalGradient(listOf(Color(0xFFCFE7C1), Color(0xFF8DBF74), Color(0xFF55874A)))
    "cafe" -> Brush.verticalGradient(listOf(Color(0xFFF6DDBF), Color(0xFFD8A878), Color(0xFF9C6B45)))
    "dogpark" -> Brush.verticalGradient(listOf(Color(0xFFE2F1D2), Color(0xFF9CCB7C), Color(0xFF5E9A4A)))
    "hero" -> Brush.horizontalGradient(listOf(Color(0xFFFBF3E8), Color(0xFFF7D9B4), Color(0xFFE9A86B)))
    else -> Brush.verticalGradient(listOf(Color(0xFFEDE7DF), Color(0xFFD9CFC2)))
}

@Composable
fun MungRoadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = C.Orange, background = C.Cream, surface = C.Card, onPrimary = Color.White),
        content = content,
    )
}
