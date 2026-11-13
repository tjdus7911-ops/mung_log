package com.mungroad.car.vehicle

import android.Manifest
import android.annotation.SuppressLint
import android.car.Car
import android.car.VehicleGear
import android.car.VehicleIgnitionState
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Pleos Connect 차량 데이터 — AAOS 표준 Car API.
 * Pleos SDK API 정책(AAOS Alignment): 표준 데이터는 AAOS 표준 API, 제조사 특화 기능만 Pleos SDK.
 * 속성 지원은 차종·속성별로 달라 CarPropertyManager 로 확인하고, 차종명으로 분기하지 않습니다.
 */
class AaosVehicleDataProvider(private val context: Context) : VehicleDataProvider {
    override val name = "Pleos 차량 (AAOS Car API)"
    override val isSimulated = false

    companion object {
        fun isAutomotive(context: Context) = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }

    @SuppressLint("MissingPermission")
    override fun snapshots(): Flow<VehicleSnapshot> = callbackFlow {
        val car = Car.createCar(context)
        val pm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        var s = VehicleSnapshot(System.currentTimeMillis())
        var fuelCapacityMl: Float? = null
        fun emit(n: VehicleSnapshot) { s = n.copy(timeMillis = System.currentTimeMillis()); trySend(s) }

        runCatching {
            if (pm.isPropertyAvailable(VehiclePropertyIds.INFO_FUEL_CAPACITY, 0)) {
                fuelCapacityMl = pm.getFloatProperty(VehiclePropertyIds.INFO_FUEL_CAPACITY, 0)
            }
        }

        val cb = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(v: CarPropertyValue<*>) {
                if (v.status != CarPropertyValue.STATUS_AVAILABLE) return
                when (v.propertyId) {
                    VehiclePropertyIds.PERF_VEHICLE_SPEED -> emit(s.copy(speedKmh = (v.value as Float) * 3.6f))
                    VehiclePropertyIds.GEAR_SELECTION -> emit(s.copy(gear = when (v.value as Int) {
                        VehicleGear.GEAR_PARK -> Gear.PARK
                        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
                        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
                        VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
                        else -> Gear.DRIVE
                    }))
                    VehiclePropertyIds.IGNITION_STATE -> {
                        val st = v.value as Int
                        emit(s.copy(ignitionOn = st == VehicleIgnitionState.ON || st == VehicleIgnitionState.START))
                    }
                    VehiclePropertyIds.FUEL_LEVEL -> {
                        val ml = v.value as Float
                        val cap = fuelCapacityMl
                        emit(s.copy(fuelPercent = if (cap != null && cap > 0) ml / cap * 100f else null))
                    }
                    VehiclePropertyIds.FUEL_LEVEL_LOW -> emit(s.copy(fuelLow = v.value as Boolean))
                    VehiclePropertyIds.RANGE_REMAINING -> emit(s.copy(rangeKm = (v.value as Float) / 1000f))
                    VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> emit(s.copy(outsideTempC = v.value as Float))
                }
            }
            override fun onErrorEvent(propId: Int, zone: Int) {
                IntegrationLog.info("AAOS 속성 미지원/오류 id=$propId → 해당 기능만 끔")
            }
        }
        val props = listOf(
            VehiclePropertyIds.PERF_VEHICLE_SPEED to CarPropertyManager.SENSOR_RATE_NORMAL,
            VehiclePropertyIds.GEAR_SELECTION to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.IGNITION_STATE to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.FUEL_LEVEL to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.FUEL_LEVEL_LOW to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.RANGE_REMAINING to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        )
        for ((id, rate) in props) {
            val ok = runCatching { pm.getCarPropertyConfig(id) != null && pm.registerCallback(cb, id, rate) }.getOrDefault(false)
            IntegrationLog.api("CarPropertyManager.registerCallback(${VehiclePropertyIds.toString(id)}) → ${if (ok) "지원" else "미지원"}")
        }

        // 위치: Fused Location SDK 지원 여부가 차종마다 달라 표준 위치 API로 시작(기획서 '확인 전 대체 방식')
        val lm = context.getSystemService(LocationManager::class.java)
        val hasLoc = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val ll = LocationListener { loc -> emit(s.copy(lat = loc.latitude, lng = loc.longitude, accuracyM = if (loc.hasAccuracy()) loc.accuracy else null)) }
        if (hasLoc) runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, ll, Looper.getMainLooper()) }

        awaitClose {
            runCatching { pm.unregisterCallback(cb) }
            if (hasLoc) runCatching { lm.removeUpdates(ll) }
            runCatching { car.disconnect() }
        }
    }
}

/** 주행 제한 — 플랫폼이 전달한 제한 상태만 따르고 지역을 앱에서 판단하지 않습니다 */
class AaosRestrictionSource(private val context: Context) : DrivingRestrictionSource {
    override fun restrictions(): Flow<UxRestriction> {
        if (!AaosVehicleDataProvider.isAutomotive(context)) return flowOf(UxRestriction.NONE)
        return callbackFlow {
            val car = Car.createCar(context)
            val mgr = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            fun map(r: CarUxRestrictions?) = if (r?.isRequiresDistractionOptimization == true) UxRestriction.DRIVING else UxRestriction.NONE
            trySend(map(mgr.currentCarUxRestrictions))
            mgr.registerListener { r ->
                IntegrationLog.event("CarUxRestrictions 변경 → ${map(r)}")
                trySend(map(r))
            }
            IntegrationLog.api("CarUxRestrictionsManager.registerListener")
            awaitClose { runCatching { mgr.unregisterListener() }; runCatching { car.disconnect() } }
        }
    }
}
