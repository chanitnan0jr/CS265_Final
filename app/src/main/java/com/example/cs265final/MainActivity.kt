package com.example.cs265final

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cs265final.data.Note
import com.example.cs265final.data.AudioSource
import com.example.cs265final.data.NoteStorage
import com.example.cs265final.data.TranscriptionStatus
import com.example.cs265final.data.contentDisplayName
import com.example.cs265final.data.filterNotes
import com.example.cs265final.data.indexOfNoteInstance
import com.example.cs265final.data.isPending
import com.example.cs265final.feature.calendar.CalendarScreen
import com.example.cs265final.feature.home.HomeScreen
import com.example.cs265final.feature.notes.LibraryScreen
import com.example.cs265final.feature.notes.NoteCard
import com.example.cs265final.feature.notes.NoteDetailScreen
import com.example.cs265final.feature.recording.RecordingScreen
import com.example.cs265final.feature.playback.PlaybackScreen
import com.example.cs265final.speech.SpeechDraft
import com.example.cs265final.transcription.TranscriptionWorker
import com.example.cs265final.ui.theme.BackgroundCream
import com.example.cs265final.ui.theme.BorderSoft
import com.example.cs265final.ui.theme.CalendarNavIcon
import com.example.cs265final.ui.theme.Cs265FinalTheme
import com.example.cs265final.ui.theme.FolderNavIcon
import com.example.cs265final.ui.theme.HomeNavIcon
import com.example.cs265final.ui.theme.PrimaryCoral
import com.example.cs265final.ui.theme.SearchIcon
import com.example.cs265final.ui.theme.SurfaceWhite
import com.example.cs265final.ui.theme.TextPrimary
import com.example.cs265final.ui.theme.TextSecondary
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import de.sciss.jump3r.mp3.BitStream
import de.sciss.jump3r.mp3.GainAnalysis
import de.sciss.jump3r.mp3.GetAudio
import de.sciss.jump3r.mp3.ID3Tag
import de.sciss.jump3r.mp3.Lame
import de.sciss.jump3r.mp3.MPEGMode
import de.sciss.jump3r.mp3.Parse
import de.sciss.jump3r.mp3.Presets
import de.sciss.jump3r.mp3.PsyModel
import de.sciss.jump3r.mp3.Quantize
import de.sciss.jump3r.mp3.QuantizePVT
import de.sciss.jump3r.mp3.Reservoir
import de.sciss.jump3r.mp3.Takehiro
import de.sciss.jump3r.mp3.VBRTag
import de.sciss.jump3r.mp3.Version
import de.sciss.jump3r.mpg.Common
import de.sciss.jump3r.mpg.Interface as MpgInterface
import de.sciss.jump3r.mpg.MPGLib

internal fun writeWavFile(target: File, pcm: ByteArray, sampleRate: Int = 16_000): File = target.apply {
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray(Charsets.US_ASCII))
        putInt(36 + pcm.size)
        // "WAVEfmt" is the RIFF chunk id; it is not a typo.
        //noinspection SpellCheckingInspection
        put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        putInt(16)
        putShort(1)
        putShort(1)
        putInt(sampleRate)
        putInt(sampleRate * 2)
        putShort(2)
        putShort(16)
        put("data".toByteArray(Charsets.US_ASCII))
        putInt(pcm.size)
    }.array()
    outputStream().use { out ->
        out.write(header)
        out.write(pcm)
    }
}

