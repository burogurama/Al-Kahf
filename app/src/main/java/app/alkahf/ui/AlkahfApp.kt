package app.alkahf.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.res.Resources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.AyahRange
import app.alkahf.data.HomeData
import app.alkahf.data.LoopPreset
import app.alkahf.data.MemorizationState
import app.alkahf.data.ReciterStatus
import app.alkahf.data.TawqitTrack
import app.alkahf.ui.components.AlkahfTab
import app.alkahf.ui.home.AyahMemorizationState
import app.alkahf.ui.home.DayActivity
import app.alkahf.ui.home.HomeScreen
import app.alkahf.ui.home.HomeUiState
import app.alkahf.ui.home.WeekDay
import app.alkahf.ui.library.LibraryScreen
import app.alkahf.ui.library.ReciterDownloadsScreen
import app.alkahf.ui.loop.LoopPlayerScreen
import app.alkahf.ui.mushaf.MushafScreen
import app.alkahf.ui.progress.ProgressScreen
import app.alkahf.ui.review.ReviewScreen
import app.alkahf.ui.tawqit.TawqitTaggingScreen
import kotlinx.coroutines.launch

/**
 * A destination on the navigation back stack. Each entry carries the arguments
 * the screen needs, so popping back restores the previous view exactly.
 */
private sealed interface Screen {
    object Home : Screen
    data class Mushaf(
        val startSurah: Int?,
        val startPage: Int?,
        val highlight: AyahRange?,
        val wird: app.alkahf.data.WirdMarks? = null,
    ) : Screen
    data class Loop(val presetId: Long?, val newPreset: Boolean) : Screen
    object Review : Screen
    object Progress : Screen
    object Library : Screen
    data class ReciterDownloads(val reciter: ReciterStatus) : Screen
    data class TawqitTagging(val draft: TawqitTrack) : Screen
    object Settings : Screen
    object KhatamProgram : Screen
    object KhatamTracker : Screen
    object KhatamPortion : Screen
    object ExercisesSetup : Screen
    object ExercisesRun : Screen
    object ExercisesResult : Screen
}

private fun buildHomeUiState(
    res: Resources,
    data: HomeData,
    surahName: (Int) -> String,
    khatam: app.alkahf.data.KhatamState?,
    exerciseReadiness: app.alkahf.data.ExerciseReadiness,
    lastExercise: app.alkahf.data.ExerciseResult?,
): HomeUiState {
    val names = data.review.names
    val sabaq = data.sabaq
    val drill = data.drill
    return HomeUiState(
        streakDays = data.streakDays,
        hasSabaq = sabaq != null,
        sabaqReference = sabaq?.let {
            res.getString(R.string.home_sabaq_reference, surahName(it.surah), it.ayahFrom, it.ayahTo)
        } ?: "",
        sabaqAyahText = sabaq?.firstAyahText ?: "",
        sabaqAyahMarker = sabaq?.firstAyahMarker ?: "",
        sabaqAyahStates = sabaq?.states?.map { it.toHomeState() } ?: emptyList(),
        reviewPortionCount = data.review.count,
        reviewEstimatedMinutes = data.review.minutes,
        reviewPortionNames = names.take(4),
        reviewOverflowCount = (names.size - 4).coerceAtLeast(0),
        hasDrill = drill != null,
        drillPresetId = drill?.id ?: 0L,
        drillPresetTitle = drill?.let {
            res.getString(
                R.string.home_drill_title,
                it.reciterName,
                surahName(it.surah),
                it.ayahFrom,
                it.ayahTo,
            )
        } ?: "",
        drillPresetDetail = drill?.let {
            res.getString(R.string.home_drill_detail, it.perAyah, it.perChain)
        } ?: "",
        weekSummary = res.getString(
            R.string.home_week_summary,
            data.week.daysPracticed,
            data.week.ayatThisWeek,
        ),
        weekDays = data.week.dayLetters.mapIndexed { index, letter ->
            val isToday = index == data.week.dayLetters.lastIndex
            WeekDay(
                letter = letter,
                activity = when {
                    isToday -> DayActivity.TODAY
                    data.week.practiced.getOrElse(index) { false } -> DayActivity.PRACTICED
                    else -> DayActivity.MISSED
                },
            )
        },
        hasKhatam = khatam != null,
        khatamPercent = khatam?.percent ?: 0,
        khatamRingFraction = khatam?.ringFraction ?: 0f,
        khatamTodayJuz = khatam?.todaysPortionJuz ?: 0,
        khatamTodayReference = khatam?.todaysPortion?.let {
            res.getString(
                R.string.khatam_portion_range,
                surahName(it.surahFrom), it.ayahFrom,
                surahName(it.surahTo), it.ayahTo,
            )
        } ?: "",
        exerciseReadinessLine = if (exerciseReadiness.ayahCount > 0) {
            res.getString(
                R.string.ex_today_readiness,
                res.getQuantityString(
                    R.plurals.ex_surah_count,
                    exerciseReadiness.surahCount,
                    exerciseReadiness.surahCount,
                ),
                res.getQuantityString(
                    R.plurals.ex_ayah_count,
                    exerciseReadiness.ayahCount,
                    exerciseReadiness.ayahCount,
                ),
            )
        } else {
            res.getString(R.string.ex_today_readiness_empty)
        },
        exerciseReady = exerciseReadiness.ayahCount > 0,
        exerciseHasLastResult = lastExercise != null,
        exerciseLastScore = lastExercise?.let { "${it.correct}/${it.total}" } ?: "",
        exerciseLastWhen = lastExercise?.let { relativeDayLabel(res, it.epochDay) } ?: "",
        exerciseLastToRevisit = lastExercise?.toRevisit ?: 0,
    )
}

