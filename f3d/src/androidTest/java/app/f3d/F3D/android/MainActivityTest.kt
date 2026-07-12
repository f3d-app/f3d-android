package app.f3d.F3D.android

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.f3d.F3D.Image
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
        val mainLayout = activity.findViewById<ConstraintLayout>(R.id.mainLayout)
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
        val difference = image.compare(baselineImage)
        println("Image difference: $difference")

        if (difference > 0.04) {
            val outputFile = File(context.getExternalFilesDir(null), baselineName)
            image.save(outputFile.absolutePath)
            println("Saved rendered image to: ${outputFile.absolutePath}")
            assertTrue("Rendered image differs from the baseline (diff=$difference, resolution=$resolution)", false)
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
            assertMatchesBaseline(image, "testOpen")
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
            assertMatchesBaseline(image, "testOpen")
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
            assertMatchesBaseline(image, "testOpen")
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
            assertMatchesBaseline(image, "testRotateAndResume")
        }

        scenario.close()
    }

    // Open a file, change an option as the options panel would, and verify the render reflects it
    @Test
    fun testChangeOption() {
        val testFile = "data/f3d.glb".copyTestAsset("f3d.glb")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(testFile), "*/*")
            setClassName(context, "app.f3d.F3D.android.MainActivity")
        }

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        Thread.sleep(3000)

        // Change the background color option, as the options panel color picker would
        scenario.onActivity { activity ->
            val mainView = getMainView(activity!!)
            val latch = CountDownLatch(1)
            mainView.applyOption {
                it.setAsDoubleVector("render.background.color", doubleArrayOf(1.0, 0.0, 0.0))
                latch.countDown()
            }
            latch.await(5, TimeUnit.SECONDS)
        }

        Thread.sleep(1000)

        scenario.onActivity { activity ->
            val image = captureImage(getMainView(activity!!))
            assertMatchesBaseline(image, "testChangeOption")
        }

        scenario.close()
    }
}