internal fun writeMp3File(target: File, pcm: ByteArray, sampleRate: Int = 16_000): File = target.apply {
    val lame = Lame(); val ga = GainAnalysis(); val bs = BitStream()
    val p = Presets(); val quantizePvt = QuantizePVT(); val qu = Quantize()
    val vbr = VBRTag(); val ver = Version(); val id3 = ID3Tag()
    val rv = Reservoir(); val tak = Takehiro(); val psy = PsyModel()
    val gaud = GetAudio(); val parse = Parse(); val mpg = MPGLib()
    val mpgInterface = MpgInterface(); val common = Common()
    lame.setModules(ga, bs, p, quantizePvt, qu, vbr, ver, id3, mpg)
    bs.setModules(ga, mpg, ver, vbr); id3.setModules(bs, ver); p.setModules(lame)
    qu.setModules(bs, rv, quantizePvt, tak); quantizePvt.setModules(tak, rv, psy); rv.setModules(bs)
    tak.setModules(quantizePvt); vbr.setModules(lame, bs, ver); gaud.setModules(parse, mpg)
    parse.setModules(ver, id3, p); mpg.setModules(mpgInterface, common); mpgInterface.setModules(vbr, common)
    val gfp = lame.lame_init()
    try {
        gfp.num_channels = 1
        gfp.in_samplerate = sampleRate
        gfp.out_samplerate = sampleRate
        gfp.brate = 64
        gfp.mode = MPEGMode.MONO
        gfp.quality = 5
        gfp.bWriteVbrTag = false
        gfp.write_id3tag_automatic = false
        check(lame.lame_init_params(gfp) >= 0) { "MP3 init failed" }
        val chunk = IntArray(16_384)
        val mp3Buf = ByteArray(32_768)
        val totalSamples = pcm.size / 2
        outputStream().use { out ->
            var pos = 0
            while (pos < totalSamples) {
                val n = minOf(chunk.size, totalSamples - pos)
                for (i in 0 until n) {
                    val b = (pos + i) * 2
                    val lo = pcm[b].toInt() and 0xFF
                    val hi = pcm[b + 1].toInt()
                    chunk[i] = ((hi shl 8) or lo) shl 16
                }
                val encoded = lame.lame_encode_buffer_int(gfp, chunk, chunk, n, mp3Buf, 0, mp3Buf.size)
                check(encoded >= 0) { "MP3 encode failed: $encoded" }
                if (encoded > 0) out.write(mp3Buf, 0, encoded)
                pos += n
            }
            val flushed = lame.lame_encode_flush(gfp, mp3Buf, 0, mp3Buf.size)
            check(flushed >= 0) { "MP3 flush failed: $flushed" }
            if (flushed > 0) out.write(mp3Buf, 0, flushed)
        }
    } finally {
        lame.lame_close(gfp)
    }
}

internal fun isManagedRecording(file: File, directory: File): Boolean {
    val canonical = file.canonicalFile
    return canonical.parentFile == directory.canonicalFile && canonical.name.startsWith("note_") && (canonical.name.endsWith(".mp3") || canonical.name.endsWith(".wav"))
}

internal fun hasNonZeroPcm(pcm: ByteArray): Boolean = pcm.any { it != 0.toByte() }

internal fun resampleMonoPcm16(pcm: ByteArray, srcRate: Int, dstRate: Int = 16_000): ByteArray {
    val srcCount = pcm.size / 2
    if (srcCount == 0 || srcRate <= 0 || srcRate == dstRate) return pcm
    val dstCount = ((srcCount.toLong() * dstRate) / srcRate).toInt().coerceAtLeast(1)
    val out = ByteArray(dstCount * 2)
    val radius = 16
    val cutoff = minOf(1.0, dstRate.toDouble() / srcRate)
    val filters = mutableMapOf<Int, DoubleArray>()
    for (i in 0 until dstCount) {
        val position = i.toLong() * srcRate
        val center = (position / dstRate).toInt()
        val phase = (position % dstRate).toInt()
        val filter = filters.getOrPut(phase) {
            val fraction = phase.toDouble() / dstRate
            DoubleArray(radius * 2) { tap ->
                val distance = fraction - (tap - radius + 1)
                if (kotlin.math.abs(distance) >= radius) 0.0 else {
                    val angle = Math.PI * distance * cutoff
                    val sinc = if (kotlin.math.abs(angle) < 1e-12) 1.0 else kotlin.math.sin(angle) / angle
                    sinc * (0.5 + 0.5 * kotlin.math.cos(Math.PI * distance / radius))
                }
            }
        }
        var sum = 0.0
        var weights = 0.0
        for (tap in filter.indices) {
            val j = center - radius + 1 + tap
            val weight = filter[tap]
            val index = j.coerceIn(0, srcCount - 1) * 2
            val sample = ((pcm[index].toInt() and 0xff) or (pcm[index + 1].toInt() shl 8)).toShort().toInt()
            sum += sample * weight
            weights += weight
        }
        val sample = kotlin.math.round(sum / weights).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        out[i * 2] = sample.toByte()
        out[i * 2 + 1] = (sample shr 8).toByte()
    }
    return out
}

