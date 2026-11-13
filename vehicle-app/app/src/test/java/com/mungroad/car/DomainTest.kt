package com.mungroad.car

import com.mungroad.car.data.Health
import com.mungroad.car.data.Pet
import com.mungroad.car.data.SampleData
import com.mungroad.car.domain.DriveState
import com.mungroad.car.domain.DriveTracker
import com.mungroad.car.domain.DriverContext
import com.mungroad.car.domain.GateDecision
import com.mungroad.car.domain.Notice
import com.mungroad.car.domain.NotificationGate
import com.mungroad.car.domain.Priority
import com.mungroad.car.domain.RestCandidates
import com.mungroad.car.domain.RestEngine
import com.mungroad.car.domain.RestGuideline
import com.mungroad.car.vehicle.Gear
import com.mungroad.car.vehicle.VehicleSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestGuidelineTest {
    private fun pet(age: Double, kg: Double = 12.0, health: Set<Health> = emptySet(), brachy: Boolean = false, vet: Int? = null) =
        Pet("p", "테스트", "믹스", age, kg, brachycephalic = brachy, health = health, vetRestMinutes = vet)

    @Test fun adultDefault120() = assertEquals(120, RestGuideline.recommend(pet(4.0)).minutes)
    @Test fun seniorMedium90() = assertEquals(90, RestGuideline.recommend(pet(9.0)).minutes) // 사슴이: 중형 9살
    @Test fun smallDogNotSeniorAt9() = assertEquals(120, RestGuideline.recommend(pet(9.0, kg = 5.0)).minutes)
    @Test fun puppy() = assertEquals(60, RestGuideline.recommend(pet(0.3)).minutes)
    @Test fun kidneyShortest() = assertEquals(60, RestGuideline.recommend(pet(9.0, health = setOf(Health.KIDNEY))).minutes)
    @Test fun brachy90() = assertEquals(90, RestGuideline.recommend(pet(3.0, brachy = true)).minutes)
    @Test fun vetFirst() = assertEquals(150, RestGuideline.recommend(pet(0.3, vet = 150)).minutes)
    @Test fun multiplePetsShortest() = assertEquals(60, RestGuideline.forPassengers(listOf(pet(4.0), pet(0.3))).minutes)
}

class DriveTrackerTest {
    private fun s(t: Long, kmh: Float?, gear: Gear = Gear.DRIVE) = VehicleSnapshot(t * 1000, speedKmh = kmh, gear = gear)

    @Test fun countsOnlyDrivingTime() {
        val tr = DriveTracker()
        var t = 0L
        repeat(60) { tr.update(s(t++, 80f)) }   // 59초 주행
        repeat(60) { tr.update(s(t++, 0f)) }    // 정체·신호 대기
        repeat(31) { tr.update(s(t++, 60f)) }   // 30초 주행
        assertEquals(89_000L, tr.drivingMillis)
    }

    @Test fun parkedIsNotDriving() {
        val tr = DriveTracker()
        assertEquals(DriveState.PARKED, tr.update(s(0, 0f, Gear.PARK)))
        assertEquals(DriveState.UNKNOWN, tr.update(VehicleSnapshot(1000)))
    }

    @Test fun resetRestTimerKeepsTotal() {
        val tr = DriveTracker()
        for (t in 0L..120L) tr.update(s(t, 50f))
        tr.resetRestTimer()
        assertEquals(0L, tr.sinceRestMillis)
        assertEquals(120_000L, tr.drivingMillis)
    }
}

class RestEngineTest {
    private val min = 60_000L

    @Test fun preNoticeThenDueOnce() {
        val e = RestEngine(90)
        assertNull(e.check(79 * min))
        assertTrue(e.check(80 * min) is RestEngine.Event.PreNotice)
        assertNull(e.check(85 * min))
        assertEquals(RestEngine.Event.Due, e.check(90 * min))
        assertNull(e.check(95 * min))
    }

    @Test fun snoozeRemindsOnlyOnce() {
        val e = RestEngine(90)
        e.check(90 * min); e.snooze(90 * min)
        assertNull(e.check(100 * min))
        assertEquals(RestEngine.Event.Reminder, e.check(105 * min))
        assertNull(e.check(130 * min))
    }

    @Test fun resetStartsOver() {
        val e = RestEngine(90)
        e.check(90 * min); e.reset()
        assertTrue(e.check(80 * min) is RestEngine.Event.PreNotice)
    }
}

class NotificationGateTest {
    @Test fun waitsForTurnSignalThenSpeaks() {
        val g = NotificationGate()
        g.offer(Notice("due", Priority.REST, "휴식", 0))
        assertTrue(g.next(DriverContext(turnSignalOn = true), 1000) is GateDecision.Wait)
        assertTrue(g.next(DriverContext(turnSignalOn = false, turnSignalOffAtMillis = 1000), 2000) is GateDecision.Wait)
        assertTrue(g.next(DriverContext(turnSignalOn = false, turnSignalOffAtMillis = 1000), 5000) is GateDecision.Speak)
    }

    @Test fun safetyBeforeRestAndDedup() {
        val g = NotificationGate()
        g.offer(Notice("due", Priority.REST, "휴식", 0))
        g.offer(Notice("due", Priority.REST, "휴식", 1))
        g.offer(Notice("ac", Priority.SAFETY, "에어컨", 2))
        val d = g.next(DriverContext(), 10) as GateDecision.Speak
        assertEquals("ac", d.notice.id)
        assertEquals(1, g.pending().size)
    }

    @Test fun afterThreeMinutesCardOnly() {
        val g = NotificationGate()
        g.offer(Notice("due", Priority.REST, "휴식", 0))
        assertTrue(g.next(DriverContext(nextTurnMeters = 200), 3 * 60_000) is GateDecision.CardOnly)
    }

    @Test fun unknownSignalsAreSkipped() {
        val g = NotificationGate()
        g.offer(Notice("due", Priority.REST, "휴식", 0))
        assertTrue(g.next(DriverContext(turnSignalOn = null, nextTurnMeters = null, inCautionZone = null), 10) is GateDecision.Speak)
    }
}

class RestCandidatesTest {
    @Test fun fuelLowPutsFuelFirst() {
        val ahead = listOf(SampleData.pyeongchang to 10.0, SampleData.okgye to 30.0, SampleData.shade to 5.0)
        val r = RestCandidates.pick(ahead, fuelLow = true, evLow = false)
        assertEquals("옥계휴게소", r.first().place.name)
    }

    @Test fun verifiedBeforeCheckNeeded() {
        val ahead = listOf(SampleData.shade to 5.0, SampleData.pyeongchang to 20.0)
        val r = RestCandidates.pick(ahead, fuelLow = false, evLow = false)
        assertEquals("평창휴게소", r.first().place.name)
    }

    @Test fun excludesBehindAndFar() {
        val ahead = listOf(SampleData.okgye to -3.0, SampleData.pyeongchang to 120.0)
        assertTrue(RestCandidates.pick(ahead, false, false).isEmpty())
    }
}
