package com.example.cs265final

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import com.example.cs265final.data.Note
import com.example.cs265final.data.NoteStorage
import com.example.cs265final.data.filterNotes
import com.example.cs265final.feature.calendar.CalendarScreen
import com.example.cs265final.feature.home.HomeScreen
import com.example.cs265final.feature.notes.LibraryScreen
import com.example.cs265final.feature.notes.NoteCard
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val rose = PrimaryCoral

class MainActivity : ComponentActivity() {
    private var playbackUri by mutableStateOf<Uri?>(null)
    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) playbackUri = uri
    }
    private var recognizer: SpeechRecognizer? = null
    private var transcript by mutableStateOf("")
    private var listening by mutableStateOf(false)
    private var message by mutableStateOf("")
    private val speechDraft = SpeechDraft()
    private val speechHandler = Handler(Looper.getMainLooper())
    private var speechTimeout: Runnable? = null
    private var speechFinishing = false

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
        if (granted) startSpeech() else message = "กรุณาอนุญาตไมโครโฟนเพื่อใช้การบันทึกเสียง"
    }
    private fun startSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { message = "อุปกรณ์นี้ไม่มีบริการแปลงเสียงเป็นข้อความ"; return }
        recognizer?.destroy()
        val session = speechDraft.start(transcript)
        currentSpeechSession = session
        speechFinishing = false
        armSpeechTimeout(session)
        listening = true
        message = ""
        try {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { speech ->
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { if (speechDraft.isCurrent(session)) { listening = true; if (!speechFinishing) clearSpeechTimeout() } }
                override fun onBeginningOfSpeech() { if (speechDraft.isCurrent(session) && !speechFinishing) clearSpeechTimeout() }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { if (speechDraft.isCurrent(session)) { speechFinishing = true; armSpeechTimeout(session) } }
                override fun onError(error: Int) { if (speechDraft.isCurrent(session)) { Log.w("Voice2NoteSpeech", "Recognition error code: $error"); listening = false; clearSpeechTimeout(); currentSpeechSession = null; speechDraft.cancel(); message = "ไม่สามารถรับเสียงได้ ลองอีกครั้งหรือพิมพ์ข้อความแทน" } }
                override fun onResults(results: Bundle?) { if (speechDraft.isCurrent(session)) { listening = false; clearSpeechTimeout(); currentSpeechSession = null; updateText(results); speechDraft.cancel() } }
                override fun onPartialResults(partialResults: Bundle?) { updateText(partialResults) }
                override fun onEvent(eventType: Int, params: Bundle?) {}
                private fun updateText(bundle: Bundle?) { if (speechDraft.isCurrent(session)) bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { speechDraft.result(session, it)?.let { value -> transcript = value } } }
            })
            speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        }
        } catch (_: RuntimeException) {
            cancelSpeech()
            message = "ไม่สามารถเริ่มรับเสียงได้ กรุณาตรวจสอบสิทธิ์ไมโครโฟนและบริการเสียง"
        }
    }
    private fun cancelSpeech() { clearSpeechTimeout(); currentSpeechSession = null; speechFinishing = false; speechDraft.cancel(); recognizer?.cancel(); recognizer?.destroy(); recognizer = null; listening = false }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("draft_transcript", transcript)
        super.onSaveInstanceState(outState)
    }
    override fun onStop() { cancelSpeech(); super.onStop() }
    override fun onDestroy() { recognizer?.destroy(); super.onDestroy() }
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
            var renaming by remember { mutableStateOf(false) }
            var renameTitle by remember { mutableStateOf("") }
            var renameError by remember { mutableStateOf("") }
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("th-TH")) }
            var date by remember { mutableStateOf(dateFormat.format(Date())) }
            val startComposer = {
                transcript = ""
                title = ""
                folder = ""
                mood = "ปกติ"
                message = ""
                editing = true
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
                                color = rose,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                        if (playbackUri != null) {
                            PlaybackScreen(uri = requireNotNull(playbackUri), onBack = { playbackUri = null })
                        } else if (selected != null) {
                            val n = selected!!
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                TextButton(onClick = { selected = null; renaming = false; renameError = "" }) { Text("‹ กลับ") }
                                if (renaming) {
                                    OutlinedTextField(
                                        value = renameTitle,
                                        onValueChange = { renameTitle = it; renameError = "" },
                                        label = { Text("ชื่อโน้ต") },
                                        singleLine = true,
                                        isError = renameError.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (renameError.isNotEmpty()) Text(renameError, color = MaterialTheme.colorScheme.error)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            val newTitle = renameTitle.trim()
                                            val index = notes.indexOfFirst { it === n }
                                            when {
                                                newTitle.isEmpty() -> renameError = "กรุณาใส่ชื่อโน้ต"
                                                index < 0 -> renameError = "ไม่พบโน้ต กรุณาลองอีกครั้ง"
                                                else -> {
                                                    val renamed = n.copy(title = newTitle)
                                                    val updated = notes.toMutableList().apply { this[index] = renamed }
                                                    if (!storageError && runCatching { storage.save(updated) }.getOrDefault(false)) {
                                                        notes = updated
                                                        selected = renamed
                                                        renaming = false
                                                    } else {
                                                        renameError = "ไม่สามารถบันทึกชื่อโน้ตได้ กรุณาลองอีกครั้ง"
                                                    }
                                                }
                                            }
                                        }) { Text("บันทึกชื่อ") }
                                        TextButton(onClick = { renaming = false; renameError = "" }) { Text("ยกเลิก") }
                                    }
                                } else {
                                    Text(n.title, style = MaterialTheme.typography.headlineMedium)
                                    TextButton(onClick = { renameTitle = n.title; renameError = ""; renaming = true }) { Text("เปลี่ยนชื่อโน้ต") }
                                }
                                Text("${dateFormat.format(Date(n.time))} · ${n.mood} · ${n.folder}", color = rose)
                                Text(n.body, style = MaterialTheme.typography.bodyLarge)
                            }
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
                                moodOptions = listOf("ดีมาก", "ดี", "ปกติ", "เครียด", "เศร้า"),
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
                                            val updated = listOf(Note(title.ifBlank { "โน้ตของฉัน" }, transcript, mood, folder.ifBlank { "ทั่วไป" }, System.currentTimeMillis())) + notes
                                            if (!storageError && runCatching { storage.save(updated) }.getOrDefault(false)) {
                                                notes = updated
                                                cancelSpeech()
                                                transcript = ""
                                                title = ""
                                                folder = ""
                                                editing = false
                                            } else {
                                                message = "ไม่สามารถบันทึกโน้ตได้ กรุณาลองอีกครั้ง"
                                            }
                                        }
                                    }
                                },
                                onCancel = { cancelSpeech(); editing = false },
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
                                )
                                "คลังโน้ต" -> LibraryScreen(
                                    notes = notes,
                                    dateFormat = dateFormat,
                                    onSelect = { selected = it },
                                    onSearchClick = { page = "ค้นหา" },
                                )
                                "บันทึกรายวัน" -> CalendarScreen(
                                    notes = notes,
                                    dateFormat = dateFormat,
                                    date = date,
                                    onDateChange = { date = it },
                                    onSelect = { selected = it },
                                    onNew = startComposer,
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
                                    notes.filterNotes(query = query).forEach { NoteCard(it, dateFormat) { selected = it } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
