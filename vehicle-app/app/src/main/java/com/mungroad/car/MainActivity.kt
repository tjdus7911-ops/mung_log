package com.mungroad.car

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mungroad.car.ui.AppViewModel
import com.mungroad.car.ui.Menu
import com.mungroad.car.ui.Screen
import com.mungroad.car.ui.components.AppHeader
import com.mungroad.car.ui.components.Sidebar
import com.mungroad.car.ui.screens.ArrivedScreen
import com.mungroad.car.ui.screens.CourseDetailScreen
import com.mungroad.car.ui.screens.CourseListScreen
import com.mungroad.car.ui.screens.DriveScreen
import com.mungroad.car.ui.screens.HomeScreen
import com.mungroad.car.ui.screens.LockOverlay
import com.mungroad.car.ui.screens.PlaceDetailScreen
import com.mungroad.car.ui.screens.QuickCheckScreen
import com.mungroad.car.ui.screens.RestingScreen
import com.mungroad.car.ui.screens.SettingsScreen
import com.mungroad.car.ui.screens.SpotListScreen
import com.mungroad.car.ui.screens.TripConfirmScreen
import com.mungroad.car.ui.screens.TripsScreen
import com.mungroad.car.ui.screens.WrapUpScreen
import com.mungroad.car.ui.theme.C
import com.mungroad.car.ui.theme.MungRoadTheme
import com.mungroad.car.vehicle.IntegrationLog

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MungRoadTheme { App(vm) } }
    }
}

@Composable
private fun App(vm: AppViewModel) {
    val u by vm.ui.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO, "android.car.permission.CAR_SPEED"))
    }
    // 음성 요청: Gleo AI SpeechToText 는 시그니처 검증 후 연결 예정 → 표준 RecognizerIntent, 실패 시 시연 문장
    var demoVoiceIdx by remember { mutableStateOf(0) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (text != null) vm.onVoice(text)
    }
    val onMic: () -> Unit = {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        val ok = !u.demoMode && runCatching { voice.launch(i) }.isSuccess
        if (!ok) {
            // 데모: 시연 문장 순환
            val samples = when {
                u.card is com.mungroad.car.ui.DriveCard.RestDue -> listOf("응")
                u.card is com.mungroad.car.ui.DriveCard.Suggestions -> listOf("1번")
                u.card is com.mungroad.car.ui.DriveCard.AirCon -> listOf("응 켜 줘")
                else -> listOf("근처 강아지 동반 카페 찾아줘", "사슴이랑 갈 만한 공원 찾아줘")
            }
            vm.onVoice(samples[demoVoiceIdx % samples.size]); demoVoiceIdx++
        }
    }

    Box(Modifier.fillMaxSize().background(C.Cream)) {
        Row(Modifier.fillMaxSize()) {
            val full = u.screen is Screen.Drive
            if (!full) Sidebar(u.menu, vm.isDriving()) { vm.select(it) }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (!full) {
                    val back: (() -> Unit)? = when (u.screen) {
                        is Screen.PlaceDetail -> { { vm.select(Menu.SPOT) } }
                        is Screen.CourseDetail -> { { vm.select(Menu.COURSE) } }
                        is Screen.TripConfirm -> { { vm.select(Menu.TRIPS) } }
                        else -> null
                    }
                    val status = if (u.route != null && u.screen !is Screen.Drive) "안내 중 · ${u.route!!.destination.name}" else null
                    AppHeader(u.snapshot?.outsideTempC, status, back, when (u.screen) { is Screen.PlaceDetail -> "멍스팟"; is Screen.CourseDetail -> "코스 탐색"; is Screen.TripConfirm -> "내 여행"; else -> null })
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (val s = u.screen) {
                        Screen.Home -> HomeScreen(u, vm)
                        Screen.Spots -> SpotListScreen(u, vm)
                        is Screen.PlaceDetail -> PlaceDetailScreen(s.place, u, vm)
                        Screen.Courses -> CourseListScreen(vm)
                        is Screen.CourseDetail -> CourseDetailScreen(s.course, vm)
                        Screen.Trips -> TripsScreen(vm)
                        is Screen.TripConfirm -> TripConfirmScreen(s.trip, u, vm)
                        Screen.Settings -> SettingsScreen(u, vm)
                        Screen.Drive -> DriveScreen(u, vm, onMic)
                        Screen.Resting -> RestingScreen(u, vm)
                        is Screen.Arrived -> ArrivedScreen(s.name, u, vm)
                        Screen.WrapUp -> WrapUpScreen(u, vm)
                        is Screen.QuickCheck -> QuickCheckScreen(s.place, u, vm)
                    }
                }
            }
            if (u.showLog) IntegrationLogPanel()
        }
        if (u.route != null && u.screen !is Screen.Drive && u.screen !is Screen.Resting && u.screen !is Screen.Arrived && u.screen !is Screen.WrapUp) {
            Text(
                "드라이브 모드로 →", color = Color.White, fontSize = 20.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp).clip(RoundedCornerShape(30.dp)).background(C.Orange).clickable { vm.openDrive() }.padding(horizontal = 28.dp, vertical = 14.dp),
            )
        }
        u.lockedMessage?.let { LockOverlay(it, onMic) { vm.dismissLock() } }
    }
}

/** 시연 영상용 연동 로그(서비스 화면에는 넣지 않음) */
@Composable
private fun IntegrationLogPanel() {
    val entries by IntegrationLog.entries.collectAsState()
    Column(Modifier.width(300.dp).fillMaxHeight().background(Color(0xFF1F2328)).padding(14.dp)) {
        Text("연동 로그 · 시연용", color = Color(0xFFF58220), fontSize = 16.sp)
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries) { e ->
                Text(
                    "${e.time} ${e.kind}\n${e.text}",
                    color = when (e.kind) { "API" -> Color(0xFF9CC9FF); "EVENT" -> Color(0xFF9BE3B5); else -> Color(0xFFBBBBBB) },
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