internal fun decodeAudioUriToPcm16k(context: Context, uri: Uri): ByteArray? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    return try {
        extractor.setDataSource(context, uri, null)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(track)
        val trackFormat = extractor.getTrackFormat(track)
        val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
        var sampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 16_000
        var channels = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
        codec = MediaCodec.createDecoderByType(mime).apply {
            configure(trackFormat, null, null, 0)
            start()
        }
        val info = MediaCodec.BufferInfo()
        val monoOut = ByteArrayOutputStream()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val inIdx = codec.dequeueInputBuffer(10_000L)
                if (inIdx >= 0) {
                    val buf = codec.getInputBuffer(inIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, 10_000L)
            when {
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val outFmt = codec.outputFormat
                    if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                outIdx >= 0 -> {
                    if (info.size > 0) {
                        val buf = codec.getOutputBuffer(outIdx)!!
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        buf.get(chunk)
                        if (channels <= 1) {
                            monoOut.write(chunk)
                        } else {
                            val stride = channels * 2
                            var pos = 0
                            while (pos + stride <= chunk.size) {
                                monoOut.write(chunk[pos].toInt())
                                monoOut.write(chunk[pos + 1].toInt())
                                pos += stride
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
        resampleMonoPcm16(monoOut.toByteArray(), sampleRate, 16_000)
    } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        runCatching { extractor.release() }
    }
}

internal class RecordingDraft : ViewModel() {
    var audioBuffer: ByteArrayOutputStream? = null
}

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private var transcript by mutableStateOf("")
    private var listening by mutableStateOf(false)
    private var message by mutableStateOf("")
    private val speechDraft = SpeechDraft()
    private val speechHandler = Handler(Looper.getMainLooper())
    private var speechTimeout: Runnable? = null
    private var speechFinishing = false
    private val recordingDraft by lazy { ViewModelProvider(this)[RecordingDraft::class.java] }
    private var speechAudioBuffer: ByteArrayOutputStream?
        get() = recordingDraft.audioBuffer
        set(value) { recordingDraft.audioBuffer = value }
    private var audioRecordActive: AtomicBoolean? = null
    private var audioRecord: AudioRecord? = null
    private var audioCaptureThread: Thread? = null
    private var audioPipeRead: ParcelFileDescriptor? = null
    private var audioPipeWrite: ParcelFileDescriptor? = null
    private var sessionAudioBuffer: ByteArrayOutputStream? = null
    private var feedbackAudioBuffer: ByteArrayOutputStream? = null
    private var gotSpeechInSession = false
    @SuppressLint("MissingPermission")
    private fun startAudioCapture(targetBuffer: ByteArrayOutputStream?): ParcelFileDescriptor? {
        stopAudioCapture()
        val minBuf = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return null
        val rec = AudioRecord(MediaRecorder.AudioSource.MIC, 16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf * 2, 6_400))
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return null
        }
        val pipe = try { ParcelFileDescriptor.createPipe() } catch (_: Exception) {
            rec.release()
            return null
        }
        val readFd = pipe[0]
        val writeFd = pipe[1]
        audioPipeRead = readFd
        audioPipeWrite = writeFd
        audioRecord = rec
        val active = AtomicBoolean(true)
        audioRecordActive = active
        try {
            rec.startRecording()
        } catch (_: RuntimeException) {
            stopAudioCapture()
            rec.release()
            return null
        }
        val thread = Thread({
            val chunk = ByteArray(3_200)
            var out: ParcelFileDescriptor.AutoCloseOutputStream? = ParcelFileDescriptor.AutoCloseOutputStream(writeFd)
            try {
                while (active.get()) {
                    val n = rec.read(chunk, 0, chunk.size)
                    if (n < 0) break
                    if (n > 0) {
                        targetBuffer?.let { synchronized(it) { it.write(chunk, 0, n) } }
                        out?.let { stream ->
                            runCatching {
                                stream.write(chunk, 0, n)
                                stream.flush()
                            }.onFailure {
                                runCatching { stream.close() }
                                out = null
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Stopping a capture can close the pipe while the reader is active.
            } finally {
                runCatching { out?.close() }
                runCatching { rec.stop() }
                runCatching { rec.release() }
            }
        }, "Voice2NoteMicTee").apply { isDaemon = true; start() }
        audioCaptureThread = thread
        return readFd
    }

    private fun stopAudioCapture() {
        audioRecordActive?.set(false)
        audioRecordActive = null
        audioRecord?.let { runCatching { it.stop() } }
        audioRecord = null
        audioCaptureThread?.takeIf { it != Thread.currentThread() }?.let { runCatching { it.join(200) } }
        audioCaptureThread = null
        runCatching { audioPipeWrite?.close() }
        audioPipeWrite = null
        runCatching { audioPipeRead?.close() }
        audioPipeRead = null
    }

    private fun flushSessionAudio() {
        val recorded = sessionAudioBuffer?.let { synchronized(it) { it.toByteArray() } }?.takeIf(::hasNonZeroPcm)
        val feedback = feedbackAudioBuffer?.let { synchronized(it) { it.toByteArray() } }?.takeIf(::hasNonZeroPcm)
        val chosen = recorded ?: feedback
        if (chosen != null) {
            val dst = speechAudioBuffer ?: ByteArrayOutputStream().also { speechAudioBuffer = it }
            synchronized(dst) { dst.write(chosen) }
            Log.i("Voice2NoteSpeech", "Captured ${chosen.size} PCM bytes (source=${if (recorded != null) "AudioRecord" else "onBufferReceived"})")
        }
        sessionAudioBuffer = null
        feedbackAudioBuffer = null
    }

    private fun clearSpeechTimeout() {
        speechTimeout?.let(speechHandler::removeCallbacks)
        speechTimeout = null
    }

    private fun armSpeechTimeout(session: Int) {
        clearSpeechTimeout()
        speechTimeout = Runnable {
            if (speechDraft.isCurrent(session)) {
                cancelSpeech()
                message = "บริการเสียงไม่ตอบสนอง ข้อความที่ได้ยังอยู่ ลองอีกครั้งได้"
            }
        }.also { speechHandler.postDelayed(it, 15_000L) }
    }

    private fun stopSpeech() {
        if (speechFinishing) return
        speechFinishing = true
        stopAudioCapture()
        flushSessionAudio()
        try {
            recognizer?.stopListening()
            currentSpeechSession?.let(::armSpeechTimeout)
        } catch (_: RuntimeException) {
            cancelSpeech()
            message = "ไม่สามารถหยุดรับเสียงได้ ข้อความที่ได้ยังอยู่"
        }
    }

    private var currentSpeechSession: Int? = null
    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        listening = false
        if (result.resultCode != RESULT_OK) {
            if (result.resultCode != RESULT_CANCELED) message = "ไม่สามารถรับเสียงได้ ลองอีกครั้งหรือพิมพ์ข้อความแทน"
            return@registerForActivityResult
        }
        val data = result.data
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim().orEmpty()
        if (text.isNotEmpty()) {
            val session = speechDraft.start(transcript)
            speechDraft.result(session, text)?.let { transcript = it }
            speechDraft.cancel()
        }
        val uri = data?.data
        Log.i("Voice2NoteSpeech", "Legacy activity result textLen=${text.length} audioUri=$uri")
        if (uri != null) {
            val pcm = runCatching { decodeAudioUriToPcm16k(this, uri) }.getOrNull()?.takeIf(::hasNonZeroPcm)
            if (pcm != null) {
                val dst = speechAudioBuffer ?: ByteArrayOutputStream().also { speechAudioBuffer = it }
                synchronized(dst) { dst.write(pcm) }
                Log.i("Voice2NoteSpeech", "Decoded legacy audio URI to ${pcm.size} PCM bytes")
            }
        }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeech()
        else message = if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            "กรุณาอนุญาตไมโครโฟนเพื่อใช้การบันทึกเสียง"
        } else {
            "เปิดสิทธิ์ไมโครโฟนในการตั้งค่าแอปเพื่อใช้การบันทึกเสียง"
        }
    }
    // readPipe is only non-null on Android 13+, where EXTRA_AUDIO_SOURCE exists.
    @SuppressLint("NewApi")
    private fun startSpeech() {
        val usePipe = Build.VERSION.SDK_INT >= 33
        if (!usePipe) {
            val legacyIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "พูดภาษาไทยเพื่อบันทึกข้อความและเสียง")
                putExtra("android.speech.extra.GET_AUDIO", true)
                putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
            }
            if (legacyIntent.resolveActivity(packageManager) != null) {
                if (speechAudioBuffer == null) speechAudioBuffer = ByteArrayOutputStream()
                message = ""
                listening = true
                speechLauncher.launch(legacyIntent)
                return
            }
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { message = "อุปกรณ์นี้ไม่มีบริการแปลงเสียงเป็นข้อความ"; return }
        val session = speechDraft.start(transcript)
        currentSpeechSession = session
        speechFinishing = false
        gotSpeechInSession = false
        if (speechAudioBuffer == null) speechAudioBuffer = ByteArrayOutputStream()
        val sessionBuf = ByteArrayOutputStream().also { sessionAudioBuffer = it }
        val feedbackBuf = ByteArrayOutputStream().also { feedbackAudioBuffer = it }
        val readPipe = if (usePipe) runCatching { startAudioCapture(sessionBuf) }.getOrElse { stopAudioCapture(); null } else null
        armSpeechTimeout(session)
        listening = true
        message = ""
        try {
        recognizer = (recognizer ?: SpeechRecognizer.createSpeechRecognizer(this)).also { speech ->
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (speechDraft.isCurrent(session)) {
                        listening = true
                        if (!speechFinishing) clearSpeechTimeout()
                    }
                }
                override fun onBeginningOfSpeech() { if (speechDraft.isCurrent(session)) { gotSpeechInSession = true; if (!speechFinishing) clearSpeechTimeout() } }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {
                    if (speechDraft.isCurrent(session) && readPipe == null && buffer != null && buffer.isNotEmpty()) {
                        synchronized(feedbackBuf) { feedbackBuf.write(buffer) }
                    }
                }
                override fun onEndOfSpeech() { if (speechDraft.isCurrent(session)) { speechFinishing = true; stopAudioCapture(); flushSessionAudio(); armSpeechTimeout(session) } }
                override fun onError(error: Int) {
                    if (speechDraft.isCurrent(session)) {
                        stopAudioCapture()
                        Log.w("Voice2NoteSpeech", "Recognition error code: $error gotSpeech=$gotSpeechInSession")
                        if (gotSpeechInSession) flushSessionAudio() else { sessionAudioBuffer = null; feedbackAudioBuffer = null }
                        listening = false
                        clearSpeechTimeout()
                        currentSpeechSession = null
                        speechDraft.cancel()
                        message = "ไม่สามารถรับเสียงได้ ลองอีกครั้งหรือพิมพ์ข้อความแทน"
                    }
                }
                override fun onResults(results: Bundle?) { if (speechDraft.isCurrent(session)) { stopAudioCapture(); flushSessionAudio(); listening = false; clearSpeechTimeout(); currentSpeechSession = null; updateText(results); speechDraft.cancel() } }
                override fun onPartialResults(partialResults: Bundle?) { updateText(partialResults) }
                override fun onEvent(eventType: Int, params: Bundle?) {}
                private fun updateText(bundle: Bundle?) { if (speechDraft.isCurrent(session)) bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { speechDraft.result(session, it)?.let { value -> gotSpeechInSession = true; transcript = value } } }
            })
            speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                if (readPipe != null) {
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readPipe)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16_000)
                }
            })
        }
        } catch (_: RuntimeException) {
            cancelSpeech()
            message = "ไม่สามารถเริ่มรับเสียงได้ กรุณาตรวจสอบสิทธิ์ไมโครโฟนและบริการเสียง"
        }
    }
    private fun cancelSpeech() { stopAudioCapture(); sessionAudioBuffer = null; feedbackAudioBuffer = null; clearSpeechTimeout(); currentSpeechSession = null; speechFinishing = false; speechDraft.cancel(); recognizer?.cancel(); recognizer?.destroy(); recognizer = null; listening = false }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("draft_transcript", transcript)
        super.onSaveInstanceState(outState)
    }
    override fun onStop() { cancelSpeech(); super.onStop() }
    override fun onDestroy() { stopAudioCapture(); recognizer?.destroy(); super.onDestroy() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transcript = savedInstanceState?.getString("draft_transcript").orEmpty()
        val storage = NoteStorage(this)
        val initialNotes = runCatching { storage.load() }
        setContent {
            var storageError by remember { mutableStateOf(initialNotes.isFailure) }
            var notes by remember { mutableStateOf(initialNotes.getOrDefault(emptyList())) }
            var page by remember { mutableStateOf("หน้าหลัก") }
            var editing by rememberSaveable { mutableStateOf(false) }
            var title by rememberSaveable { mutableStateOf("") }
            var mood by rememberSaveable { mutableStateOf("ปกติ") }
            var folder by rememberSaveable { mutableStateOf("") }
            var query by remember { mutableStateOf("") }
            var selected by remember { mutableStateOf<Note?>(null) }
            var playbackUri by remember { mutableStateOf<Uri?>(null) }
            val uploadMimeTypes = remember { arrayOf("audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/wav", "audio/x-wav") }
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("th-TH")) }
            var date by remember { mutableStateOf(dateFormat.format(Date())) }
            val startComposer = {
                transcript = ""
                title = ""
                folder = ""
                mood = "ปกติ"
                message = ""
                speechAudioBuffer = null
                editing = true
            }
            val updateNote: (Note, Note) -> Boolean = { current, replacement ->
                val index = notes.indexOfNoteInstance(current)
                if (index < 0 || storageError) false
                else {
                    // savePreservingTranscriptions can replace a pending note, so re-resolve the selection by position.
                    val selectedIndex = selected?.let { notes.indexOfNoteInstance(it) } ?: -1
                    val updated = notes.toMutableList().apply { this[index] = replacement }
                    val saved = runCatching { storage.savePreservingTranscriptions(updated) }.getOrNull()
                    if (saved == null) false
                    else {
                        notes = saved
                        if (selectedIndex >= 0) selected = saved.getOrNull(selectedIndex)
                        true
                    }
                }
            }
            val enqueueTranscription: (Note) -> Unit = { note ->
                if (note.jobId.isNotBlank()) {
                    WorkManager.getInstance(this@MainActivity).enqueueUniqueWork(
                        note.jobId,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<TranscriptionWorker>()
                            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                            .setInputData(workDataOf(TranscriptionWorker.KEY_JOB_ID to note.jobId))
                            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                            .build(),
                    )
                }
            }
            val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    val persistent = runCatching {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        true
                    }.getOrElse {
                        runCatching { contentResolver.persistedUriPermissions.any { grant -> grant.uri == uri && grant.isReadPermission } }.getOrDefault(false)
                    }
                    if (!persistent) {
                        Toast.makeText(this@MainActivity, "ไม่สามารถเก็บสิทธิ์เข้าถึงไฟล์ได้ ไฟล์นี้จะเล่นได้ชั่วคราวเท่านั้น", Toast.LENGTH_LONG).show()
                    } else {
                        val current = selected
                        if (current != null) {
                            // The detail action is for playback; only the Home upload sends audio away for transcription.
                            if (updateNote(current, current.copy(audioUri = uri.toString(), audioSource = AudioSource.UPLOADED))) {
                                Toast.makeText(this@MainActivity, "เพิ่มไฟล์เสียงแล้ว", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "ไม่สามารถบันทึกไฟล์ลงในโน้ตได้", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val jobId = UUID.randomUUID().toString()
                            val uploaded = Note(
                                title = contentDisplayName(contentResolver, uri) ?: "ไฟล์เสียงที่อัปโหลด",
                                body = "",
                                mood = "ปกติ",
                                folder = "ไฟล์ที่อัปโหลด",
                                time = System.currentTimeMillis(),
                                audioUri = uri.toString(),
                                audioSource = AudioSource.UPLOADED,
                                jobId = jobId,
                                transcription = TranscriptionStatus.QUEUED,
                            )
                            val updated = listOf(uploaded) + notes
                            val saved = if (storageError) null else runCatching { storage.savePreservingTranscriptions(updated) }.getOrNull()
                            val noteToTranscribe = saved?.firstOrNull { it.jobId == jobId }
                            if (saved != null) notes = saved
                            if (noteToTranscribe == null) {
                                Toast.makeText(this@MainActivity, "ไม่สามารถบันทึกไฟล์ลงในโน้ตได้", Toast.LENGTH_LONG).show()
                            } else {
                                enqueueTranscription(noteToTranscribe)
                                Toast.makeText(this@MainActivity, "กำลังถอดข้อความในเบื้องหลัง", Toast.LENGTH_SHORT).show()
                                playbackUri = uri
                            }
                        }
                    }
                }
            }
            val retryTranscription: (Note) -> Unit = { target ->
                if (target.transcription == TranscriptionStatus.FAILED) {
                    val oldJob = target.jobId
                    val jobId = UUID.randomUUID().toString()
                    val queued = target.copy(jobId = jobId, transcription = TranscriptionStatus.QUEUED, transcriptionError = "")
                    if (updateNote(target, queued)) {
                        notes.firstOrNull { it.jobId == jobId && it.transcription.isPending() }?.let(enqueueTranscription)
                        if (oldJob.isNotBlank() && oldJob != jobId) WorkManager.getInstance(this@MainActivity).cancelUniqueWork(oldJob)
                    }
                }
            }
            val deleteNote: (Note) -> Unit = { target ->
                val index = notes.indexOfNoteInstance(target)
                if (index >= 0) {
                    val updated = notes.toMutableList().apply { removeAt(index) }
                    val saved = if (storageError) null else runCatching { storage.savePreservingTranscriptions(updated) }.getOrNull()
                    if (saved != null) {
                        notes = saved
                        if (target.jobId.isNotBlank()) WorkManager.getInstance(this@MainActivity).cancelUniqueWork(target.jobId)
                        if (target.audioUri.isNotBlank() && saved.none { it.audioUri == target.audioUri }) {
                            target.audioUri.toUri().takeIf { it.scheme == "file" }?.path?.let { path ->
                                runCatching {
                                    val file = File(path)
                                    if (isManagedRecording(file, filesDir)) file.delete()
                                }
                            }
                        }
                        if (selected === target) selected = null
                    } else {
                        Toast.makeText(this@MainActivity, "ไม่สามารถลบโน้ตได้ กรุณาลองอีกครั้ง", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "ไม่พบโน้ตที่ต้องการลบ", Toast.LENGTH_LONG).show()
                }
            }
            LaunchedEffect(notes.any { it.transcription.isPending() }) {
                while (notes.any { it.transcription.isPending() }) {
                    delay(2.seconds)
                    val stored = try {
                        storage.load()
                    } catch (_: Exception) {
                        storageError = true
                        null
                    }
                    if (stored == null) break
                    val storedByJob = stored.filter { it.jobId.isNotBlank() }.associateBy { it.jobId }
                    val previous = notes
                    var changed = false
                    val merged = previous.map { note ->
                        if (!note.transcription.isPending()) note
                        else storedByJob[note.jobId]?.takeIf { latest ->
                            latest.transcription != note.transcription || latest.body != note.body || latest.transcriptionError != note.transcriptionError
                        }?.let { latest ->
                            changed = true
                            note.copy(body = latest.body, transcription = latest.transcription, transcriptionError = latest.transcriptionError)
                        } ?: note
                    }
                    if (changed) {
                        val selectedIndex = selected?.let { previous.indexOfNoteInstance(it) } ?: -1
                        notes = merged
                        if (selectedIndex >= 0) selected = merged.getOrNull(selectedIndex)
                    }
                }
            }
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryCoral,
                selectedTextColor = PrimaryCoral,
                unselectedIconColor = TextPrimary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color.Transparent,
            )

            Cs265FinalTheme {
                Scaffold(
                    containerColor = BackgroundCream,
                    bottomBar = {
                        if (!editing && selected == null && playbackUri == null) {
                            Column {
                                HorizontalDivider(color = BorderSoft, thickness = 1.dp)
                                NavigationBar(containerColor = SurfaceWhite, tonalElevation = 0.dp) {
                                    listOf("หน้าหลัก", "คลังโน้ต", "บันทึกรายวัน", "ค้นหา").forEach { tab ->
                                        val active = page == tab
                                        NavigationBarItem(
                                            selected = active,
                                            onClick = { page = tab },
                                            colors = navColors,
                                            icon = {
                                                val tint = if (active) PrimaryCoral else TextPrimary
                                                when (tab) {
                                                    "หน้าหลัก" -> HomeNavIcon(selected = active, color = tint, modifier = Modifier.size(22.dp))
                                                    "คลังโน้ต" -> FolderNavIcon(selected = active, color = tint, modifier = Modifier.size(22.dp))
                                                    "บันทึกรายวัน" -> CalendarNavIcon(selected = active, color = tint, modifier = Modifier.size(22.dp))
                                                    else -> SearchIcon(color = tint, modifier = Modifier.size(22.dp))
                                                }
                                            },
                                            label = { Text(tab, style = MaterialTheme.typography.labelSmall) },
                                        )
                                    }
                                }
                            }
                        }
                    },
                ) { padding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (storageError) {
                            Text(
                                text = "ไม่สามารถอ่านโน้ตที่บันทึกไว้ได้ ข้อมูลเดิมจะไม่ถูกเขียนทับ",
                                color = PrimaryCoral,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                        if (playbackUri != null) {
                            PlaybackScreen(uri = requireNotNull(playbackUri), onBack = { playbackUri = null })
                        } else if (selected != null) {
                            val n = selected!!
                            NoteDetailScreen(
                                note = n,
                                dateFormat = dateFormat,
                                onRename = { newTitle -> updateNote(n, n.copy(title = newTitle)) },
                                onListenOriginal = { audioPicker.launch(uploadMimeTypes) },
                                onDelete = { deleteNote(n) },
                                onRetryTranscription = { retryTranscription(n) },
                                onBack = { selected = null },
                            )
                        } else if (editing) {
                            RecordingScreen(
                                title = title,
                                onTitleChange = { title = it },
                                transcript = transcript,
                                onTranscriptChange = { value ->
                                    if (listening) cancelSpeech()
                                    transcript = value
                                },
                                listening = listening,
                                mood = mood,
                                onMoodChange = { mood = it },
                                folder = folder,
                                onFolderChange = { folder = it },
                                message = message,
                                onToggleMic = {
                                    if (listening) stopSpeech()
                                    else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startSpeech()
                                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                onSave = {
                                    when {
                                        listening -> message = "กรุณาหยุดรับเสียงและรอข้อความก่อนบันทึก"
                                        transcript.isBlank() -> message = "เพิ่มข้อความก่อนบันทึก"
                                        else -> {
                                            stopAudioCapture()
                                            flushSessionAudio()
                                            val now = System.currentTimeMillis()
                                            val pcm = speechAudioBuffer?.let { synchronized(it) { it.toByteArray() } }?.takeIf { hasNonZeroPcm(it) }
                                            var audioFile: File? = null
                                            val audioSaved = pcm == null || runCatching {
                                                audioFile = File.createTempFile("note_", ".mp3", filesDir)
                                                writeMp3File(requireNotNull(audioFile), pcm)
                                            }.isSuccess
                                            if (!audioSaved) {
                                                audioFile?.let { runCatching { it.delete() } }
                                                message = "ไม่สามารถบันทึกไฟล์เสียงได้ กรุณาลองอีกครั้ง"
                                            } else {
                                                val audioUri = audioFile?.toURI()?.toString().orEmpty()
                                                val source = if (audioUri.isNotEmpty()) AudioSource.RECORDED else AudioSource.UNKNOWN
                                                val updated = listOf(Note(title.ifBlank { "โน้ตของฉัน" }, transcript, mood, folder.ifBlank { "ทั่วไป" }, now, audioUri, source)) + notes
                                                val saved = if (storageError) null else runCatching { storage.savePreservingTranscriptions(updated) }.getOrNull()
                                                if (saved != null) {
                                                    notes = saved
                                                    cancelSpeech()
                                                    speechAudioBuffer = null
                                                    transcript = ""
                                                    title = ""
                                                    folder = ""
                                                    editing = false
                                                } else {
                                                    audioFile?.let { runCatching { it.delete() } }
                                                    message = "ไม่สามารถบันทึกโน้ตได้ กรุณาลองอีกครั้ง"
                                                }
                                            }
                                        }
                                    }
                                },
                                onCancel = { cancelSpeech(); speechAudioBuffer = null; editing = false },
                            )
                        } else {
                            when (page) {
                                "หน้าหลัก" -> HomeScreen(
                                    notes = notes,
                                    dateFormat = dateFormat,
                                    onNew = startComposer,
                                    onSelect = { selected = it },
                                    onUpload = { audioPicker.launch(uploadMimeTypes) },
                                    onViewAll = { page = "คลังโน้ต" },
                                    onDelete = deleteNote,
                                )
                                "คลังโน้ต" -> LibraryScreen(
                                    notes = notes,
                                    dateFormat = dateFormat,
                                    onSelect = { selected = it },
                                    onSearchClick = { page = "ค้นหา" },
                                    onDelete = deleteNote,
                                )
                                "บันทึกรายวัน" -> CalendarScreen(
                                    notes = notes,
                                    dateFormat = dateFormat,
                                    date = date,
                                    onDateChange = { date = it },
                                    onSelect = { selected = it },
                                    onNew = startComposer,
                                    onDelete = deleteNote,
                                )
                                else -> Column(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text("ค้นหา", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                                    OutlinedTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        label = { Text("ค้นหาชื่อหรือข้อความ") },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    notes.filterNotes(query = query).forEach { note -> NoteCard(note, dateFormat, onDelete = deleteNote) { selected = it } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
