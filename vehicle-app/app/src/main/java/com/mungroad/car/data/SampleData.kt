package com.mungroad.car.data

/**
 * 시연용 샘플 데이터. 장소명·거리·운영 시간은 예시이며 화면에 '샘플 데이터'로 표시합니다.
 * 서비스에서는 한국관광공사 반려동물 동반여행 OpenAPI·한국도로공사 휴게시설 정보를 서버(MUNGROAD_API_BASE_URL)를 통해 받습니다.
 */
object SampleData {
    val saseumi = Pet(
        id = "pet-1", name = "사슴이", breed = "믹스", ageYears = 9.0, weightKg = 12.0,
        activity = "활발함", caution = "차멀미가 있어요 · 물은 조금씩",
    )

    private const val TOUR = "한국관광공사 반려동물 동반여행"
    private const val EX = "한국도로공사 휴게시설 정보"

    val cafe = Place(
        "p-cafe", "바다숲 카페", Category.CAFE, 37.7790, 128.9080, "강원 강릉시 창해로", Verification.VERIFIED,
        conditions = listOf("중형견 가능", "실내 동반 가능", "목줄 필수"), restrictions = listOf("대형견 불가"),
        source = TOUR, verifiedAt = "2026.11.02", hours = "10:00 – 21:00", closedDays = "매주 화요일", phone = "033-000-0000",
        userChecks = 12, userCheckedAt = "11.08", image = "cafe",
    )
    val dogPark = Place(
        "p-dogpark", "해피독 파크", Category.PARK, 37.7700, 128.8900, "강원 강릉시 난설헌로", Verification.CHECK_NEEDED,
        phone = "033-000-0001", userChecks = 3, userCheckedAt = "10.30", image = "dogpark",
    )
    val pinePark = Place(
        "p-pine", "솔밭 공원", Category.PARK, 37.7850, 128.9000, "강원 강릉시 송정동", Verification.VERIFIED,
        conditions = listOf("대형견 가능", "야외 공간", "목줄 필수"), source = TOUR, verifiedAt = "2026.10.21", hours = "상시 개방", image = "park",
    )
    val bakery = Place(
        "p-bakery", "파도 베이커리", Category.CAFE, 37.7910, 128.9150, "강원 강릉시 해안로", Verification.CHECK_NEEDED,
        phone = "033-000-0002", image = "cafe",
    )
    val restaurant = Place(
        "p-food", "바다 식당", Category.RESTAURANT, 37.7960, 128.9180, "강원 강릉시 경포로", Verification.VERIFIED,
        conditions = listOf("중형견 가능", "야외 테라스"), source = TOUR, verifiedAt = "2026.10.30", hours = "11:00 – 20:30", image = "cafe",
    )
    val stay = Place(
        "p-stay", "솔향 펜션", Category.STAY, 37.8000, 128.9000, "강원 강릉시 사천면", Verification.VERIFIED,
        conditions = listOf("소·중형견 가능", "객실 동반"), source = TOUR, verifiedAt = "2026.09.14", image = "park",
    )
    val beachWalk = Place(
        "p-beach", "해변 산책로", Category.PARK, 37.8050, 128.9080, "강원 강릉시 경포해변", Verification.VERIFIED,
        conditions = listOf("야외", "목줄 필수"), source = TOUR, verifiedAt = "2026.10.12", image = "sea",
    )
    val gyeongpo = Place(
        "p-dest", "강릉 경포해변", Category.PARK, 37.8056, 128.9086, "강원 강릉시 경포해변", Verification.VERIFIED,
        conditions = listOf("야외", "목줄 필수"), source = TOUR, verifiedAt = "2026.10.12", image = "sea",
    )

    // 휴게소(경로 앞 휴식 후보)
    val okgye = Place(
        "r-okgye", "옥계휴게소", Category.REST_AREA, 37.6200, 129.0400, "동해고속도로", Verification.VERIFIED,
        conditions = listOf("반려견 놀이터"), source = EX, verifiedAt = "2025.06", hasDogPlayground = true, hasFuel = true,
        userChecks = 21, userCheckedAt = "11.10", image = "dogpark",
    )
    val gangneungRest = Place(
        "r-gangneung", "강릉휴게소", Category.REST_AREA, 37.7230, 128.8380, "영동고속도로", Verification.VERIFIED,
        conditions = listOf("반려견 놀이터"), source = EX, verifiedAt = "2025.06", hasDogPlayground = true, hasFuel = true, hasEvCharger = true,
        userChecks = 9, userCheckedAt = "11.02", image = "dogpark",
    )
    val pyeongchang = Place(
        "r-pyeongchang", "평창휴게소", Category.REST_AREA, 37.5600, 128.4300, "영동고속도로", Verification.VERIFIED,
        conditions = listOf("잔디 산책로"), source = EX, verifiedAt = "2025.06", hasEvCharger = true, image = "park",
    )
    val shade = Place(
        "r-shade", "솔향 쉼터", Category.REST_AREA, 37.4800, 128.2000, "국도 6호선", Verification.CHECK_NEEDED, image = "sea",
    )
    val restAreas = listOf(pyeongchang, gangneungRest, okgye, shade)

    val nearby = listOf(cafe, dogPark, pinePark, bakery, restaurant, stay)

    val gangneungCourse = Course(
        "c-gangneung", "강릉 바다 코스", "강릉", "바다", 128, 130,
        listOf(CourseStop(beachWalk), CourseStop(okgye, isRest = true), CourseStop(bakery), CourseStop(restaurant)), "sea",
    )
    val yangpyeongCourse = Course(
        "c-yangpyeong", "양평 힐링 코스", "양평", "자연", 87, 90,
        listOf(CourseStop(pinePark), CourseStop(cafe)), "park",
    )
    val feedCourse = Course(
        "c-feed", "경포 산책 반나절 코스", "강릉", "바다", 96, 100,
        listOf(CourseStop(beachWalk), CourseStop(cafe)), "sea", fromFeed = true,
    )
    val courses = listOf(gangneungCourse, yangpyeongCourse)

    val trips = listOf(
        Trip("t-1", "사슴이와 강릉 바다", "11월 15일 (토)", "집", gyeongpo, listOf(okgye, bakery), listOf(saseumi.id), startDate = "2026-11-15"),
        Trip("t-2", "양평 숲 나들이", "11월 22일 (토)", "집", pinePark, emptyList(), listOf(saseumi.id)),
    )
}
