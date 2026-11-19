package com.example.media

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VoiceRecorderPlayer(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    private var timerJob: Job? = null
    private var recordStartMs: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _recordedBase64 = MutableStateFlow<String?>(null)
    val recordedBase64: StateFlow<String?> = _recordedBase64.asStateFlow()

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    fun startRecording(scope: CoroutineScope): Result<Unit> {
        stopPlayback()
        discardRecording()
        return try {
            val outFile = File(context.cacheDir, "kigo_voice_${System.currentTimeMillis()}.m4a")
            currentFile = outFile
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(32000)
            mr.setAudioSamplingRate(16000)
            mr.setMaxDuration(45_000)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            recordStartMs = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationMs.value = 0L
            timerJob?.cancel()
            timerJob = scope.launch(Dispatchers.Main) {
                while (_isRecording.value) {
                    _recordingDurationMs.value = System.currentTimeMillis() - recordStartMs
                    delay(200)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            cleanupRecorder()
            Result.failure(e)
        }
    }

    fun stopRecording(): Result<Pair<String, Long>> {
        timerJob?.cancel()
        val duration = (_recordingDurationMs.value).coerceAtLeast(1000L)
        return try {
            recorder?.stop()
            cleanupRecorder()
            _isRecording.value = false
            val file = currentFile ?: return Result.failure(IllegalStateException("Recording file missing"))
            val bytes = file.readBytes()
            if (bytes.size > MAX_AUDIO_BYTES) {
                file.delete()
                return Result.failure(IllegalStateException("Voice clip exceeds 420 KB limit. Keep clips under 45 seconds."))
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            _recordedBase64.value = b64
            Result.success(b64 to duration)
        } catch (e: Exception) {
            cleanupRecorder()
            _isRecording.value = false
            Result.failure(e)
        }
    }

    fun discardRecording() {
        timerJob?.cancel()
        if (_isRecording.value) {
            try {
                recorder?.stop()
            } catch (_: Exception) { }
            cleanupRecorder()
        }
        _isRecording.value = false
        _recordingDurationMs.value = 0L
        _recordedBase64.value = null
        currentFile?.delete()
        currentFile = null
    }

    fun togglePlayPause(messageId: String, audioBase64: String) {
        if (audioBase64.isBlank()) return
        if (_playingMessageId.value == messageId && player != null) {
            val mp = player!!
            if (mp.isPlaying) {
                mp.pause()
                _isPaused.value = true
            } else {
                mp.start()
                _isPaused.value = false
            }
            return
        }
        stopPlayback()
        try {
            val bytes = Base64.decode(audioBase64, Base64.NO_WRAP)
            val tempFile = File(context.cacheDir, "kigo_play_${messageId}.m4a")
            tempFile.writeBytes(bytes)
            val mp = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    _playingMessageId.value = null
                    _isPaused.value = false
                    tempFile.delete()
                }
                prepare()
                start()
            }
            player = mp
            _playingMessageId.value = messageId
            _isPaused.value = false
        } catch (_: Exception) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) { }
        try {
            player?.release()
        } catch (_: Exception) { }
        player = null
        _playingMessageId.value = null
        _isPaused.value = false
    }

    private fun cleanupRecorder() {
        try {
            recorder?.release()
        } catch (_: Exception) { }
        recorder = null
    }

    fun releaseAll() {
        discardRecording()
        stopPlayback()
    }

    private companion object {
        const val MAX_AUDIO_BYTES = 420 * 1024
    }
}
