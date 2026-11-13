package com.mungroad.car.data

/** 반려견 크기(체급). 노령 기준 나이가 체급별로 다릅니다(PetMD). */
enum class DogSize(val label: String, val seniorAge: Int) {
    SMALL("소형", 10), MEDIUM("중형", 8), LARGE("대형", 7), GIANT("초대형", 6);

    companion object {
        fun fromWeight(kg: Double): DogSize = when {
            kg < 10 -> SMALL
            kg < 25 -> MEDIUM
            kg < 45 -> LARGE
            else -> GIANT
        }
    }
}

enum class Health(val label: String) {
    NONE("특이사항 없음"), JOINT("관절"), HEART("심장"), RESPIRATORY("호흡기"), KIDNEY("신장 질환"), DIABETES("당뇨")
}

data class Pet(
    val id: String,
    val name: String,
    val breed: String,
    val ageYears: Double,
    val weightKg: Double,
    val brachycephalic: Boolean = false,
    val health: Set<Health> = emptySet(),
    val activity: String = "보통",
    val caution: String = "",
    /** 보호자가 직접 정한 휴식 간격(분). null 이면 기준표 추천값 */
    val customRestMinutes: Int? = null,
    /** 수의사가 정한 간격(분). 있으면 가장 먼저 적용 */
    val vetRestMinutes: Int? = null,
) {
    val size: DogSize get() = DogSize.fromWeight(weightKg)
}

enum class Category(val label: String) {
    ALL("전체"), CAFE("카페"), RESTAURANT("식당"), PARK("공원"), STAY("숙소"), FACILITY("동반 시설"), REST_AREA("휴게소")
}

/** 장소 정보 확인 상태 — 기획서 '확인된 정보만 확정' 원칙 */
enum class Verification(val label: String) {
    /** 공공 데이터·제휴 장소가 확인한 정보: 배지 + 출처 + 확인일 */
    VERIFIED("확인됨"),
    /** 동반 정보가 없거나 오래됨: 동반 여부를 확정하지 않음 */
    CHECK_NEEDED("방문 전 확인 필요"),
}

data class Place(
    val id: String,
    val name: String,
    val category: Category,
    val lat: Double,
    val lng: Double,
    val address: String,
    val verification: Verification,
    /** 확인된 동반 조건(확인됨일 때만 채움) */
    val conditions: List<String> = emptyList(),
    /** 확인된 제한(예: 대형견 불가) */
    val restrictions: List<String> = emptyList(),
    val source: String? = null,
    val verifiedAt: String? = null,
    val hours: String? = null,
    val closedDays: String? = null,
    val phone: String? = null,
    /** 휴게소 부가 정보 */
    val hasDogPlayground: Boolean = false,
    val hasFuel: Boolean = false,
    val hasEvCharger: Boolean = false,
    /** 3차 멍슐랭: 보호자 확인 건수·최근 확인일(공식 정보와 분리) */
    val userChecks: Int = 0,
    val userCheckedAt: String? = null,
    val image: String = "place",
)

data class CourseStop(val place: Place, val isRest: Boolean = false)

data class Course(
    val id: String,
    val name: String,
    val region: String,
    val theme: String,
    val totalKm: Int,
    val minutes: Int,
    val stops: List<CourseStop>,
    val image: String,
    /** 3차: 여행 피드에서 담아 온 코스 */
    val fromFeed: Boolean = false,
)

data class Trip(
    val id: String,
    val name: String,
    val dateLabel: String,
    val origin: String,
    val destination: Place,
    /** NaviHelper 경유지는 최대 2곳 */
    val waypoints: List<Place>,
    val petIds: List<String>,
    val sample: Boolean = true,
)

enum class RestStartMethod { BUTTON, VOICE, DOOR }

data class RestRecord(val startedAtMillis: Long, val endedAtMillis: Long?, val placeName: String?, val method: RestStartMethod)

data class DriveSession(
    val startedAtMillis: Long,
    val drivingMillis: Long,
    val distanceKm: Double,
    val rests: List<RestRecord>,
    val visited: List<String>,
)
