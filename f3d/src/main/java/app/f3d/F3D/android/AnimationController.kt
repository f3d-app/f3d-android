package app.f3d.F3D.android

import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import java.util.Locale

class AnimationController(private val view: MainView, root: View) {

    private val context = root.context

    private val infoBar: View = root.findViewById(R.id.animInfoBar)
    private val controlBar: View = root.findViewById(R.id.animControlBar)
    private val nameLabel: TextView = root.findViewById(R.id.animNameLabel)
    private val timeLabel: TextView = root.findViewById(R.id.animTimeLabel)
    private val speedLabel: TextView = root.findViewById(R.id.animSpeedLabel)
    private val playButton: ImageButton = root.findViewById(R.id.animPlayButton)
    private val prevButton: ImageButton = root.findViewById(R.id.animPrevButton)
    private val nextButton: ImageButton = root.findViewById(R.id.animNextButton)
    private val seekBar: AnimationSeekBar = root.findViewById(R.id.animSeekBar)
    private val speedButton: TextView = root.findViewById(R.id.animSpeedButton)

    private var hasAnimation = false
    private var hiddenForChrome = false
    private var names: List<String> = emptyList()
    private var activeIndex = 0
    private var speedIndex = DEFAULT_SPEED_INDEX
    private var isSeeking = false

    init {
        playButton.setOnClickListener { view.toggleAnimation() }
        prevButton.setOnClickListener { step(-1) }
        nextButton.setOnClickListener { step(+1) }
        speedButton.setOnClickListener { cycleSpeed() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) view.seekAnimation(progress.toDouble() / sb.max)
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
                isSeeking = false
            }
        })

        view.onAnimationLoaded = { info -> onLoaded(info) }
        view.onAnimationProgress = { current, min, max -> onProgress(current, min, max) }
        view.onPlayStateChanged = { playing -> updatePlayIcon(playing) }

        updateSpeedLabel()
    }

    /** Hides the controls while another surface (e.g. the console) takes over the screen. */
    fun setHiddenForChrome(hidden: Boolean) {
        hiddenForChrome = hidden
        applyVisibility()
    }

    private fun onLoaded(info: AnimationInfo) {
        hasAnimation = info.available > 0
        names = info.names
        activeIndex = info.activeIndex

        if (hasAnimation) {
            val span = info.max - info.min
            seekBar.setKeyFrameFractions(
                if (span > 0.0) {
                    info.keyFrames.map { ((it - info.min) / span).toFloat() }.toFloatArray()
                } else {
                    FloatArray(0)
                }
            )

            val multiple = names.size > 1
            prevButton.visibility = if (multiple) View.VISIBLE else View.GONE
            nextButton.visibility = if (multiple) View.VISIBLE else View.GONE

            val name = names.getOrElse(activeIndex) { "" }
            nameLabel.text = name
            nameLabel.visibility = if (name.isEmpty()) View.GONE else View.VISIBLE

            onProgress(info.min, info.min, info.max)

            if (info.resetPlayback) {
                speedIndex = DEFAULT_SPEED_INDEX
                updateSpeedLabel()
                updatePlayIcon(false)
            }
        }
        applyVisibility()
    }

    private fun onProgress(current: Double, min: Double, max: Double) {
        timeLabel.text = String.format(Locale.US, "%.2f / %.2f s", current - min, max - min)
        if (isSeeking) return
        val span = max - min
        val fraction = if (span > 0.0) (current - min) / span else 0.0
        seekBar.progress = (fraction * seekBar.max).toInt().coerceIn(0, seekBar.max)
    }

    private fun step(delta: Int) {
        if (names.size <= 1) return
        val next = ((activeIndex + delta) % names.size + names.size) % names.size
        view.selectAnimation(next)
    }

    private fun cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEEDS.size
        view.setAnimationSpeed(SPEEDS[speedIndex])
        updateSpeedLabel()
    }

    private fun updateSpeedLabel() {
        val text = formatSpeed(SPEEDS[speedIndex])
        speedButton.text = text
        speedLabel.text = text
    }

    private fun updatePlayIcon(playing: Boolean) {
        playButton.setImageResource(
            if (playing) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24
        )
        playButton.contentDescription =
            context.getString(if (playing) R.string.animation_pause else R.string.animation_play)
    }

    private fun applyVisibility() {
        val visibility = if (hasAnimation && !hiddenForChrome) View.VISIBLE else View.GONE
        infoBar.visibility = visibility
        controlBar.visibility = visibility
    }

    private fun formatSpeed(value: Double): String {
        val number = if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
        return "${number}×"
    }

    private companion object {
        val SPEEDS = doubleArrayOf(0.25, 0.5, 1.0, 2.0, 4.0)
        const val DEFAULT_SPEED_INDEX = 2
    }
}
