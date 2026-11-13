package com.mungroad.car.sdk

import android.content.Context
import com.mungroad.car.vehicle.CabinSource
import com.mungroad.car.vehicle.NavigationProvider
import com.mungroad.car.vehicle.Speaker

/**
 * Pleos SDK 미포함 빌드(mungroad.usePleosSdk=false)용 Stub.
 * 일반 Android 에뮬레이터·휴대폰에서도 앱이 실행되고, 데모 모드·표준 API 대체 동작으로 같은 흐름을 보여 줍니다.
 */
object PleosBindings {
    const val ENABLED = false
    fun navigation(context: Context): NavigationProvider? = null
    fun cabin(context: Context): CabinSource? = null
    fun speaker(context: Context): Speaker? = null
    fun status(): String = "Pleos SDK 미포함 빌드 · 데모/표준 API 대체 동작"
}
