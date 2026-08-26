package app.f3d.F3D.android

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.f3d.F3D.Image
import app.f3d.F3D.android.Utils.ConsoleLog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity?> =
            ActivityScenarioRule(MainActivity::class.java)

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    /** Copy a test asset to the app's internal storage and return the file. */
    private fun String.copyTestAsset(destName: String): File {
        val file = File(context.filesDir, destName)
        instrumentation.context.assets.open(this).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    /** Extract the MainView from an activity's layout. */
    private fun getMainView(activity: MainActivity): MainView {
        val mainLayout = activity.findViewById<ViewGroup>(R.id.mainLayout)
        return (0 until mainLayout.childCount)
                .map { mainLayout.getChildAt(it) }
                .filterIsInstance<MainView>()
                .first()
    }

    /** Render to an image on the GL thread and return it. */
    private fun captureImage(mainView: MainView): Image {
        var image: Image? = null
        val latch = CountDownLatch(1)

        mainView.queueEvent {
            image = mainView.renderToImage()
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)

        assertTrue("Rendered image is null", image != null)
        assertTrue("Rendered image has invalid width", image!!.width > 0)
        assertTrue("Rendered image has invalid height", image.height > 0)

        return image
    }

    /** Compare a rendered image against a baseline, saving on mismatch or missing baseline. */
    private fun assertMatchesBaseline(image: Image, baselinePrefix: String) {
        val resolution = "${image.width}x${image.height}"
        val baselineName = "${baselinePrefix}_$resolution.png"
        val baselineFile = File(context.filesDir, "baseline.png")

        try {
            instrumentation.context.assets.open("baselines/$baselineName").use { input ->
                baselineFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (_: java.io.FileNotFoundException) {
            val outputFile = File(context.getExternalFilesDir(null), baselineName)
            image.save(outputFile.absolutePath)
            println("No baseline found for $resolution. Saved rendered image to: ${outputFile.absolutePath}")
            assertTrue("No baseline found for resolution $resolution. Rendered image saved as ${outputFile.absolutePath}", false)
        }

        val baselineImage = Image(baselineFile.absolutePath)
        try {
            val difference = image.compare(baselineImage)
            println("Image difference: $difference")

            if (difference > 0.04) {
                val outputFile = File(context.getExternalFilesDir(null), baselineName)
                image.save(outputFile.absolutePath)
                println("Saved rendered image to: ${outputFile.absolutePath}")
                assertTrue("Rendered image differs from the baseline (diff=$difference, resolution=$resolution)", false)
            }
        } finally {
            baselineImage.delete()
        }
    }

    // Simulate a click on the '+' button and cancel, to make sure we do not crash
    @Test
    fun testOpenAndCancel() {
        // Set up a monitor to intercept the file picker intent and return CANCELED
        val filter = IntentFilter(Intent.ACTION_GET_CONTENT)
        filter.addDataType("*/*")
        val result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
        val monitor = instrumentation.addMonitor(filter, result, true)

        // Click the + button
        activityRule.scenario.onActivity { activity ->
            activity!!.findViewById<FloatingActionButton>(R.id.addButton).performClick()
        }

        // Give the click time to propagate and fire the intent
        instrumentation.waitForIdleSync()
        assertTrue("File picker intent was not fired", monitor.hits > 0)

        instrumentation.removeMonitor(monitor)
    }

    // Simulate a click on the '+' button, select a file, and verify it loads and renders
    @Test
    fun testOpenFile() {
        val testFile = "data/f3d.glb".copyTestAsset("f3d.glb")
        val uri = Uri.fromFile(testFile)

        // Set up a monitor to intercept the file picker and return the file URI
        val filter = IntentFilter(Intent.ACTION_GET_CONTENT)
        filter.addDataType("*/*")
        val resultIntent = Intent().apply { data = uri }
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent)
        val monitor = instrumentation.addMonitor(filter, result, true)

        // Click the + button
        activityRule.scenario.onActivity { activity ->
            activity!!.findViewById<FloatingActionButton>(R.id.addButton).performClick()
        }

        instrumentation.waitForIdleSync()
        assertTrue("File picker intent was not fired", monitor.hits > 0)
        instrumentation.removeMonitor(monitor)

        Thread.sleep(3000)

        activityRule.scenario.onActivity { activity ->
            val image = captureImage(getMainView(activity!!))
            try {
                assertMatchesBaseline(image, "testOpen")
            } finally {
                image.delete()
            }
        }
    }

    // Simulate opening a file from a file explorer by launching the activity with an ACTION_VIEW intent
    @Test
    fun testOpenFromFileExplorer() {
        val testFile = "data/f3d.glb".copyTestAsset("f3d.glb")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(testFile), "*/*")
            setClassName(context, "app.f3d.F3D.android.MainActivity")
        }

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val image = captureImage(getMainView(activity!!))
            try {
                assertMatchesBaseline(image, "testOpen")
            } finally {
                image.delete()
            }
        }

        scenario.close()
    }

    // Simulate opening a file from a file explorer while the app is already running
    @Test
    fun testOpenFromFileExplorerWhileRunning() {
        val testFile = "data/f3d.glb".copyTestAsset("f3d.glb")

        instrumentation.waitForIdleSync()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(testFile), "*/*")
            setClassName(context, "app.f3d.F3D.android.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        Thread.sleep(3000)

        activityRule.scenario.onActivity { activity ->
            val image = captureImage(getMainView(activity!!))
            try {
                assertMatchesBaseline(image, "testOpen")
            } finally {
                image.delete()
            }
        }
    }

    // Open a file, rotate 90 degrees, switch apps, come back, and verify the view
    @Test
    fun testRotateAndResume() {
        val testFile = "data/f3d.glb".copyTestAsset("f3d.glb")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(testFile), "*/*")
            setClassName(context, "app.f3d.F3D.android.MainActivity")
        }

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        Thread.sleep(3000)

        // Perform a rotation on the GL thread
        scenario.onActivity { activity ->
            val mainView = getMainView(activity!!)
            val latch = CountDownLatch(1)
            mainView.queueEvent {
                mainView.rotateCamera(90.0, 0.0)
                latch.countDown()
            }
            latch.await(5, TimeUnit.SECONDS)
        }

        Thread.sleep(1000)

        // Simulate switching to another app
        scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(1000)

        // Come back to the app
        scenario.moveToState(Lifecycle.State.RESUMED)
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            val image = captureImage(getMainView(activity!!))
            try {
                assertMatchesBaseline(image, "testRotateAndResume")
            } finally {
                image.delete()
            }
        }

        scenario.close()
    }

    // Open a file, toggle an option through the options panel UI, and verify the render reflects it
    @Test
    fun testChangeOption() {
        val testFile = "data/f3d.glb".copyTestAsset("f3d.glb")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(testFile), "*/*")
            setClassName(context, "app.f3d.F3D.android.MainActivity")
        }

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        Thread.sleep(3000)

        // Open the options panel and toggle the grid off through the UI.
        // The panel content is built on the GL thread, so wait for it before matching a widget.
        onView(withId(R.id.optionsButton)).perform(click())
        Thread.sleep(1000)
        onView(withText("Grid")).perform(click())

        Thread.sleep(1000)

        scenario.onActivity { activity ->
            val image = captureImage(getMainView(activity!!))
            try {
                assertMatchesBaseline(image, "testChangeOption")
            } finally {
                image.delete()
            }
        }

        scenario.close()
    }

    // The console captures libf3d log output and runs commands against the engine. Asserted on
    // text rather than a rendered baseline: none of it reaches the 3D view.
    @Test
    fun testConsole() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        Thread.sleep(3000)

        onView(withId(R.id.consoleButton)).perform(click())
        Thread.sleep(1000)

        // Plugin loading happens before the console is ever opened, so its output is only there
        // if the log forwarder was installed early enough and the buffer survives independently.
        // Asserted on the buffer rather than on screen: the console shows its tail, and those
        // lines scrolled out long ago.
        assertTrue(
            "Plugin loading was not captured",
            ConsoleLog.snapshot().any { it.message.contains("Loading plugin") },
        )

        // A rejected command reports itself through the log, which is what surfaces it here.
        onView(withId(R.id.consoleInput))
            .inRoot(isDialog())
            .perform(typeText("not_a_command"), closeSoftKeyboard())
        onView(withId(R.id.consoleSendButton)).inRoot(isDialog()).perform(click())
        Thread.sleep(1000)
        onView(withSubstring("is not recognized"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        onView(withId(R.id.consoleClearButton)).inRoot(isDialog()).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.consoleEmpty)).inRoot(isDialog()).check(matches(isDisplayed()))

        scenario.close()
    }

    /** Launch the activity with the (animated) test model already loaded. */
    private fun launchWithTestFile(): ActivityScenario<MainActivity> {
        val testFile = "data/f3d.glb".copyTestAsset("f3d.glb")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(testFile), "*/*")
            setClassName(context, "app.f3d.F3D.android.MainActivity")
        }
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        Thread.sleep(3000)
        return scenario
    }

    // An animated file surfaces the playback controls. They auto-hide after a spell, so a viewport
    // touch is simulated to bring them back before asserting.
    @Test
    fun testAnimationControlsAppear() {
        val scenario = launchWithTestFile()

        scenario.onActivity { activity -> getMainView(activity!!).onViewportTouch?.invoke() }
        Thread.sleep(300)

        onView(withId(R.id.animSeekBar)).check(matches(isDisplayed()))
        onView(withId(R.id.animPlayButton)).check(matches(isDisplayed()))

        scenario.close()
    }

    // The play button toggles playback, reflected in its content description (Play <-> Pause).
    @Test
    fun testAnimationPlayPauseToggle() {
        val scenario = launchWithTestFile()

        val playDesc = context.getString(R.string.animation_play)
        val pauseDesc = context.getString(R.string.animation_pause)

        scenario.onActivity { activity ->
            val button = activity!!.findViewById<ImageButton>(R.id.animPlayButton)
            assertEquals(playDesc, button.contentDescription.toString())
            button.performClick()
        }

        // The toggle hops to the GL thread and posts the play-state change back, so give it a moment.
        Thread.sleep(500)

        scenario.onActivity { activity ->
            val button = activity!!.findViewById<ImageButton>(R.id.animPlayButton)
            assertEquals(pauseDesc, button.contentDescription.toString())
        }

        scenario.close()
    }

    // The lock button toggles the auto-hide behavior, reflected in its content description.
    @Test
    fun testLockTogglesAutoHide() {
        val scenario = launchWithTestFile()

        scenario.onActivity { activity ->
            val lock = activity!!.findViewById<ImageButton>(R.id.animLockButton)
            assertEquals(
                context.getString(R.string.animation_autohide),
                lock.contentDescription.toString()
            )
            lock.performClick()
            assertEquals(
                context.getString(R.string.animation_locked),
                lock.contentDescription.toString()
            )
        }

        scenario.close()
    }

    /** Open the scene panel and let its content be built off the rendering thread. */
    private fun openScenePanel() {
        onView(withId(R.id.sceneButton)).perform(click())
        Thread.sleep(500)
    }

    // ListAdapter diffs off the main thread, so the rows land after Espresso considers the app
    // idle. Waiting on the row count rather than on a delay keeps the assertions deterministic.
    private fun awaitSceneRows(scenario: ActivityScenario<MainActivity>) {
        val deadline = System.currentTimeMillis() + 5000
        var count = 0
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity { activity ->
                count = activity!!.findViewById<RecyclerView>(R.id.sceneTree).adapter?.itemCount ?: 0
            }
            if (count > 0) {
                return
            }
            Thread.sleep(100)
        }
        assertTrue("The scene tree stayed empty", false)
    }

    // The panel lists the hierarchy libf3d reports for the loaded file.
    @Test
    fun testScenePanelShowsHierarchy() {
        val scenario = launchWithTestFile()

        openScenePanel()
        awaitSceneRows(scenario)

        onView(withId(R.id.sceneTree)).check(matches(isDisplayed()))
        onView(withText("<stream>")).check(matches(isDisplayed()))
        onView(withText("<object>")).check(matches(isDisplayed()))
        onView(withText("<group>")).check(matches(isDisplayed()))

        scenario.close()
    }

    // The counters come straight from scene.getSceneInfo, pinning what libf3d reports for the
    // test model.
    @Test
    fun testScenePanelShowsCounters() {
        val scenario = launchWithTestFile()

        openScenePanel()
        awaitSceneRows(scenario)

        onView(withId(R.id.sceneInfo)).check(matches(isDisplayed()))
        onView(withId(R.id.sceneInfo)).check(matches(withSubstring("1 actor")))
        onView(withId(R.id.sceneInfo)).check(matches(withSubstring("24 points")))
        onView(withId(R.id.sceneInfo)).check(matches(withSubstring("8 cells")))

        scenario.close()
    }

    // The file name is not shown anywhere else in the app, so the panel title carries it.
    @Test
    fun testScenePanelTitleIsFileName() {
        val scenario = launchWithTestFile()

        openScenePanel()
        awaitSceneRows(scenario)

        onView(withId(R.id.sceneTitle)).check(matches(withText("f3d.glb")))

        scenario.close()
    }

    // With nothing loaded the counters make no sense and the title has no file to name.
    @Test
    fun testScenePanelEmptyState() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        Thread.sleep(3000)

        openScenePanel()

        onView(withId(R.id.sceneEmpty)).check(matches(isDisplayed()))
        onView(withId(R.id.sceneInfo)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.sceneTitle)).check(matches(withText(R.string.scene)))

        scenario.close()
    }

    // Hiding a node has to reach the engine, which only the render can confirm. Compared against
    // the previous frame rather than a baseline, so it holds on every device profile.
    @Test
    fun testHideNodeUpdatesRender() {
        val scenario = launchWithTestFile()

        openScenePanel()
        awaitSceneRows(scenario)

        var before: Image? = null
        scenario.onActivity { activity -> before = captureImage(getMainView(activity!!)) }

        onView(allOf(withId(R.id.nodeVisibility), hasSibling(withText("<stream>"))))
            .perform(click())
        Thread.sleep(1000)

        scenario.onActivity { activity ->
            val after = captureImage(getMainView(activity!!))
            try {
                val difference = after.compare(before!!)
                println("Image difference after hiding the root node: $difference")
                assertTrue(
                    "Hiding the root node did not change the render (diff=$difference)",
                    difference > 0.04
                )
            } finally {
                after.delete()
                before!!.delete()
            }
        }

        scenario.close()
    }
}
