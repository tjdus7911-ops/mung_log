package com.mungroad.car.domain

import com.mungroad.car.vehicle.Gear
import com.mungroad.car.vehicle.VehicleSnapshot
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class DriveState { PARKED, STOPPED, DRIVING, UNKNOWN }

/**
 * 실제 주행 시간 누적기.
 * 차량은 속도·기어·시동으로 '달린 시간'과 '멈춘 시간'을 직접 구분할 수 있어(AAOS PERF_VEHICLE_SPEED·GEAR_SELECTION·IGNITION_STATE)
 * 휴대폰 위치 추정과 달리 신호 대기·정체를 주행 시간에서 뺄 수 있습니다.
 */
class DriveTracker(private val movingKmh: Float = 5f) {
    var drivingMillis: Long = 0; private set
    var sinceRestMillis: Long = 0; private set
    var distanceKm: Double = 0.0; private set
    var state: DriveState = DriveState.UNKNOWN; private set

    private var lastTime: Long? = null
    private var lastLat: Double? = null
    private var lastLng: Double? = null

    fun classify(s: VehicleSnapshot): DriveState = when {
        s.gear == Gear.PARK || s.ignitionOn == false -> DriveState.PARKED
        s.speedKmh == null -> DriveState.UNKNOWN
        s.speedKmh >= movingKmh -> DriveState.DRIVING
        else -> DriveState.STOPPED
    }

    fun update(s: VehicleSnapshot): DriveState {
        val st = classify(s)
        val prev = lastTime
        if (prev != null && s.timeMillis > prev) {
            val dt = s.timeMillis - prev
            // 앱이 잠시 멈췄다 돌아온 경우(예: 10분 이상 공백)는 누적하지 않음
            if (dt < 10 * 60_000L && state == DriveState.DRIVING && st == DriveState.DRIVING) {
                drivingMillis += dt
                sinceRestMillis += dt
            }
        }
        if (s.lat != null && s.lng != null && (s.accuracyM ?: 0f) <= 50f) {
            val pl = lastLat; val pg = lastLng
            if (pl != null && pg != null) {
                val d = haversineKm(pl, pg, s.lat, s.lng)
                // 비정상 급증(순간이동) 제거: 1초에 100m 이상 + 5km 이상 점프는 버림
                val dtSec = ((s.timeMillis - (prev ?: s.timeMillis)) / 1000.0).coerceAtLeast(1.0)
                if (d < 5.0 || d / dtSec < 0.1) distanceKm += d
            }
            lastLat = s.lat; lastLng = s.lng
        }
        lastTime = s.timeMillis
        state = st
        return st
    }

    /** 휴식을 마치고 다시 출발하면 주기를 처음부터 셉니다 */
    fun resetRestTimer() { sinceRestMillis = 0 }

    fun restore(drivingMillis: Long, sinceRestMillis: Long, distanceKm: Double) {
        this.drivingMillis = drivingMillis; this.sinceRestMillis = sinceRestMillis; this.distanceKm = distanceKm
    }

    companion object {
        fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
            return 2 * r * asin(sqrt(a))
        }
    }
}
