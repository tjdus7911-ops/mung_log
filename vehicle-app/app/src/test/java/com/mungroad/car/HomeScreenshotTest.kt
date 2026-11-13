package com.mungroad.car

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.mungroad.car.ui.AppViewModel
import com.mungroad.car.ui.Menu
import com.mungroad.car.ui.UiState
import com.mungroad.car.ui.components.AppHeader
import com.mungroad.car.ui.components.Sidebar
import com.mungroad.car.ui.screens.HomeScreen
import com.mungroad.car.ui.theme.GlassBackground
import com.mungroad.car.ui.theme.MungRoadTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 최종 시안과 비교하기 위한 차량 홈 스크린샷(Pleos Connect 1600×900, mdpi = 1dp:1px) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1600dp-h900dp-land-mdpi")
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun carHome() {
        val vm = AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        val u = UiState(restMinutes = 90, restReason = "노령견(중형 8살~)")
        compose.setContent {
            MungRoadTheme {
                GlassBackground(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize()) {
                        Sidebar(Menu.HOME, false) {}
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            AppHeader(24f, null, null, null)
                            Box(Modifier.weight(1f).fillMaxWidth()) { HomeScreen(u, vm) }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/car_home.png")
    }
}
