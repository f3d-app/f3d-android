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

    // Simulate a click on the '+' button and cancel, to make sure we do not crash
    @Test
    fun testOpenAndCancel() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

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
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        // Copy test asset to app's internal storage
        val testFile = File(context.filesDir, "f3d.glb")
        instrumentation.context.assets.open("data/f3d.glb").use { input ->
            testFile.outputStream().use { output -> input.copyTo(output) }
        }

        // Create a content URI for the file
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

        // Wait for the file to load and render
        Thread.sleep(3000)

        // Take a screenshot to verify the file was loaded
        activityRule.scenario.onActivity { activity ->
            val mainLayout = activity!!.findViewById<ConstraintLayout>(R.id.mainLayout)
            val mainView = mainLayout.getChildAt(mainLayout.childCount - 1) as MainView

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

            val resolution = "${image.width}x${image.height}"

            // Copy test baseline image to app's internal storage
            val baselineName = "testOpen_$resolution.png"
            val testFile = File(context.filesDir, "baseline.png")
            try {
                instrumentation.context.assets.open("baselines/$baselineName").use { input ->
                    testFile.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (_: java.io.FileNotFoundException) {
                // No baseline for this resolution, save the rendered image as a candidate
                val outputFile = File(context.getExternalFilesDir(null), baselineName)
                image.save(outputFile.absolutePath)
                println("No baseline found for $resolution. Saved rendered image to: ${outputFile.absolutePath}")
                assertTrue("No baseline found for resolution $resolution. Rendered image saved as ${outputFile.absolutePath}", false)
            }

            val baselineImage = Image(testFile.absolutePath)

            val difference = image.compare(baselineImage)
            println("Image difference: $difference")

            if (difference > 0.04) {
                // If the difference is greater than the threshold, save the rendered image for
                // manual inspection
                val outputFile = File(context.getExternalFilesDir(null), baselineName)
                image.save(outputFile.absolutePath)
                println("Saved rendered image to: ${outputFile.absolutePath}")

                assertTrue("Rendered image differs from the baseline (diff=$difference, resolution=$resolution)", false)
            }
        }
    }

    // Simulate opening a file from a file explorer by launching the activity with an ACTION_VIEW intent
    @Test
    fun testOpenFromFileExplorer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        // Copy test asset to app's internal storage
        val testFile = File(context.filesDir, "f3d.glb")
        instrumentation.context.assets.open("data/f3d.glb").use { input ->
            testFile.outputStream().use { output -> input.copyTo(output) }
        }

        // Create an ACTION_VIEW intent with the file URI, as if a file explorer selected F3D
        val uri = Uri.fromFile(testFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            setClassName(context, "app.f3d.F3D.android.MainActivity")
        }

        // Launch the activity with the ACTION_VIEW intent
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        // Wait for the file to load and render
        Thread.sleep(3000)

        // Take a screenshot to verify the file was loaded
        scenario.onActivity { activity ->
            val mainLayout = activity!!.findViewById<ConstraintLayout>(R.id.mainLayout)
            val mainView = mainLayout.getChildAt(mainLayout.childCount - 1) as MainView

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

            val resolution = "${image.width}x${image.height}"

            // Copy test baseline image to app's internal storage
            val baselineName = "testOpen_$resolution.png"
            val baselineFile = File(context.filesDir, "baseline.png")
            try {
                instrumentation.context.assets.open("baselines/$baselineName").use { input ->
                    baselineFile.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (_: java.io.FileNotFoundException) {
                // No baseline for this resolution, save the rendered image as a candidate
                val outputFile = File(context.getExternalFilesDir(null), baselineName)
                image.save(outputFile.absolutePath)
                println("No baseline found for $resolution. Saved rendered image to: ${outputFile.absolutePath}")
                assertTrue("No baseline found for resolution $resolution. Rendered image saved as ${outputFile.absolutePath}", false)
            }

            val baselineImage = Image(baselineFile.absolutePath)

            val difference = image.compare(baselineImage)
            println("Image difference: $difference")

            if (difference > 0.04) {
                // If the difference is greater than the threshold, save the rendered image for
                // manual inspection
                val outputFile = File(context.getExternalFilesDir(null), baselineName)
                image.save(outputFile.absolutePath)
                println("Saved rendered image to: ${outputFile.absolutePath}")

                assertTrue("Rendered image differs from the baseline (diff=$difference, resolution=$resolution)", false)
            }
        }

        scenario.close()
    }

    // Open a file, rotate 90 degrees, switch apps, come back, and verify the view
    @Test
    fun testRotateAndResume() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        // Copy test asset to app's internal storage
        val testFile = File(context.filesDir, "f3d.glb")
        instrumentation.context.assets.open("data/f3d.glb").use { input ->
            testFile.outputStream().use { output -> input.copyTo(output) }
        }

        // Launch the activity with the file via ACTION_VIEW
        val uri = Uri.fromFile(testFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            setClassName(context, "app.f3d.F3D.android.MainActivity")
        }

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        // Wait for the file to load and render
        Thread.sleep(3000)

        // Perform a rotation (equivalent to a 100px horizontal swipe) on the GL thread
        scenario.onActivity { activity ->
            val mainLayout = activity!!.findViewById<ConstraintLayout>(R.id.mainLayout)
            val mainView = mainLayout.getChildAt(mainLayout.childCount - 1) as MainView

            val latch = CountDownLatch(1)
            mainView.queueEvent {
                mainView.rotateCamera(90.0, 0.0)
                latch.countDown()
            }
            latch.await(5, TimeUnit.SECONDS)
        }

        // Wait for the swipe to take effect
        Thread.sleep(1000)

        // Simulate switching to another app (moves to CREATED = onStop called)
        scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(1000)

        // Come back to the app
        scenario.moveToState(Lifecycle.State.RESUMED)
        Thread.sleep(2000)

        // Take a screenshot and compare with baseline
        scenario.onActivity { activity ->
            val mainLayout = activity!!.findViewById<ConstraintLayout>(R.id.mainLayout)
            val mainView = mainLayout.getChildAt(mainLayout.childCount - 1) as MainView

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

            val resolution = "${image.width}x${image.height}"

            val baselineName = "testRotateAndResume_$resolution.png"
            val baselineFile = File(context.filesDir, "baseline.png")
            try {
                instrumentation.context.assets.open("baselines/$baselineName").use { input ->
                    baselineFile.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (_: java.io.FileNotFoundException) {
                // No baseline for this resolution, save the rendered image as a candidate
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

        scenario.close()
    }
}
