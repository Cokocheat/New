package com.example.game.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    private val sampleRate = 22050
    private var lastEatTime = 0L

    // In-memory static AudioTracks - zero disk I/O, no Stagefright/OMX component queries
    private val eatTracks = arrayOfNulls<AudioTrack>(3)
    private var eatTrackIndex = 0
    private var potionTrack: AudioTrack? = null
    private var crashTrack: AudioTrack? = null
    private var clickTrack: AudioTrack? = null
    private var killTrack: AudioTrack? = null

    init {
        scope.launch {
            try {
                val popData = generateSmoothPopSound()
                for (i in eatTracks.indices) {
                    eatTracks[i] = createStaticTrack(popData, sampleRate)
                }
                potionTrack = createStaticTrack(generatePotionSound(), sampleRate)
                crashTrack = createStaticTrack(generateCrashSound(), sampleRate)
                clickTrack = createStaticTrack(generateClickSound(), sampleRate)
                killTrack = createStaticTrack(generateKillSound(), sampleRate)
            } catch (_: Throwable) {}
        }
    }

    private fun createStaticTrack(pcmData: ShortArray, rate: Int): AudioTrack? {
        return try {
            val minBuf = AudioTrack.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(pcmData.size * 2, if (minBuf > 0) minBuf else pcmData.size * 2)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track
        } catch (_: Throwable) {
            null
        }
    }

    private fun playTrack(track: AudioTrack?, volume: Float) {
        if (!isSoundEnabled || track == null) return
        try {
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.setVolume(volume.coerceIn(0.0f, 1.0f))
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        } catch (_: Throwable) {}
    }

    // Silky smooth pop sound with round-robin pool & throttling
    fun playEat() {
        if (!isSoundEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastEatTime < 45) return
        lastEatTime = now

        try {
            val track = eatTracks[eatTrackIndex]
            eatTrackIndex = (eatTrackIndex + 1) % eatTracks.size
            playTrack(track, 0.40f)
        } catch (_: Throwable) {}
    }

    fun playPotion() {
        if (!isSoundEnabled) return
        vibrate(40)
        try {
            playTrack(potionTrack, 0.70f)
        } catch (_: Throwable) {}
    }

    fun playCrash() {
        if (isVibrationEnabled) vibrate(120)
        if (!isSoundEnabled) return
        try {
            playTrack(crashTrack, 0.75f)
        } catch (_: Throwable) {}
    }

    fun playClick() {
        if (!isSoundEnabled) return
        vibrate(20)
        try {
            playTrack(clickTrack, 0.50f)
        } catch (_: Throwable) {}
    }

    fun playKill() {
        if (!isSoundEnabled) return
        vibrate(70)
        try {
            playTrack(killTrack, 0.85f)
        } catch (_: Throwable) {}
    }

    private fun vibrate(durationMs: Long) {
        if (!isVibrationEnabled) return
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    fun release() {
        try {
            eatTracks.forEach {
                try {
                    it?.stop()
                    it?.release()
                } catch (_: Throwable) {}
            }
            potionTrack?.let { try { it.stop(); it.release() } catch (_: Throwable) {} }
            crashTrack?.let { try { it.stop(); it.release() } catch (_: Throwable) {} }
            clickTrack?.let { try { it.stop(); it.release() } catch (_: Throwable) {} }
            killTrack?.let { try { it.stop(); it.release() } catch (_: Throwable) {} }
        } catch (_: Throwable) {}
    }

    // Soft bubble pop sound: warm sine envelope, no square distortion
    private fun generateSmoothPopSound(): ShortArray {
        val numSamples = (sampleRate * 0.055f).toInt()
        val buffer = ShortArray(numSamples)
        val startFreq = 620.0
        val endFreq = 380.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            val env = sin(progress * PI).pow(1.5)
            val sample = sin(2 * PI * freq * (i.toDouble() / sampleRate)) * env
            buffer[i] = (sample * 11000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    // Triumphant kill sound: bright 2-note fanfare (E6 to G6)
    private fun generateKillSound(): ShortArray {
        val numSamples = (sampleRate * 0.22f).toInt()
        val buffer = ShortArray(numSamples)
        val half = numSamples / 2
        for (i in 0 until numSamples) {
            val freq = if (i < half) 659.25 else 783.99
            val noteProgress = if (i < half) i.toDouble() / half else (i - half).toDouble() / (numSamples - half)
            val env = (1.0 - noteProgress).pow(1.2) * sin(noteProgress * PI).coerceAtLeast(0.1)
            val sample = (sin(2 * PI * freq * (i.toDouble() / sampleRate)) * 0.75 +
                    sin(4 * PI * freq * (i.toDouble() / sampleRate)) * 0.25) * env
            buffer[i] = (sample * 16000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generatePotionSound(): ShortArray {
        val numSamples = (sampleRate * 0.22f).toInt()
        val buffer = ShortArray(numSamples)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        val samplesPerNote = numSamples / notes.size
        for (i in 0 until numSamples) {
            val noteIdx = min(i / samplesPerNote, notes.size - 1)
            val freq = notes[noteIdx]
            val noteProgress = (i % samplesPerNote).toDouble() / samplesPerNote
            val env = (1.0 - noteProgress).pow(1.5)
            val sample = sin(2 * PI * freq * (i.toDouble() / sampleRate)) * env
            buffer[i] = (sample * 14000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateCrashSound(): ShortArray {
        val numSamples = (sampleRate * 0.28f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val boomFreq = 110.0 * (1.0 - progress)
            val boom = sin(2 * PI * boomFreq * (i.toDouble() / sampleRate))
            val env = (1.0 - progress).pow(2.2)
            val sample = boom * env
            buffer[i] = (sample * 16000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateClickSound(): ShortArray {
        val numSamples = (sampleRate * 0.03f).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val env = 1.0 - progress
            val sample = sin(2 * PI * 1100.0 * (i.toDouble() / sampleRate)) * env
            buffer[i] = (sample * 9000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }
}
