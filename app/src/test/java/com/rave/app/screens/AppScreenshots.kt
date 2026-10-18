package com.rave.app.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.work.testing.WorkManagerTestInitHelper
import com.rave.app.MainViewModel
import com.rave.app.data.food.FoodRepository
import com.rave.app.data.local.AppDatabase
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.LocationPointEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.data.repository.WaterRepository
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.MealSlot
import com.rave.app.domain.model.ActivityStatus
import com.rave.app.domain.model.AppStyle
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitType
import com.rave.app.ui.RaveApp
import com.rave.app.ui.onboarding.OnboardingScreen
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.rave.app.ui.navigation.ActivitySummaryRoute
import com.rave.app.ui.navigation.AboutRoute
import com.rave.app.ui.navigation.AddEditHabitRoute
import com.rave.app.ui.navigation.LegalRoute
import com.rave.app.ui.navigation.CompetitionsRoute
import com.rave.app.ui.navigation.FoodResultRoute
import com.rave.app.ui.navigation.FoodRoute
import com.rave.app.ui.navigation.FriendsRoute
import com.rave.app.ui.navigation.LiveTrackingRoute
import com.rave.app.ui.navigation.MealPlanRoute
import com.rave.app.ui.navigation.PaywallRoute
import com.rave.app.ui.navigation.ProfileRoute
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rave.app.domain.workout.MuscleGroup
import com.rave.app.ui.workout.guide.EXERCISE_GUIDES
import com.rave.app.ui.workout.guide.ExerciseGuideContent
import com.rave.app.ui.workout.guide.ExerciseThumb
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the real app screens (real ViewModels, Room, DataStore) with some sample data and
 * saves PNGs to app/build/screenshots. Run: ./gradlew :app:testDebugUnitTest --tests '*AppScreenshots*'
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class AppScreenshots {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ScreenshotHostActivity>()

    @Inject lateinit var db: AppDatabase
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var habits: HabitRepository
    @Inject lateinit var water: WaterRepository
    @Inject lateinit var food: FoodRepository

    private val outDir = File("build/screenshots").apply { mkdirs() }
    private var activityId = 0L

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(compose.activity)
        hilt.inject()
        runBlocking { seed() }
    }

    private suspend fun seed() {
        settings.update { it.copy(displayName = "Rayan", onboardingDone = true, dailyStepGoal = 8000) }
        val today = LocalDate.now()
        val start = today.minusDays(14)
        val made = listOf(
            Habit(name = "Drink water", icon = "water", color = HabitColor.BLUE, type = HabitType.COUNTER, target = 8, unit = "glasses", createdDate = start),
            Habit(name = "Gym", icon = "workout", color = HabitColor.PEACH, type = HabitType.TIMED, target = 45, createdDate = start),
            Habit(name = "Meditate", icon = "meditate", color = HabitColor.PINK, type = HabitType.TIMED, target = 10,
                reminderTimes = listOf(LocalTime.of(19, 30)), remindersEnabled = true, createdDate = start),
            Habit(name = "Read", icon = "read", color = HabitColor.GREEN, type = HabitType.CHECKBOX, createdDate = start),
        ).map { it.copy(id = habits.save(it)) }
        for (d in 1L..12L) made.forEach { h -> habits.complete(h, today.minusDays(d)) }
        habits.complete(made[0], today)
        habits.complete(made[1], today)
        water.add(5)

        // A few past walks plus this evening's walk with a route.
        val zone = ZoneId.systemDefault()
        val dao = db.activityDao()
        for (d in 1L..6L) {
            val t = today.minusDays(d).atTime(18, 30).atZone(zone).toInstant().toEpochMilli()
            dao.insert(ActivityEntity(type = ActivityType.WALK, status = ActivityStatus.COMPLETED, startTime = t, endTime = t + 35 * 60_000,
                distanceM = 3200.0 + d * 150, durationS = 35 * 60L, movingTimeS = 34 * 60L, avgPaceSPerKm = 590.0, calories = 190))
        }
        val t0 = today.atTime(18, 42).atZone(zone).toInstant().toEpochMilli()
        activityId = dao.insert(ActivityEntity(type = ActivityType.WALK, status = ActivityStatus.COMPLETED, startTime = t0,
            endTime = t0 + 2320_000, distanceM = 4210.0, durationS = 2320, movingTimeS = 2290, avgPaceSPerKm = 551.0,
            calories = 236, elevationGainM = 18.0, name = "Evening walk", isPB = true, pbTags = "FASTEST_1K"))
        val pts = db.locationPointDao()
        for (i in 0..160) {
            val a = i / 160.0
            pts.insert(LocationPointEntity(activityId = activityId,
                lat = 31.4840 + a * 0.020 + sin(a * 9) * 0.0016, lng = 74.3220 + a * 0.024 + cos(a * 7) * 0.0020,
                accuracy = 5f, speed = 1.9f, timestamp = t0 + (a * 2320_000).toLong()))
        }

        food.log("Anda paratha + chai", 520, 18.0, 52.0, 26.0, 9.0, FoodSource.MANUAL, MealSlot.BREAKFAST)
        food.log("Daal chawal", 690, 24.0, 110.0, 14.0, 4.0, FoodSource.MANUAL, MealSlot.LUNCH)
        food.log("Greek yogurt", 130, 11.0, 9.0, 4.0, 6.0, FoodSource.MANUAL, MealSlot.SNACK)
    }

    private fun shoot(name: String, route: Any?, settleMs: Long = 2500, style: AppStyle = AppStyle.CLASSIC) {
        // Some screens run endless animations (pulses, the buddy), so the compose clock is driven
        // by hand: with auto-advance off, waiting for idle doesn't wait for those to finish.
        compose.mainClock.autoAdvance = false
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            val vm: MainViewModel = hiltViewModel()
            RaveTheme(darkTheme = false, style = style) { RaveApp(vm, nav) }
        }
        settle(500)
        if (route != null) compose.runOnUiThread { nav.navigate(route) }
        settle(settleMs)
        Thread.sleep(1500) // let IO (Room / network) finish
        settle(1500)
        // Draw the window directly: captureToImage() waits for an idle UI, which screens with
        // endless animations never reach.
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun settle(ms: Long) {
        var left = ms
        while (left > 0) {
            compose.mainClock.advanceTimeBy(100)
            shadowOf(Looper.getMainLooper()).idle()
            left -= 100
        }
    }

    @Test fun s01_home() = shoot("01_home", null)
    @Test fun s02_scan() = shoot("02_scan", FoodRoute)
    @Test fun s03_result() = shoot("03_scan_result", FoodResultRoute("5449000000996"), settleMs = 6000)
    @Test fun s04_tracking() = shoot("04_live_walk", LiveTrackingRoute(ActivityType.WALK.name))
    @Test fun s05_summary() = shoot("05_walk_summary", ActivitySummaryRoute(activityId))
    @Test fun s06_squad() = shoot("06_squad", FriendsRoute)
    @Test fun s07_competitions() = shoot("07_competitions", CompetitionsRoute)
    @Test fun s08_profile() = shoot("08_profile", ProfileRoute)
    @Test fun s09_habit() = shoot("09_new_habit", AddEditHabitRoute())
    @Test fun s10_mealplan() = shoot("10_meal_plan", MealPlanRoute)
    @Test fun s11_paywall() = shoot("11_paywall", PaywallRoute())
    @Test fun s12_home_bento() = shoot("12_home_bento", null, style = AppStyle.BENTO)
    @Test fun s13_scan_bento() = shoot("13_scan_bento", FoodRoute, style = AppStyle.BENTO)

    /** Onboarding: render the real screen and swipe through its pages. */
    @Test fun s14_onboarding() {
        compose.mainClock.autoAdvance = false
        compose.setContent { RaveTheme(darkTheme = false) { OnboardingScreen(onFinished = {}) } }
        settle(3000)
        listOf("14_onboarding_1", "15_onboarding_2", "16_onboarding_3", "17_onboarding_4", "18_onboarding_5", "19_onboarding_setup", "20_onboarding_signin")
            .forEachIndexed { i, name ->
                if (i > 0) {
                    compose.onRoot().performTouchInput { swipeLeft() }
                    settle(3500)
                }
                save(name)
            }
    }

    @Test fun s23_about() = shoot("23_about", AboutRoute)
    @Test fun s24_privacy() = shoot("24_privacy_policy", LegalRoute("PRIVACY"))
    @Test fun s25_credits() = shoot("25_credits", LegalRoute("CREDITS"))

    /** Every built-in exercise figure, plus one full guide. */
    @Test fun s21_exercise_figures() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RaveTheme(darkTheme = false) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(8.dp)) {
                    EXERCISE_GUIDES.chunked(4).forEach { row ->
                        Row {
                            row.forEach { g ->
                                Column(Modifier.weight(1f).padding(3.dp)) {
                                    ExerciseThumb(g, MuscleGroup.CHEST, Modifier.fillMaxWidth().aspectRatio(1.07f))
                                    Text(stringResource(g.name), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        settle(500)
        save("21_exercise_figures")
    }

    @Test fun s22_exercise_guide() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RaveTheme(darkTheme = false) {
                Surface(Modifier.fillMaxSize()) {
                    ExerciseGuideContent(EXERCISE_GUIDES.first { it.key == "squat" }, MuscleGroup.LEGS, Modifier.padding(top = 40.dp))
                }
            }
        }
        settle(1200)
        save("22_exercise_guide")
    }

    private fun save(name: String) {
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
