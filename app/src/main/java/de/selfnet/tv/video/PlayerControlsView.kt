package de.selfnet.tv.video

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import de.selfnet.tv.service.formatTime
import de.wiomoc.tv.R
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
    private val autoHideHandler = Handler(Looper.getMainLooper())
    private val autoHideHandlerToken = 10


    init {
        onConfigurationChanged(resources.configuration)
        startAutoHideTimer(5000)
    }

    fun toggleVisibility(autoHide: Boolean = true) {
        autoHideHandler.removeCallbacksAndMessages(autoHideHandlerToken)
        isVisible = !isVisible
        if (isVisible && autoHide) {
            startAutoHideTimer(3000)
        }
    }

    private fun startAutoHideTimer(timeoutMS: Int) {
        autoHideHandler.postAtTime({
            isVisible = false
        }, autoHideHandlerToken, SystemClock.uptimeMillis() + timeoutMS)
    }

    var isVisible: Boolean = true
        set(value) {
            field = value
            if (value) {
                updateProgress()
                visibility = View.VISIBLE
                view.animate().setDuration(300).alpha(1.0f)
            } else {
                animate().setDuration(300).alpha(0.0f).withEndAction {
                    visibility = View.GONE
                }
            }
        }

    var currentEvent: EPGEvent? = null
        set(value) {
            field = value!!
            updateProgress()
            startTimeTextView.text = formatTime(value.time)
            endTimeTextView.text = formatTime(Date(value.time.time + value.duration * 1000))
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
