package com.example.cs265final

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import com.example.cs265final.data.filterNotes
import com.example.cs265final.data.indexOfNoteInstance
import com.example.cs265final.feature.calendar.CalendarScreen
import com.example.cs265final.feature.home.HomeScreen
import com.example.cs265final.feature.notes.LibraryScreen
import com.example.cs265final.feature.notes.NoteCard
import com.example.cs265final.feature.notes.NoteDetailScreen
import com.example.cs265final.feature.recording.RecordingScreen
import com.example.cs265final.feature.playback.PlaybackScreen
import com.example.cs265final.speech.SpeechDraft
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
import java.util.concurrent.atomic.AtomicBoolean

internal fun writeWavFile(target: File, pcm: ByteArray, sampleRate: Int = 16_000): File = target.apply {
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray(Charsets.US_ASCII))
        putInt(36 + pcm.size)
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

internal fun isManagedRecording(file: File, directory: File): Boolean {
    val canonical = file.canonicalFile
    return canonical.parentFile == directory.canonicalFile && canonical.name.startsWith("note_") && canonical.name.endsWith(".wav")
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
    private var audioPipeRead: ParcelFileDescriptor? = null
    private var audioPipeWrite: ParcelFileDescriptor? = null

    @SuppressLint("MissingPermission")
    private fun startAudioCapture(): ParcelFileDescriptor? {
        stopAudioCapture()
        if (Build.VERSION.SDK_INT < 33) return null
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
        val buffer = speechAudioBuffer
        Thread({
            val chunk = ByteArray(3_200)
            var pipeOpen = true
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(writeFd).use { out ->
                    while (active.get()) {
                        val n = rec.read(chunk, 0, chunk.size)
                        if (n < 0) break
                        if (n > 0) {
                            buffer?.let { synchronized(it) { it.write(chunk, 0, n) } }
                            if (pipeOpen) {
                                runCatching {
                                    out.write(chunk, 0, n)
                                    out.flush()
                                }.onFailure { pipeOpen = false }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Stopping a capture can close the pipe while the reader is active.
            } finally {
                runCatching { rec.stop() }
                runCatching { rec.release() }
            }
        }, "Voice2NoteMicTee").apply { isDaemon = true; start() }
        return readFd
    }

    private fun stopAudioCapture() {
        audioRecordActive?.set(false)
        audioRecordActive = null
        audioRecord?.let { runCatching { it.stop() } }
        audioRecord = null
        runCatching { audioPipeWrite?.close() }
        audioPipeWrite = null
        runCatching { audioPipeRead?.close() }
        audioPipeRead = null
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
        try {
            recognizer?.stopListening()
            currentSpeechSession?.let(::armSpeechTimeout)
        } catch (_: RuntimeException) {
            cancelSpeech()
            message = "ไม่สามารถหยุดรับเสียงได้ ข้อความที่ได้ยังอยู่"
        }
    }

    private var currentSpeechSession: Int? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeech()
        else message = if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            "กรุณาอนุญาตไมโครโฟนเพื่อใช้การบันทึกเสียง"
        } else {
            "เปิดสิทธิ์ไมโครโฟนในการตั้งค่าแอปเพื่อใช้การบันทึกเสียง"
        }
    }
    private fun startSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { message = "อุปกรณ์นี้ไม่มีบริการแปลงเสียงเป็นข้อความ"; return }
        recognizer?.destroy()
        val session = speechDraft.start(transcript)
        currentSpeechSession = session
        speechFinishing = false
        if (speechAudioBuffer == null) speechAudioBuffer = ByteArrayOutputStream()
        val readPipe = runCatching { startAudioCapture() }.getOrElse { stopAudioCapture(); null }
        armSpeechTimeout(session)
        listening = true
        message = ""
        try {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { speech ->
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { if (speechDraft.isCurrent(session)) { listening = true; if (!speechFinishing) clearSpeechTimeout() } }
                override fun onBeginningOfSpeech() { if (speechDraft.isCurrent(session) && !speechFinishing) clearSpeechTimeout() }
                override fun onRmsChanged(rmsdB: Float) {}
                // ponytail: recognizer feedback buffers have an unknown sample rate and may be incomplete.
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { if (speechDraft.isCurrent(session)) { speechFinishing = true; stopAudioCapture(); armSpeechTimeout(session) } }
                override fun onError(error: Int) { if (speechDraft.isCurrent(session)) { stopAudioCapture(); Log.w("Voice2NoteSpeech", "Recognition error code: $error"); listening = false; clearSpeechTimeout(); currentSpeechSession = null; speechDraft.cancel(); message = "ไม่สามารถรับเสียงได้ ลองอีกครั้งหรือพิมพ์ข้อความแทน" } }
                override fun onResults(results: Bundle?) { if (speechDraft.isCurrent(session)) { stopAudioCapture(); listening = false; clearSpeechTimeout(); currentSpeechSession = null; updateText(results); speechDraft.cancel() } }
                override fun onPartialResults(partialResults: Bundle?) { updateText(partialResults) }
                override fun onEvent(eventType: Int, params: Bundle?) {}
                private fun updateText(bundle: Bundle?) { if (speechDraft.isCurrent(session)) bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { speechDraft.result(session, it)?.let { value -> transcript = value } } }
            })
            speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                if (readPipe != null && Build.VERSION.SDK_INT >= 33) {
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
    private fun cancelSpeech() { stopAudioCapture(); clearSpeechTimeout(); currentSpeechSession = null; speechFinishing = false; speechDraft.cancel(); recognizer?.cancel(); recognizer?.destroy(); recognizer = null; listening = false }
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
                    val updated = notes.toMutableList().apply { this[index] = replacement }
                    runCatching { storage.save(updated) }.getOrDefault(false).also { saved ->
                        if (saved) {
                            notes = updated
                            if (selected === current) selected = replacement
                        }
                    }
                }
            }
            val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    playbackUri = uri
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
                        val saved = if (current != null) {
                            updateNote(current, current.copy(audioUri = uri.toString(), audioSource = AudioSource.UPLOADED))
                        } else {
                            val uploaded = Note(
                                title = "ไฟล์เสียงที่อัปโหลด",
                                body = "ไฟล์เสียงที่อัปโหลด",
                                mood = "ปกติ",
                                folder = "ไฟล์ที่อัปโหลด",
                                time = System.currentTimeMillis(),
                                audioUri = uri.toString(),
                                audioSource = AudioSource.UPLOADED,
                            )
                            val updated = listOf(uploaded) + notes
                            (!storageError && runCatching { storage.save(updated) }.getOrDefault(false)).also {
                                if (it) notes = updated
                            }
                        }
                        if (!saved) {
                            Toast.makeText(this@MainActivity, "ไม่สามารถบันทึกไฟล์ลงในโน้ตได้", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            val deleteNote: (Note) -> Unit = { target ->
                val index = notes.indexOfNoteInstance(target)
                if (index >= 0) {
                    val updated = notes.toMutableList().apply { removeAt(index) }
                    if (!storageError && runCatching { storage.save(updated) }.getOrDefault(false)) {
                        if (target.audioUri.isNotBlank() && updated.none { it.audioUri == target.audioUri }) {
                            Uri.parse(target.audioUri).takeIf { it.scheme == "file" }?.path?.let { path ->
                                runCatching {
                                    val file = File(path)
                                    if (isManagedRecording(file, filesDir)) file.delete()
                                }
                            }
                        }
                        notes = updated
                        if (selected === target) selected = null
                    } else {
                        Toast.makeText(this@MainActivity, "ไม่สามารถลบโน้ตได้ กรุณาลองอีกครั้ง", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "ไม่พบโน้ตที่ต้องการลบ", Toast.LENGTH_LONG).show()
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
                                                    "หน้าหลัก" -> HomeNavIcon(selected = active, color = tint)
                                                    "คลังโน้ต" -> FolderNavIcon(selected = active, color = tint)
                                                    "บันทึกรายวัน" -> CalendarNavIcon(selected = active, color = tint)
                                                    else -> SearchIcon(color = tint)
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
                                onListenOriginal = { audioPicker.launch(arrayOf("audio/*")) },
                                onDelete = { deleteNote(n) },
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
                                            val now = System.currentTimeMillis()
                                            val pcm = speechAudioBuffer?.let { synchronized(it) { it.toByteArray() } }?.takeIf { it.isNotEmpty() }
                                            var audioFile: File? = null
                                            val audioSaved = pcm == null || runCatching {
                                                audioFile = File.createTempFile("note_", ".wav", filesDir)
                                                writeWavFile(requireNotNull(audioFile), pcm)
                                            }.isSuccess
                                            if (!audioSaved) {
                                                audioFile?.let { runCatching { it.delete() } }
                                                message = "ไม่สามารถบันทึกไฟล์เสียงได้ กรุณาลองอีกครั้ง"
                                            } else {
                                                val audioUri = audioFile?.toURI()?.toString().orEmpty()
                                                val source = if (audioUri.isNotEmpty()) AudioSource.RECORDED else AudioSource.UNKNOWN
                                                val updated = listOf(Note(title.ifBlank { "โน้ตของฉัน" }, transcript, mood, folder.ifBlank { "ทั่วไป" }, now, audioUri, source)) + notes
                                                if (!storageError && runCatching { storage.save(updated) }.getOrDefault(false)) {
                                                    notes = updated
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
                                    onUpload = { audioPicker.launch(arrayOf("audio/*")) },
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
                                    notes.filterNotes(query = query).forEach { NoteCard(it, dateFormat, onDelete = deleteNote) { selected = it } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
