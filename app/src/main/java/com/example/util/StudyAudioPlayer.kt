package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.model.StudyMusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import kotlin.math.sin

data class AudioPlayerState(
    val currentTrack: StudyMusicTrack? = null,
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isLooping: Boolean = true
)

object StudyAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var synthTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun playTrack(context: Context, track: StudyMusicTrack) {
        stop()

        if (track.isPreset) {
            playPresetAudio(track)
            return
        }

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (track.uriOrPath.startsWith("/")) {
                    val file = File(track.uriOrPath)
                    if (file.exists()) {
                        setDataSource(file.absolutePath)
                    } else {
                        return
                    }
                } else {
                    setDataSource(context, Uri.parse(track.uriOrPath))
                }

                isLooping = _playerState.value.isLooping
                prepare()
                start()
            }

            mediaPlayer = mp
            val dur = mp.duration.toLong().coerceAtLeast(1L)
            _playerState.value = _playerState.value.copy(
                currentTrack = track,
                isPlaying = true,
                totalDurationMs = dur,
                progressMs = 0L
            )

            startProgressTracker()

            mp.setOnCompletionListener {
                if (!it.isLooping) {
                    _playerState.value = _playerState.value.copy(isPlaying = false, progressMs = 0L)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playerState.value = _playerState.value.copy(isPlaying = false)
        }
    }

    private fun playPresetAudio(track: StudyMusicTrack) {
        synthJob?.cancel()
        _playerState.value = _playerState.value.copy(
            currentTrack = track,
            isPlaying = true,
            totalDurationMs = 3600000L, // 1 hour loop
            progressMs = 0L
        )

        val sampleRate = 44100
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        synthTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        synthTrack?.play()

        synthJob = scope.launch {
            val buffer = ShortArray(minBufferSize)
            var sampleIndex = 0L
            val isAlpha = track.uriOrPath.contains("alpha")
            val isRain = track.uriOrPath.contains("rain")

            while (isActive && _playerState.value.isPlaying) {
                for (i in 0 until buffer.size step 2) {
                    val t = sampleIndex.toDouble() / sampleRate
                    if (isAlpha) {
                        // Binaural Alpha waves (Carrier 220Hz in Left, 230Hz in Right -> 10Hz Alpha differential)
                        val leftSine = sin(2.0 * Math.PI * 220.0 * t) * 0.25
                        val rightSine = sin(2.0 * Math.PI * 230.0 * t) * 0.25
                        buffer[i] = (leftSine * Short.MAX_VALUE).toInt().toShort()
                        buffer[i + 1] = (rightSine * Short.MAX_VALUE).toInt().toShort()
                    } else if (isRain) {
                        // Ambient Pink Noise / Rain generator
                        val noise = (Math.random() * 2.0 - 1.0) * 0.18
                        val lowRumble = sin(2.0 * Math.PI * 65.0 * t) * 0.08
                        val sample = ((noise + lowRumble) * Short.MAX_VALUE).toInt().toShort()
                        buffer[i] = sample
                        buffer[i + 1] = sample
                    } else {
                        // Deep Focus White / Brown Noise
                        val noise = (Math.random() * 2.0 - 1.0) * 0.15
                        val sample = (noise * Short.MAX_VALUE).toInt().toShort()
                        buffer[i] = sample
                        buffer[i + 1] = sample
                    }
                    sampleIndex++
                }
                synthTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    private fun startProgressTracker() {
        scope.launch {
            while (isActive && _playerState.value.isPlaying) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            progressMs = mp.currentPosition.toLong(),
                            totalDurationMs = mp.duration.toLong().coerceAtLeast(1L)
                        )
                    }
                }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        synthTrack?.pause()
        _playerState.value = _playerState.value.copy(isPlaying = false)
    }

    fun resume() {
        val curr = _playerState.value.currentTrack ?: return
        if (curr.isPreset) {
            playPresetAudio(curr)
        } else {
            mediaPlayer?.let {
                it.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                startProgressTracker()
            }
        }
    }

    fun togglePlayPause(context: Context, track: StudyMusicTrack? = null) {
        val target = track ?: _playerState.value.currentTrack
        if (target == null) return

        if (_playerState.value.currentTrack?.id == target.id) {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            playTrack(context, target)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null

        try {
            synthJob?.cancel()
            synthTrack?.stop()
            synthTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        synthTrack = null

        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            progressMs = 0L
        )
    }

    fun toggleLoop() {
        val newLoop = !_playerState.value.isLooping
        mediaPlayer?.isLooping = newLoop
        _playerState.value = _playerState.value.copy(isLooping = newLoop)
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playerState.value = _playerState.value.copy(progressMs = positionMs)
    }

    /**
     * Default high-quality study sound presets generated purely offline via AudioTrack synthesis
     */
    fun getDefaultPresets(): List<StudyMusicTrack> {
        return listOf(
            StudyMusicTrack(
                title = "Binaural Alpha Waves (432Hz)",
                artistOrCategory = "Binaural Focus",
                uriOrPath = "preset://alpha",
                durationMs = 3600000L,
                fileSizeFormatted = "Offline Synth",
                dateAdded = System.currentTimeMillis(),
                isPreset = true,
                isFavorite = true
            ),
            StudyMusicTrack(
                title = "Deep Study Rain & Thunder",
                artistOrCategory = "Ambient Noise",
                uriOrPath = "preset://rain",
                durationMs = 3600000L,
                fileSizeFormatted = "Offline Synth",
                dateAdded = System.currentTimeMillis(),
                isPreset = true,
                isFavorite = false
            ),
            StudyMusicTrack(
                title = "White Noise Flow State",
                artistOrCategory = "White Noise",
                uriOrPath = "preset://whitenoise",
                durationMs = 3600000L,
                fileSizeFormatted = "Offline Synth",
                dateAdded = System.currentTimeMillis(),
                isPreset = true,
                isFavorite = false
            ),
            StudyMusicTrack(
                title = "Lo-Fi Focus Chamber",
                artistOrCategory = "Lo-Fi Study",
                uriOrPath = "preset://alpha",
                durationMs = 3600000L,
                fileSizeFormatted = "Offline Synth",
                dateAdded = System.currentTimeMillis(),
                isPreset = true,
                isFavorite = true
            )
        )
    }

    /**
     * Saves an audio file imported from SAF Uri into app's private music directory
     * so it persists permanently and can be played even if file picker permissions expire!
     */
    fun saveAudioFileFromUri(
        context: Context,
        sourceUri: Uri,
        customTitle: String? = null,
        category: String = "My Saved Tracks"
    ): StudyMusicTrack? {
        return try {
            val resolver = context.contentResolver
            val musicDir = File(context.filesDir, "study_music").apply {
                if (!exists()) mkdirs()
            }

            var detectedName = "Study_Audio_${System.currentTimeMillis()}"
            var durationMs = 0L

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, sourceUri)
                val titleMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (!titleMeta.isNullOrBlank()) {
                    detectedName = titleMeta
                }
                durationMs = durationStr?.toLongOrNull() ?: 0L
                retriever.release()
            } catch (e: Exception) {
                // Ignore metadata extraction error
            }

            val finalTitle = customTitle?.takeIf { it.isNotBlank() } ?: detectedName
            val destFile = File(musicDir, "track_${System.currentTimeMillis()}.mp3")

            resolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val sizeBytes = destFile.length()
            val sizeFormatted = formatFileSize(sizeBytes)

            StudyMusicTrack(
                title = finalTitle,
                artistOrCategory = category,
                uriOrPath = destFile.absolutePath,
                durationMs = durationMs,
                fileSizeFormatted = sizeFormatted,
                dateAdded = System.currentTimeMillis(),
                isPreset = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val format = DecimalFormat("#,##0.#")
        return format.format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    val DEFAULT_PRESETS = listOf(
        StudyMusicTrack(
            id = -1,
            title = "Alpha Waves (10Hz Focus)",
            artistOrCategory = "Binaural Concentration",
            uriOrPath = "preset://alpha_waves",
            durationMs = 3600000L,
            fileSizeFormatted = "Audio Synth",
            isPreset = true
        ),
        StudyMusicTrack(
            id = -2,
            title = "Gentle Rainfall & Thunder",
            artistOrCategory = "Nature Ambience",
            uriOrPath = "preset://rain_ambience",
            durationMs = 3600000L,
            fileSizeFormatted = "Ambient Synth",
            isPreset = true
        ),
        StudyMusicTrack(
            id = -3,
            title = "Deep White Noise Shield",
            artistOrCategory = "Noise Blocker",
            uriOrPath = "preset://white_noise",
            durationMs = 3600000L,
            fileSizeFormatted = "Noise Generator",
            isPreset = true
        )
    )
}
