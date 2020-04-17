package de.wiomoc.tv.video

import android.content.Context
import android.util.Pair
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection

class AC3PreferringTrackSelector(context: Context) : DefaultTrackSelector(context) {
    override fun selectAudioTrack(
        groups: TrackGroupArray,
        formatSupports: Array<out IntArray>,
        mixedMimeTypeAdaptationSupports: Int,
        params: Parameters,
        enableAdaptiveTrackSelection: Boolean
    ): Pair<TrackSelection.Definition, AudioTrackScore>? {
        // XXX Prefer the AC3 over MP2
        for (i in 0 until groups.length) {
            groups[i].getFormat(0).let { format ->
                if (format.sampleMimeType == "audio/ac3") {
                    Format::class.java.getDeclaredField("bitrate").apply {
                        isAccessible = true
                        // XXX increment bitrate by one, so it going to win over MP2 if MP2 has the same bitrate
                        setInt(format, format.bitrate + 1)
                    }
                }
            }
        }

        return super.selectAudioTrack(
            groups,
            formatSupports,
            mixedMimeTypeAdaptationSupports,
            params,
            enableAdaptiveTrackSelection
        )
    }
}