/** A relative-day label ("today" / "yesterday" / "N days ago") for the Today card. */
private fun relativeDayLabel(res: Resources, epochDay: Long): String {
    val days = (java.time.LocalDate.now().toEpochDay() - epochDay).toInt().coerceAtLeast(0)
    return when (days) {
        0 -> res.getString(R.string.ex_today_when_today)
        1 -> res.getString(R.string.ex_today_when_yesterday)
        else -> res.getString(R.string.ex_today_when_days_ago, days)
    }
}

private fun MemorizationState.toHomeState(): AyahMemorizationState = when (this) {
    MemorizationState.NOT_STARTED -> AyahMemorizationState.NOT_STARTED
    MemorizationState.LEARNING -> AyahMemorizationState.LEARNING
    MemorizationState.MEMORIZED -> AyahMemorizationState.MEMORIZED
    MemorizationState.STRONG -> AyahMemorizationState.STRONG
}

@Composable
fun AlkahfApp(
    playDrillSignal: Int = 0,
    openKhatamSignal: Int = 0,
    onThemeChange: (app.alkahf.ui.theme.ThemeChoice) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
) {
    // The navigation back stack; the last entry is the visible screen. Each entry
    // carries its own arguments so popping back restores the previous view.
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = backStack.last()
    val context = LocalContext.current
    val repository = (context.applicationContext as AlkahfApplication).repository
    val scope = rememberCoroutineScope()

    fun navigate(screen: Screen) {
        if (backStack.last() != screen) backStack.add(screen)
    }
    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
    // Opens the muṣḥaf for a khatam reading session: resumes at the last-read page
    // (or today's portion's start) and carries the wird's start/end āyāt so the
    // muṣḥaf can mark where today's portion begins and ends.
    fun startKhatamReading(state: app.alkahf.data.KhatamState) {
        val portion = state.todaysPortion
        navigate(
            Screen.Mushaf(
                startSurah = null,
                startPage = state.resumePage,
                highlight = null,
                wird = portion?.let {
                    app.alkahf.data.WirdMarks(
                        startAyahId = it.surahFrom * 1000 + it.ayahFrom,
                        endAyahId = it.surahTo * 1000 + it.ayahTo,
                    )
                },
            ),
        )
    }
    // Bottom-nav tab: jump back to the tab if it's already in the stack, else open
    // it — so tab switching never stacks duplicates or strands a forward history.
    fun selectTab(screen: Screen) {
        val index = backStack.indexOfLast { it == screen }
        if (index >= 0) {
            while (backStack.size > index + 1) backStack.removeAt(backStack.lastIndex)
        } else {
            backStack.add(screen)
        }
    }
    fun onTab(tab: AlkahfTab) = selectTab(
        when (tab) {
            AlkahfTab.TODAY -> Screen.Home
            AlkahfTab.MUSHAF -> Screen.Mushaf(null, null, null)
            AlkahfTab.REVIEW -> Screen.Review
            AlkahfTab.PROGRESS -> Screen.Progress
            AlkahfTab.LIBRARY -> Screen.Library
        },
    )

    val resources = LocalContext.current.resources
    val surahName = rememberSurahNamer()
    val khatamController = remember { app.alkahf.ui.khatam.KhatamController(repository) }
    val khatamState by khatamController.state.collectAsState()
    // False until the controller's first refresh completes; distinguishes "no
    // active khatam" from "still loading" so the tracker doesn't bounce a valid
    // khatam back to Today before its state has loaded.
    val khatamLoaded by khatamController.loaded.collectAsState()
    val exercisesController = remember { app.alkahf.ui.exercises.ExercisesController(repository) }
    // The "log it complete" sheet shown over the tracker; not a back-stack entry.
    var showKhatamLog by remember { mutableStateOf(false) }
    // Bumped to reload the Home dashboard after an in-place change (e.g. marking
    // the sabaq memorized) that doesn't change the visible screen.
    var homeRefresh by remember { mutableStateOf(0) }
    val homeState by produceState(initialValue = HomeUiState(), current, homeRefresh, surahName) {
        if (current is Screen.Home) {
            value = buildHomeUiState(
                resources,
                repository.homeData(),
                surahName,
                repository.activeKhatam(),
                repository.exerciseReadiness(),
                repository.lastExerciseResult(),
            )
        }
    }
    // Keep the khatam controller's state fresh whenever a khatam screen is shown.
    LaunchedEffect(current) {
        if (current is Screen.KhatamTracker ||
            current is Screen.KhatamPortion ||
            current is Screen.KhatamProgram
        ) {
            khatamController.refresh()
        }
    }

    // A reminder's "Listen" action drops the user straight into the sabaq drill.
    // Opening the Loop player applies the preset, which starts playback (so the
    // drill is already reciting); presetId null → the default sabaq drill.
    LaunchedEffect(playDrillSignal) {
        if (playDrillSignal > 0) navigate(Screen.Loop(presetId = null, newPreset = false))
    }

    // A khatam reminder drops the user into the tracker (which pops itself back to
    // Today if the khatam was cancelled / no longer exists).
    LaunchedEffect(openKhatamSignal) {
        if (openKhatamSignal > 0) navigate(Screen.KhatamTracker)
    }

    // System back pops the stack; at the root, let the OS handle it (exit).
    BackHandler(enabled = backStack.size > 1) { back() }

    when (val screen = current) {
        Screen.Home -> HomeScreen(
            state = homeState,
            onOpenMushaf = { navigate(Screen.Mushaf(null, null, null)) },
            onOpenSabaq = { navigate(Screen.Mushaf(null, null, highlight = repository.sabaqRange)) },
            // (sabaqRange is null when there's no sabaq → opens the Mushaf normally)
            onMarkSabaq = {
                scope.launch {
                    // Only advances when every āyah is memorized/strong; the button
                    // is shown only then, and the repository rejects it otherwise.
                    repository.markSabaqDone()
                    homeRefresh++
                }
            },
            onOpenLoop = {
                navigate(Screen.Loop(presetId = homeState.drillPresetId.takeIf { it != 0L }, newPreset = false))
            },
            onOpenReview = { navigate(Screen.Review) },
            onOpenProgress = { navigate(Screen.Progress) },
            onOpenLibrary = { navigate(Screen.Library) },
            onOpenSettings = { navigate(Screen.Settings) },
            onBeginKhatam = { navigate(Screen.KhatamProgram) },
            onOpenKhatam = { navigate(Screen.KhatamTracker) },
            onStartExercises = { navigate(Screen.ExercisesSetup) },
        )
        is Screen.Mushaf -> MushafScreen(
            startSurah = screen.startSurah,
            startPage = screen.startPage,
            highlightRange = screen.highlight,
            wird = screen.wird,
            onPageRead = { page ->
                // Persist the read position only for a khatam reading session.
                if (screen.wird != null) scope.launch { repository.setKhatamReadPage(page) }
            },
            onBack = { back() },
            onImportReciter = { reciter -> navigate(Screen.ReciterDownloads(reciter)) },
        )
        is Screen.Loop -> LoopPlayerScreen(
            presetId = screen.presetId,
            newPreset = screen.newPreset,
            onBack = { back() },
        )
        Screen.Review -> ReviewScreen(
            onBack = { back() },
            onOpenExercises = { navigate(Screen.ExercisesSetup) },
        )
        Screen.Progress -> ProgressScreen(
            onOpenPage = { page -> navigate(Screen.Mushaf(null, page, null)) },
            onSelectTab = ::onTab,
        )
        Screen.Library -> LibraryScreen(
            onOpenPreset = { id -> navigate(Screen.Loop(presetId = id, newPreset = false)) },
            onNewPreset = { navigate(Screen.Loop(presetId = null, newPreset = true)) },
            onManageReciter = { reciter -> navigate(Screen.ReciterDownloads(reciter)) },
            onSelectTab = ::onTab,
        )
        Screen.Settings -> app.alkahf.ui.settings.SettingsScreen(
            onBack = { back() },
            onThemeChange = onThemeChange,
            onLanguageChange = onLanguageChange,
        )
        is Screen.TawqitTagging -> TawqitTaggingScreen(
            draft = screen.draft,
            onSaved = { back() },
            onClose = { back() },
        )
        is Screen.ReciterDownloads -> ReciterDownloadsScreen(
            reciterKey = screen.reciter.key,
            reciterName = screen.reciter.displayName,
            isImported = screen.reciter.isImported,
            onTimeSurah = { surah ->
                scope.launch {
                    val draft = repository.tawqitDraftForImport(screen.reciter.key, surah)
                    if (draft != null) navigate(Screen.TawqitTagging(draft))
                }
            },
            onBack = { back() },
        )
        Screen.KhatamProgram -> app.alkahf.ui.khatam.KhatamProgramScreen(
            onClose = { back() },
            onBegin = { pace, reminderEnabled, reminderMinute ->
                scope.launch {
                    khatamController.program(pace, reminderEnabled, reminderMinute)
                    // Arm/clear the khatam reminder alarm for the new config.
                    app.alkahf.notify.ReminderScheduler.reschedule(context)
                    // Replace the program step with the tracker so back goes Home.
                    back()
                    navigate(Screen.KhatamTracker)
                }
            },
        )
        Screen.KhatamTracker -> {
            val state = khatamState
            if (state == null) {
                // Only fall back once the first load has finished; until then the
                // refresh triggered on entry is still resolving (a null here is
                // "loading", not "no khatam"). Bouncing early closed the tracker
                // immediately after opening it.
                if (khatamLoaded) LaunchedEffect(Unit) { back() }
            } else {
                app.alkahf.ui.khatam.KhatamTrackerScreen(
                    state = state,
                    onBack = { back() },
                    onOpenPortion = { navigate(Screen.KhatamPortion) },
                    onStartReading = { startKhatamReading(state) },
                    onReminderChange = { enabled, minute ->
                        scope.launch {
                            khatamController.setReminder(enabled, minute)
                            app.alkahf.notify.ReminderScheduler.reschedule(context)
                        }
                    },
                    onCancel = {
                        scope.launch {
                            khatamController.cancel()
                            // Drop the khatam reminder alarm now that it's gone.
                            app.alkahf.notify.ReminderScheduler.reschedule(context)
                            back()
                        }
                    },
                )
                if (showKhatamLog) {
                    app.alkahf.ui.khatam.KhatamLogSheet(
                        state = state,
                        onDone = {
                            scope.launch {
                                khatamController.logTodayPortion()
                                homeRefresh++
                                showKhatamLog = false
                            }
                        },
                        onReadAhead = {
                            showKhatamLog = false
                            state.todaysPortion?.let { navigate(Screen.Mushaf(null, it.pageTo + 1, null)) }
                        },
                        onDismiss = { showKhatamLog = false },
                    )
                }
            }
        }
        Screen.KhatamPortion -> {
            val state = khatamState
            if (state == null) {
                // Wait for the first load before bailing (see KhatamTracker).
                if (khatamLoaded) LaunchedEffect(Unit) { back() }
            } else {
                app.alkahf.ui.khatam.KhatamPortionScreen(
                    state = state,
                    onBack = { back() },
                    onStartReading = { startKhatamReading(state) },
                    onMarkComplete = {
                        // The log sheet lives on the tracker; pop to it and show it.
                        back()
                        showKhatamLog = true
                    },
                )
            }
        }
        Screen.ExercisesSetup -> app.alkahf.ui.exercises.ExercisesSetupScreen(
            controller = exercisesController,
            onClose = { back() },
            onGenerate = { scopeArg, types, length ->
                scope.launch {
                    exercisesController.generate(scopeArg, types, length)
                    navigate(Screen.ExercisesRun)
                }
            },
        )
        Screen.ExercisesRun -> app.alkahf.ui.exercises.ExercisesRunnerScreen(
            controller = exercisesController,
            onClose = { back() },
            // The runner advances to the result. Pop the runner; if a result is
            // already on the stack (the user came from "Review the n"), return to
            // it, otherwise push a fresh one. Either way system back from the
            // result skips the answered runner.
            onFinished = {
                back()
                if (backStack.last() != Screen.ExercisesResult) navigate(Screen.ExercisesResult)
            },
        )
        Screen.ExercisesResult -> app.alkahf.ui.exercises.ExercisesResultScreen(
            controller = exercisesController,
            // Closing the result pops the whole pushed flow (Result + Setup) back
            // to wherever it was opened (Today / Review), refreshing Today.
            onClose = {
                back()
                back()
                homeRefresh++
            },
            // "Review the n" re-opens the session at the missed question.
            onReviewMissed = { index ->
                exercisesController.goTo(index)
                navigate(Screen.ExercisesRun)
            },
            // "New test" pops back to a fresh Setup and refreshes Today.
            onNewTest = {
                back()
                homeRefresh++
            },
        )
    }
}
