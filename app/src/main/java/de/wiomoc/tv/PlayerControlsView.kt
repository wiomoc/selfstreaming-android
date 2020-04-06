package de.wiomoc.tv

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import java.util.*

class PlayerControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val view: View =
        LayoutInflater.from(context).inflate(R.layout.player_controls, this) // Sideeffect! attaches View
    private val startTimeTextView: TextView = view.findViewById(R.id.player_controls_start_time)
    private val endTimeTextView: TextView = view.findViewById(R.id.player_controls_end_time)
    private val progress: ProgressBar = view.findViewById(R.id.player_controls_progress)
    private val exitFullscreenButton: ImageView = view.findViewById(R.id.player_controls_exit_full_screen)

    init {
        onConfigurationChanged(resources.configuration)
    }

    var isVisible: Boolean = true
        set(value) {
            if (value) {
                visibility = View.VISIBLE
                updateProgress()
            } else {
                visibility = View.GONE
            }
            field = value
        }

    var currentEvent: EPGEvent? = null
        set(value) {
            field = value!!
            updateProgress()
            startTimeTextView.text = String.format("%02d:%02d", value.time.hours, value.time.minutes)
            val endTimeDate = Date(value.time.time + value.duration * 1000)
            endTimeTextView.text = String.format("%02d:%02d", endTimeDate.hours, endTimeDate.minutes)
        }

    private fun updateProgress() {
        if (!isVisible) return
        val event = currentEvent ?: return
        val now = Date().time
        val offsetToEventStartMillis = now - event.time.time
        if (offsetToEventStartMillis < 0) return // Event in the future

        progress.progress = (offsetToEventStartMillis.toFloat() / (event.duration.toFloat() * 1000) * 100).toInt()

        postDelayed(this::updateProgress, 10000)
    }

    fun setOnExitFullscreenListener(listener: (View) -> Unit) =
        exitFullscreenButton.setOnClickListener(listener)

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            exitFullscreenButton.visibility = View.VISIBLE
        } else {
            exitFullscreenButton.visibility = View.GONE
        }
    }

}
