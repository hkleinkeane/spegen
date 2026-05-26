/*
 * Copyright 2026 Harper Klein Keane
 *
 * This file is part of SpeGen.
 *
 * SpeGen is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * SpeGen is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SpeGen. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.hkleinkeane.spegen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val DEMO_MODE = false

// Shared secret which is used for calling for an access token
const val CLIENT_SECRET = "d65234627cc790cba662f6b3"

// Access token that is used for calling to the API
var accesstoken = ""

var name = ""

// Screen height and width variables as determined by GetScreenDimensions()
var screenHeight = 0.dp
var screenWidth = 0.dp

// Is the device in landscape?
var isLandscape = false

var static_row_height = 0.dp

var menu_static_row_height = 0.dp

var button_boxes_width = 0.dp

val editor_mode = mutableStateOf(false)
val show_edit_item_dialog = mutableStateOf(false)
val show_add_item_dialog = mutableStateOf(false)
val show_new_menu_dialog = mutableStateOf(false)
val edit_target_menu_id = mutableIntStateOf(-1)
val edit_target_index = mutableIntStateOf(-1)

val home = menutemplate(
    0, "Home",
    listOf("I", "want", "more", "help", "yes", "no", "stop", "please", "People", "Actions", "Food", "Feelings"),
    listOf(null, null, null, null, null, null, null, null, 1, 2, 3, 4),
    listOf(2, 2, 2, 2, 2, 2, 2, 2, null, null, null, null),
    listOf(true, true, true, true, true, true, true, true, false, false, false, false)
)

val people = menutemplate(
    1, "People",
    listOf("you", "me", "mom", "dad", "sister", "brother", "friend", "teacher"),
    listOf(null, null, null, null, null, null, null, null),
    listOf(2, 2, 2, 2, 2, 2, 2, 2),
    listOf(true, true, true, true, true, true, true, true)
)

val actions = menutemplate(
    2, "Actions",
    listOf("eat", "drink", "play", "go", "sleep", "read", "watch", "listen"),
    listOf(null, null, null, null, null, null, null, null),
    listOf(2, 2, 2, 2, 2, 2, 2, 2),
    listOf(true, true, true, true, true, true, true, true)
)

val food = menutemplate(
    3, "Food",
    listOf("water", "milk", "apple", "banana", "sandwich", "pizza", "cookie", "snack"),
    listOf(null, null, null, null, null, null, null, null),
    listOf(2, 2, 2, 2, 2, 2, 2, 2),
    listOf(true, true, true, true, true, true, true, true)
)

val feelings = menutemplate(
    4, "Feelings",
    listOf("happy", "sad", "tired", "sick", "hungry", "thirsty", "scared", "excited"),
    listOf(null, null, null, null, null, null, null, null),
    listOf(2, 2, 2, 2, 2, 2, 2, 2),
    listOf(true, true, true, true, true, true, true, true)
)

var MenuList = mutableStateListOf<menutemplate>(home, people, actions, food, feelings)

var box_size = 100.dp

var box_padding = 20.dp

var menu_height = 0.dp

var menu_width = screenWidth - (button_boxes_width * 2)

var inputboxselecteditems_text = mutableStateListOf<String>()

var inputboxselecteditems_has_symbol = mutableStateListOf<Boolean>()

// Parallel lists for audio path and pronunciation override per input-box token
var inputboxselecteditems_audio = mutableStateListOf<String>()
var inputboxselecteditems_pron = mutableStateListOf<String>()

var ttsEngine: MutableState<TtsEngine?> = mutableStateOf(null)

// Bigram autocomplete model – persisted so it improves over time
var ngram_model = mutableStateOf(seedNgramModel())

// Controls whether AutocompleteMenu replaces the symbol grid
var show_autocomplete = mutableStateOf(false)

var wordfinder_display = mutableIntStateOf(0)

var wordfinder_display_buttonguide = mutableIntStateOf(0)

var switchmenuparser = mutableStateOf(0)

var linked_menu = mutableStateOf(0)

var menukeylist = mutableListOf<Int>()

var wordfinder_path_ids = mutableStateListOf<Int>()

var wordfinder_path_names = mutableStateListOf<String>()

var createclonefolder = mutableStateOf(false)

var createclonesymbol = mutableStateOf(false)

var wordfinder_target_is_symbol = false

// Text padding for folder/symbol names. Minimum should be 20 dp or else issues will occur
var item_text_padding = 20.dp

val item_positions = mutableStateMapOf<String, Offset>()

var tts_data_found = mutableStateOf(false)

var current_menu_id = 0

val wordfinder_highlight_index = mutableIntStateOf(-1)

var input_box_height = 0.dp

var static_terms = mutableStateListOf<String>("Yes", "No", "Thank you", "I need help", "Excuse me", "I use a talker to communicate")

val show_settings = mutableStateOf(false)

var menu_terms_ids = mutableStateListOf(0, 1, 2, 3, 4)

var trigger_save = mutableStateOf(false)

var trigger_load = mutableStateOf(false)

var trigger_state_change_check = mutableStateOf(false)

var state_has_changed = mutableStateOf(false)

val show_tutorial = mutableStateOf(false)

var tts_speech_rate = mutableStateOf(1.0f)
var tts_pitch = mutableStateOf(1.0f)

var tts_pause_between_words = mutableStateOf(false)
var tts_pause_duration = mutableLongStateOf(500L)

var screen_display = mutableStateOf(true)

val static_row_text_size = mutableFloatStateOf(16f)
val static_row_text_padding = mutableFloatStateOf(8f)
val menu_row_text_size = mutableFloatStateOf(16f)
val menu_row_text_padding = mutableFloatStateOf(8f)

val cache_progress = mutableStateOf(0)
val cache_total = mutableStateOf(0)
val cache_running = mutableStateOf(false)

val show_cache_prompt = mutableStateOf(false)
val show_delete_menu_dialog = mutableStateOf(false)
val show_goto_menu_dialog = mutableStateOf(false)
val menu_history = mutableStateListOf<Int>()

internal val httpClient = HttpClient()

@Composable
fun App() {
    ttsEngine = rememberTtsEngine()
    val platformContext = LocalPlatformContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val isReturningUser = loadAllPreferences()
        if (!isReturningUser) show_tutorial.value = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                scope.launch { saveAllPreferences() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MenuKeyGen()
    Screen()
    Box {
        if (createclonefolder.value) {
            if (wordfinder_path_ids.isNotEmpty() && wordfinder_path_names.isNotEmpty()) {
                var index = 0
                for (i in 0 until (MenuFinder(wordfinder_path_ids[0]).item_list.size)) {
                    if (wordfinder_path_names.size > 1) {
                        if (MenuFinder(wordfinder_path_ids[0]).item_list[i] == wordfinder_path_names[1] && !MenuFinder(
                                wordfinder_path_ids[0]
                            ).item_type[i]
                        ) {
                            index = i
                        }
                    } else {
                        if (MenuFinder(wordfinder_path_ids[0]).item_list[i] == wordfinder_path_names[0] && !MenuFinder(
                                wordfinder_path_ids[0]
                            ).item_type[i]
                        ) {
                            index = i
                        }
                    }
                }
                wordfinder_highlight_index.intValue = index
                if (!MenuFinder(wordfinder_path_ids[0]).item_type[index]) {
                    var total_box_size = box_size + (box_padding * 2)
                    val itemKey = "${wordfinder_path_ids[0]}-$index"
                    val captured = item_positions[itemKey]
                    val density = LocalDensity.current
                    val x_offset = captured?.let { with(density) { it.x.toDp() } } ?: 0.dp
                    val y_offset = captured?.let { with(density) { it.y.toDp() } }
                        ?: (button_boxes_width * 2)
                    val menu = MenuFinder(wordfinder_path_ids[0])
                    val folder_name = menu.item_list[index]
                    var folder_image_url by remember { mutableStateOf("") }
                    val folder_menu = menu.pointers[index]
                    val vertical_stretch =
                        ((menu_height) - ((((menu_height) / (total_box_size)).toInt()) * total_box_size))
                    LaunchedEffect(folder_name) {
                        val res = useApiWithToken(accesstoken, folder_name)
                        folder_image_url = res?.image_url ?: ""
                    }
                    Surface(color = Color.Transparent) {
                        Folder(
                            folder_name,
                            folder_image_url,
                            folder_menu!!,
                            vertical_stretch,
                            x_offset,
                            y_offset,
                            Modifier.zIndex(1000f)
                        )
                    }
                }
            }
        }
        if (createclonesymbol.value) {
            var index = 0
            val lookupName = if (wordfinder_path_names.size > 1)
                wordfinder_path_names[1]
            else
                wordfinder_path_names[0]
            for (i in 0 until (MenuFinder(wordfinder_path_ids[0]).item_list.size)) {
                if (MenuFinder(wordfinder_path_ids[0]).item_list[i] == lookupName && MenuFinder(
                        wordfinder_path_ids[0]
                    ).item_type[i]
                ) {
                    index = i
                }
            }
            wordfinder_highlight_index.intValue = index
            if (MenuFinder(wordfinder_path_ids[0]).item_type[index]) {
                var total_box_size = box_size + (box_padding * 2)
                val itemKey = "${wordfinder_path_ids[0]}-$index"
                val captured = item_positions[itemKey]
                val density = LocalDensity.current
                val x_offset = captured?.let { with(density) { it.x.toDp() } } ?: 0.dp
                val y_offset = captured?.let { with(density) { it.y.toDp() } }
                    ?: (button_boxes_width * 2)
                val menu = MenuFinder(wordfinder_path_ids[0])
                val symbol_name = menu.item_list[index]
                var symbol_image_url by remember { mutableStateOf("") }
                val vertical_stretch =
                    ((menu_height) - ((((menu_height) / (total_box_size)).toInt()) * total_box_size))
                val tts_type = menu.tts[index]!!
                LaunchedEffect(symbol_name) {
                    val res = useApiWithToken(accesstoken, symbol_name)
                    symbol_image_url = res?.image_url ?: ""
                }
                Surface(color = Color.Transparent) {
                    Symbol(
                        symbol_name,
                        symbol_image_url,
                        vertical_stretch,
                        tts_type,
                        x_offset,
                        y_offset,
                        Modifier.zIndex(100f)
                    )
                }
            }
        }
    }
    if (show_tutorial.value) {
        TutorialOverlay(onFinish = {
            show_tutorial.value = false
            trigger_save.value = true
        })
    }
    if (show_settings.value) {
        SettingsScreen(onClose = { show_settings.value = false })
    }
    if (editor_mode.value) {
        if (show_edit_item_dialog.value) EditItemDialog()
        if (show_add_item_dialog.value) AddItemDialog()
        if (show_new_menu_dialog.value) NewMenuDialog()
        if (show_delete_menu_dialog.value) DeleteMenuDialog()
        if (show_goto_menu_dialog.value) GotoMenuDialogue()
    }
    LaunchedEffect(Unit) {
        show_cache_prompt.value =
            !show_tutorial.value && isOnline() && hasUncachedImages(platformContext)
    }
    if (show_cache_prompt.value) {
        CachePrompt()
    }
    LaunchedEffect(trigger_save.value) {
        if (trigger_save.value) {
            try { saveAllPreferences() }
            finally { cleanOrphanedCustomImages(); trigger_save.value = false }
        }
    }
    LaunchedEffect(trigger_load.value) {
        if (trigger_load.value) {
            try { loadAllPreferences() }
            finally {
                cleanOrphanedCustomImages()
                switchmenuparser.value++
                trigger_load.value = false
            }
        }
    }
    LaunchedEffect(trigger_state_change_check.value) {
        if (trigger_state_change_check.value) {
            state_has_changed.value = hasStateChanged()
            trigger_state_change_check.value = false
        }
    }
}

fun resetToDefaults() {
    // Clear composed sentence
    inputboxselecteditems_text.clear()
    inputboxselecteditems_has_symbol.clear()
    inputboxselecteditems_audio.clear()
    inputboxselecteditems_pron.clear()
    show_autocomplete.value = false

    // Clear all wordfinder state
    wordfinder_display.intValue = 0
    wordfinder_display_buttonguide.intValue = 0
    wordfinder_path_ids.clear()
    wordfinder_path_names.clear()
    createclonefolder.value = false
    createclonesymbol.value = false
    wordfinder_highlight_index.intValue = -1

    // Clear menu history
    menu_history.clear()

    // Reset menus to defaults
    MenuList.clear()
    MenuList.addAll(listOf(home, people, actions, food, feelings))

    // Reset static row
    static_terms.clear()
    static_terms.addAll(listOf("Yes", "No", "Thank you", "I need help", "Excuse me", "I use a talker to communicate"))

    // Reset menu row
    menu_terms_ids.clear()
    menu_terms_ids.addAll(listOf(0, 1, 2, 3, 4))

    // Close any open overlays
    editor_mode.value = false
    show_settings.value = false
    show_edit_item_dialog.value = false
    show_add_item_dialog.value = false
    show_new_menu_dialog.value = false

    // Back to home menu
    linked_menu.value = 0
    switchmenuparser.value++

    // Reset tutorial
    show_tutorial.value = true

    // Reset TTS settings
    tts_pitch.value = 1.0f
    tts_speech_rate.value = 1.0f
    tts_pause_between_words.value = false
    tts_pause_duration.longValue = 500L

    // Reset ngram model
    ngram_model.value = seedNgramModel()

    // Refresh screen
    screen_display.value = false

    // Reset row variables
    static_row_text_size.floatValue = 16f
    static_row_text_padding.floatValue = 4f
    menu_row_text_size.floatValue = 16f
    menu_row_text_padding.floatValue = 4f
}

// ---------------------------------------------------------------------------
// Bigram n-gram model for word prediction / autocomplete
// ---------------------------------------------------------------------------

@Serializable
data class NgramModel(
    /** bigrams[word][nextWord] = occurrence count */
    val bigrams: Map<String, Map<String, Int>> = emptyMap()
)

/**
 * Format a Float to exactly one decimal place without java.lang.String.format.
 * Works on all KMP targets (no java.util dependency).
 */
private fun Float.oneDecimal(): String {
    val v = kotlin.math.round(this * 10).toInt()
    val whole = v / 10
    val frac = kotlin.math.abs(v % 10)
    return "$whole.$frac"
}

/** Returns a pre-seeded NgramModel with common AAC phrase patterns. */
fun seedNgramModel(): NgramModel {
    val pairs = listOf(
        "i" to "want", "i" to "need", "i" to "like", "i" to "don't",
        "i" to "feel", "i" to "am", "i" to "go", "i" to "eat",
        "want" to "more", "want" to "help", "want" to "to",
        "more" to "please", "need" to "help",
        "stop" to "please", "yes" to "please",
        "i" to "stop", "please" to "help"
    )
    val bigrams = mutableMapOf<String, MutableMap<String, Int>>()
    for ((first, second) in pairs) {
        val map = bigrams.getOrPut(first) { mutableMapOf() }
        map[second] = (map[second] ?: 0) + 1
    }
    return NgramModel(bigrams.mapValues { it.value.toMap() })
}

/**
 * Record that [nextWord] followed [prevWord] and update [ngram_model] in place.
 * Call this whenever a word is appended to the input box.
 */
fun updateNgramModel(prevWord: String, nextWord: String) {
    val prev = prevWord.lowercase().trim()
    val next = nextWord.lowercase().trim()
    if (prev.isBlank() || next.isBlank()) return
    val current = ngram_model.value
    val updated = current.bigrams.toMutableMap()
    val followers = (updated[prev] ?: emptyMap()).toMutableMap()
    followers[next] = (followers[next] ?: 0) + 1
    updated[prev] = followers
    ngram_model.value = NgramModel(updated)
}

// ---------------------------------------------------------------------------

@Serializable
data class PersistedState(
    val box_size_dp: Float,
    val box_padding_dp: Float,
    val input_box_height_dp: Float,
    val item_text_padding_dp: Float,
    val has_seen_tutorial: Boolean,
    val tts_data_found: Boolean,
    val menu_list: List<menutemplate>,
    val static_terms: List<String>,
    val static_row_height: Float,
    val menu_static_row_height: Float,
    val button_boxes_width: Float,
    val menu_row_ids: List<Int>,
    val tts_speech_rate: Float = 1.0f,
    val tts_pitch: Float = 1.0f,
    val tts_pause_between_words: Boolean = false,
    val tts_pause_duration: Long = 500L,
    val static_row_text_size: Float = 16f,
    val static_row_text_padding: Float = 4f,
    val menu_row_text_size: Float = 16f,
    val menu_row_text_padding: Float = 4f,
    val ngram_model: NgramModel = NgramModel()
)

fun PersistedState.withPaddedLists(): PersistedState = copy(
    menu_list = menu_list.map { menu ->
        val n = menu.item_list.size
        val uuids       = menu.item_uuids.toMutableList()
        val urls        = menu.image_urls.toMutableList()
        val custom      = menu.custom_image_paths.toMutableList()
        val audioPaths  = menu.custom_audio_paths.toMutableList()
        val audioNames  = menu.custom_audio_names.toMutableList()
        val pronOvr     = menu.pronunciation_overrides.toMutableList()
        @OptIn(ExperimentalUuidApi::class)
        while (uuids.size      < n) uuids.add(Uuid.random().toString())
        while (urls.size       < n) urls.add("")
        while (custom.size     < n) custom.add("")
        while (audioPaths.size < n) audioPaths.add("")
        while (audioNames.size < n) audioNames.add("")
        while (pronOvr.size    < n) pronOvr.add("")
        menu.copy(
            item_uuids            = uuids,
            image_urls            = urls,
            custom_image_paths    = custom,
            custom_audio_paths    = audioPaths,
            custom_audio_names    = audioNames,
            pronunciation_overrides = pronOvr
        )
    }
)

fun load_vars(state: PersistedState) {
    box_size = state.box_size_dp.dp
    box_padding = state.box_padding_dp.dp
    input_box_height = state.input_box_height_dp.dp
    item_text_padding = state.item_text_padding_dp.dp
    tts_data_found.value = state.tts_data_found

    static_terms.clear()
    static_terms.addAll(state.static_terms)

    static_row_height = state.static_row_height.dp
    menu_static_row_height = state.menu_static_row_height.dp
    button_boxes_width = state.button_boxes_width.dp

    menu_terms_ids.clear()
    menu_terms_ids.addAll(state.menu_row_ids)

    show_tutorial.value = state.has_seen_tutorial
    tts_speech_rate.value = state.tts_speech_rate
    tts_pitch.value = state.tts_pitch
    tts_pause_between_words.value = state.tts_pause_between_words
    tts_pause_duration.longValue = state.tts_pause_duration
    static_row_text_size.floatValue = state.static_row_text_size
    static_row_text_padding.floatValue = state.static_row_text_padding
    menu_row_text_size.floatValue = state.menu_row_text_size
    menu_row_text_padding.floatValue = state.menu_row_text_padding
    ngram_model.value = state.ngram_model

    MenuList.clear()
    MenuList.addAll(state.menu_list)

    val h = MenuList.firstOrNull { it.id == 0 }
    println("SpeGen load_vars END: incoming state Home cip=" +
            "${state.menu_list.firstOrNull { it.id == 0 }?.custom_image_paths}")
    println("SpeGen load_vars END: MenuList Home cip=${h?.custom_image_paths}")
}

/**
 * Loads all preferences from storage.
 * Returns true if data was found (returning user), false on a fresh install.
 */
suspend fun loadAllPreferences(): Boolean {
    val json = AppStorage.loadJson() ?: return false
    val state = try {
        Json { ignoreUnknownKeys = true }.decodeFromString<PersistedState>(json).withPaddedLists()
    } catch (e: Exception) {
        println("Failed to load preferences: ${e.message}")
        return false
    }
    load_vars(state)
    return true
}

fun currentPersistedState(): PersistedState = PersistedState(
    box_size_dp = box_size.value,
    box_padding_dp = box_padding.value,
    input_box_height_dp = input_box_height.value,
    item_text_padding_dp = item_text_padding.value,
    has_seen_tutorial = show_tutorial.value,
    tts_data_found = tts_data_found.value,
    menu_list = MenuList.toList(),
    static_terms = static_terms.toList(),
    static_row_height = static_row_height.value,
    menu_static_row_height = menu_static_row_height.value,
    button_boxes_width = button_boxes_width.value,
    menu_row_ids = menu_terms_ids.toList(),
    tts_speech_rate = tts_speech_rate.value,
    tts_pitch = tts_pitch.value,
    tts_pause_between_words = tts_pause_between_words.value,
    tts_pause_duration = tts_pause_duration.value,
    static_row_text_size = static_row_text_size.floatValue,
    static_row_text_padding = static_row_text_padding.floatValue,
    menu_row_text_size = menu_row_text_size.floatValue,
    menu_row_text_padding = menu_row_text_padding.floatValue,
    ngram_model = ngram_model.value
)

suspend fun saveAllPreferences() {
    killDanglingPointers()
    AppStorage.saveJson(Json.encodeToString(currentPersistedState()))
}

fun menutemplate.displayUrl(idx: Int): String {
    val custom = custom_image_paths.getOrNull(idx)
    if (!custom.isNullOrBlank()) return custom
    return image_urls.getOrNull(idx) ?: ""
}

fun PersistedState.normalizedForComparison() = copy(
    menu_list = menu_list.map { it.copy(image_urls = emptyList()) }
)
suspend fun hasStateChanged(): Boolean {
    val savedJson = AppStorage.loadJson() ?: return true
    val savedState = try {
        Json.decodeFromString<PersistedState>(savedJson)
    } catch (e: Exception) { return true }
    return currentPersistedState().normalizedForComparison() != savedState.normalizedForComparison()
}

suspend fun precacheAllImages(platformContext: PlatformContext) {
    if (cache_running.value) return

    val urls = MenuList
        .flatMap { it.image_urls }
        .filter { it.isNotBlank() }
        .distinct()

    if (urls.isEmpty()) return

    cache_running.value = true
    cache_progress.value = 0
    cache_total.value = urls.size

    val loader = SingletonImageLoader.get(platformContext)

    for (url in urls) {
        val request = ImageRequest.Builder(platformContext)
            .data(url)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        loader.execute(request)
        cache_progress.value += 1
    }

    cache_running.value = false
}

suspend fun resolveAndPrecacheAll(platformContext: PlatformContext) {
    if (cache_running.value) return
    getAccessToken()
    for (i in MenuList.indices) {
        val menu = MenuList[i]
        val complete = menu.image_urls.size == menu.item_list.size &&
                menu.image_urls.none { it.isBlank() }
        if (!complete) {
            val resolved = menu.item_list.mapIndexed { idx, word ->
                menu.image_urls.getOrNull(idx)?.takeIf { it.isNotBlank() }
                    ?: (useApiWithToken(accesstoken, word)?.image_url ?: "")
            }
            MenuList[i] = menu.copy(image_urls = resolved)
        }
    }
    saveAllPreferences()
    precacheAllImages(platformContext)
}

suspend fun hasUncachedImages(platformContext: PlatformContext): Boolean = withContext(Dispatchers.Default) {
    val loader = SingletonImageLoader.get(platformContext)
    val disk = loader.diskCache ?: return@withContext false
    for (menu in MenuList) {
        if (menu.image_urls.size != menu.item_list.size) return@withContext true
        for (url in menu.image_urls) {
            if (url.isBlank()) continue
            val snapshot = disk.openSnapshot(url)
            if (snapshot == null) return@withContext true
            snapshot.close()
        }
    }
    false
}

@Composable
fun CachePrompt() {
    val scope = rememberCoroutineScope()
    val platformContext = LocalPlatformContext.current

    AlertDialog(
        // While caching will swallow dismiss attempts
        onDismissRequest = {
            if (!cache_running.value) show_cache_prompt.value = false
        },
        title = {
            Text(
                if (cache_running.value) "Downloading images…"
                else "Save images for offline use?"
            )
        },
        text = {
            if (cache_running.value) {
                Column {
                    Text(
                        "Caching ${cache_progress.value} / ${cache_total.value}…",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val total = cache_total.value
                    if (total > 0) {
                        LinearProgressIndicator(
                            progress = { cache_progress.value.toFloat() / total },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please keep the app open until this finishes.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Text(
                    "SpeGen found symbols that haven't been downloaded to this " +
                            "device yet. Saving them now lets the app work fully without " +
                            "an internet connection. This may take a moment.\n\n" +
                            "You can also do this any time from Settings.",
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            // Buttons only exist before caching starts
            if (!cache_running.value) {
                Button(onClick = {
                    scope.launch {
                        resolveAndPrecacheAll(platformContext)
                        show_cache_prompt.value = false
                    }
                }) { Text("Download now") }
            }
        },
        dismissButton = {
            if (!cache_running.value) {
                Button(onClick = { show_cache_prompt.value = false }) { Text("Not now") }
            }
        }
    )
}

@Composable
fun GetScreenDimensions() {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
    isLandscape = windowInfo.containerSize.width > windowInfo.containerSize.height
}

@Serializable
data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Long
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class ApiSymbolResponse(
    // Data class for useApiWithToken to allow to parse the response data
    val id: Int,
    val symbol_key: String,
    val name: String,
    val locale: String,
    val license: String,
    val license_url: String,
    val author: String,
    val author_url: String,
    val source_url: String? = null,
    val skins: Boolean? = false,
    val repo_key: String,
    val hc: Boolean? = false,
    val extension: String,
    val image_url: String,
    val search_string: String? = null,
    val unsafe_result: Boolean,
    val _href: String,
    val details_url: String
)

suspend fun getAccessToken(): AccessTokenResponse? {
    return withContext(Dispatchers.Default) {
        try {
            val response = httpClient.submitForm(
                url = "https://www.opensymbols.org/api/v2/token",
                formParameters = Parameters.build { append("secret", CLIENT_SECRET) }
            )
            val tokenResponse = Json.decodeFromString<AccessTokenResponse>(response.bodyAsText())
            accesstoken = tokenResponse.access_token
            tokenResponse
        } catch (e: Exception) {
            println("Failed to get access token: ${e.message}")
            null
        }
    }
}

suspend fun useApiWithToken(token: String?, search: String): ApiSymbolResponse? {
    return withContext(Dispatchers.Default) {
        try {
            val response = httpClient.get("https://www.opensymbols.org/api/v2/symbols") {
                url {
                    parameters.append("q", search)
                    parameters.append("locale", "en")
                    parameters.append("safe", "0")
                    if (token != null) parameters.append("access_token", token)
                }
            }
            val text = response.bodyAsText()
            var symbolstring = text.replace("[", "").replace("]", "").split("},")[0]
            if (symbolstring.length > 1) symbolstring += "}"
            if (symbolstring.contains("}")) {
                symbolstring = symbolstring.dropLast(symbolstring.count { it == '}' } - 1)
                Json.decodeFromString<ApiSymbolResponse>(symbolstring)
            } else null
        } catch (e: Exception) {
            println("API call failed: ${e.message}")
            null
        }
    }
}

suspend fun useApiMultipleWithToken(token: String?, search: String, count: Int, index: Int): List<ApiSymbolResponse>? {
    return withContext(Dispatchers.Default) {
        try {
            val response = httpClient.get("https://www.opensymbols.org/api/v2/symbols") {
                url {
                    parameters.append("q", search)
                    parameters.append("locale", "en")
                    parameters.append("safe", "0")
                    if (token != null) parameters.append("access_token", token)
                }
            }
            val text = response.bodyAsText()
            val listofsymbols = mutableListOf<ApiSymbolResponse>()
            for (i in 0 until count) {
                var symbolstring = text.replace("[", "").replace("]", "").split("},")[i + index]
                if (symbolstring.length > 1) symbolstring += "}"
                if (symbolstring.contains("}")) {
                    symbolstring = symbolstring.dropLast(symbolstring.count { it == '}' } - 1)
                    listofsymbols += Json.decodeFromString<ApiSymbolResponse>(symbolstring)
                }
            }
            listofsymbols
        } catch (e: Exception) {
            println("API call failed: ${e.message}")
            null
        }
    }
}

fun findCachedUrl(word: String): String {
    for (menu in MenuList) {
        val idx = menu.item_list.indexOfFirst { it.equals(word, ignoreCase = true) }
        if (idx >= 0) {
            val url = menu.image_urls.getOrNull(idx)
            if (!url.isNullOrBlank()) return url
        }
    }
    return ""
}

@Composable
fun <T> ReorderableTermList(
    items: MutableList<T>,
    dragContent: @Composable RowScope.(index: Int, item: T) -> Unit,
    trailingContent: @Composable RowScope.(index: Int, item: T) -> Unit
) {
    val density = LocalDensity.current
    val rowHeightDp = 56.dp
    val rowHeightPx = with(density) { rowHeightDp.toPx() }

    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column {
        items.forEachIndexed { index, item ->
            val isDragging = index == draggingIndex
            val dimmed = draggingIndex >= 0 && !isDragging

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeightDp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset(y = with(density) {
                        if (isDragging) dragOffsetY.toDp() else 0.dp
                    })
                    .alpha(if (dimmed) 0.4f else 1f)
                    .background(if (isDragging) Color(0xFFE0E0E0) else Color.White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Draggable zone
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(index, items.size) {
                            detectDragGestures(
                                onDragStart = { draggingIndex = index; dragOffsetY = 0f },
                                onDragEnd = { draggingIndex = -1; dragOffsetY = 0f },
                                onDragCancel = { draggingIndex = -1; dragOffsetY = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragOffsetY += drag.y
                                    val movedRows = (dragOffsetY / rowHeightPx).toInt()
                                    val target = draggingIndex + movedRows
                                    if (movedRows != 0 &&
                                        target in items.indices &&
                                        target != draggingIndex
                                    ) {
                                        val moved = items.removeAt(draggingIndex)
                                        items.add(target, moved)
                                        draggingIndex = target
                                        dragOffsetY -= movedRows * rowHeightPx
                                    }
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "≡",
                        fontSize = 20.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    dragContent(index, item)
                }
                trailingContent(index, item)
            }
        }
    }
}

fun navigateTo(menuId: Int) {
    menu_history.add(linked_menu.value)
    linked_menu.value = menuId
    switchmenuparser.value++
}

/**
 * Speak a single item, choosing between custom audio, pronunciation override, and TTS.
 * Falls back gracefully when menu_id / item_index are not available.
 */
fun speakItem(menuId: Int, itemIndex: Int, displayName: String) {
    val menu = MenuFinder(menuId)
    val audioPath = menu.custom_audio_paths.getOrNull(itemIndex)?.takeIf { it.isNotBlank() }
    val pron      = menu.pronunciation_overrides.getOrNull(itemIndex)?.takeIf { it.isNotBlank() }
    when {
        audioPath != null -> playAudioFile(audioPath)
        pron != null -> ttsEngine.value?.speak(pron, TtsEngine.QUEUE_FLUSH, "item_$menuId.$itemIndex")
        tts_pause_between_words.value -> {
            val words = displayName.split(" ")
            ttsEngine.value?.speak(words[0], TtsEngine.QUEUE_FLUSH, "word_0")
            for (i in 1 until words.size) {
                ttsEngine.value?.playSilentUtterance(tts_pause_duration.value, TtsEngine.QUEUE_ADD, "pause_$i")
                ttsEngine.value?.speak(words[i], TtsEngine.QUEUE_ADD, "word_$i")
            }
        }
        else -> ttsEngine.value?.speak(displayName, TtsEngine.QUEUE_FLUSH, "item_$menuId.$itemIndex")
    }
}

@Composable
fun TutorialOverlay(onFinish: () -> Unit) {
    val slides = listOf(
        "Welcome to SpeGen" to "Tap symbols to build sentences.",
        "Categories" to "Tap a folder like People or Actions to find more words.",
        "Search" to "Tap the Search button on the right to find any word quickly.",
        "Speak" to "Tap the input bar at the top to read your sentence aloud."
    )
    var currentSlide by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .zIndex(2000f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = slides[currentSlide].first,
                color = Color.White,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            Text(
                text = slides[currentSlide].second,
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (currentSlide > 0) {
                    Button(onClick = { currentSlide -= 1 }) { Text("Back") }
                }
                Button(onClick = {
                    if (currentSlide < slides.size - 1) currentSlide += 1
                    else onFinish()
                }) {
                    Text(if (currentSlide < slides.size - 1) "Next" else "Done")
                }
                if (currentSlide < slides.size - 1) {
                    Button(onClick = onFinish) { Text("Skip") }
                }
            }
        }
    }
}


// Function that creates the static row of always accessible words at the bottom of the screen for easy access with for loop that allows for customization through variables
@Composable
fun Static_Row_Needs() {
    var text_color = Color.Black // Set as var to be able to be customized by user later
    var text_alignment = Alignment.Center // Set as var to be able to be customized by user later
    var box_color = Color.White // Set as var to be able to be customized by user later
    var border_size = 2.dp // Set as var to be able to be customized by user later
    var border_color = Color.Black // Set as var to be able to be customized by user later
    var width = (screenWidth/static_terms.size.dp).dp // Determine width of boxes by dividing screen width by total number of boxes which is equal to number of needed terms
    static_row_height = screenHeight * (1f / 8f) // Fraction deterxmined by base value of 70.dp then converted to fraction and applied to screen height to (hopefully) make box height scale with screen height
    var y_offset = (screenHeight-static_row_height) // Determines Y offset by subtracting height from the total screen width
    var x_offset = (0).dp // Determines X offset. Not needed since the first box starts at the left edge of the screen.
    for (i in 0 until static_terms.size) // For loop to create modular number of boxes. Starts at zero due to X offset calculations and ends at the number of terms minus 1 since it starts at zero
        Column() {
            val text = static_terms[i]
            Box(
                // FIX Y OFFSET
                modifier = Modifier
                    .offset((x_offset + (width * i)), y_offset)
                    .width(width)
                    .height(static_row_height)
                    .background(color = box_color)
                    .border(border = BorderStroke(border_size, border_color))
                    .clickable(onClick = {
                        if (ttsEngine.value?.isSpeaking == true) {
                            ttsEngine.value?.stop()
                        } else ttsEngine.value?.speak(
                            text, TtsEngine.QUEUE_FLUSH, ""
                        )
                    })
            ) {
                Text(
                    text = static_terms[i],
                    color = text_color,
                    fontSize = static_row_text_size.floatValue.sp,
                    modifier = Modifier
                        .align(text_alignment)
                        .padding(static_row_text_padding.floatValue.dp)
                )
            }
        }
}

@Composable
fun InputBox(modifier: Modifier) {
    LaunchedEffect(Unit) {
        getAccessToken()
    }

    input_box_height = screenHeight * (1f / 4f)

    Row {
        LazyRow(
            modifier = modifier
                .width(screenWidth - (button_boxes_width * 2))
                .height(input_box_height)
                .background(Color.White)
                .border(4.dp, Color.Black)
                .clickable {
                    if (ttsEngine.value?.isSpeaking == true) {
                        stopSentenceSequenced()
                    } else if (inputboxselecteditems_text.isNotEmpty()) {
                        val words      = inputboxselecteditems_text.toList()
                        val audioPaths = inputboxselecteditems_audio.toList()
                        val pauseMs    = if (tts_pause_between_words.value) tts_pause_duration.value else 0L
                        playSentenceSequenced(words, audioPaths, pauseMs) {}
                    }
                }
        ) {
            if (inputboxselecteditems_text.size == inputboxselecteditems_has_symbol.size) {
                items(inputboxselecteditems_text.size) { index ->
                    if (inputboxselecteditems_has_symbol[index]) {
                        InputBox_Symbol(index)
                    }
                    else
                    {
                        InputBox_Text(index)
                    }
                }
            }
        }
    }
}

@Composable
fun InputBox_Symbol(index: Int) {
    var name = inputboxselecteditems_text[index]
    var url by remember { mutableStateOf("") }
    LaunchedEffect(inputboxselecteditems_text) {
        val cached = findCachedUrl(name)
        url = cached.ifBlank {
            useApiWithToken(accesstoken, name)?.image_url ?: ""
        }
    }
    name = name.replaceFirstChar {
        if (it.isLowerCase())
            it.titlecase()
        else it.toString()
    }
    var height_dp = 16
    var width_dp = height_dp * 3.0625
    Box {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(url)
                .build(),
            "Picture of $name",
            modifier = Modifier
                .background(Color.White)
                .padding(box_padding)
                .scale(1f)
                .fillMaxHeight()
                .aspectRatio(1f)
        )
        Text(
            text = name,
            color = Color.Black,
            modifier = Modifier
                .padding(2.dp)
                .fillMaxSize()
                .align(Alignment.BottomCenter),
            textAlign = TextAlign.Center)
    }
}


@Composable
fun InputBox_Text(index: Int) {
    var name by remember {mutableStateOf(inputboxselecteditems_text[index])}
    name = name.replaceFirstChar {
        if (it.isLowerCase())
            it.titlecase()
        else it.toString()
    }
    Box(modifier = Modifier
        .fillMaxHeight()
        .aspectRatio(1f), contentAlignment = Alignment.CenterStart) {
        Text(
            text = name,
            color = Color.Black,
            modifier = Modifier
                .background(Color.White)
                .padding(box_padding)
                .scale(1f)
                .fillMaxSize(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
    }
}

@Composable
@NonSkippableComposable
fun Symbol(Name: String, image_url: String, Vertical_Stretch: Dp, tts_type: Int, x_offset: Dp = 0.dp, y_offset: Dp = 0.dp, modifier: Modifier = Modifier, menu_id: Int? = null, item_index: Int? = null) {
    if (x_offset > 0.dp || y_offset > 0.dp) {
        Row(modifier = Modifier.fillMaxSize())
        {
            if (wordfinder_display_buttonguide.intValue >= 1) {
                ButtonGuide_Wordfinder()
            }
        }
    }
    val name = Name.replaceFirstChar {
        if (it.isLowerCase())
            it.titlecase()
        else it.toString() }
    var height_dp = 16
    var width_dp = height_dp*3.0625
    Box(modifier = modifier)  {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(image_url)
                .build(),
            "Picture of $Name",
            modifier = modifier
                .offset(x_offset, y_offset)
                .height(box_size + Vertical_Stretch + (box_padding * 3))
                .background(Color.White)
                .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
                .padding(box_padding)
                .scale(1f)
                .width(box_size)
                .clickable(onClick = {
                    if (editor_mode.value && menu_id != null && item_index != null) {
                        edit_target_menu_id.intValue = menu_id
                        edit_target_index.intValue = item_index
                        show_edit_item_dialog.value = true
                        return@clickable
                    }
                    // ---- helpers for this click scope ----
                    fun doSpeak() {
                        if (menu_id != null && item_index != null) {
                            speakItem(menu_id, item_index, name)
                        } else {
                            // Word-finder clone: no menu context, use TTS directly
                            if (tts_pause_between_words.value) {
                                val sw = name.split(" ")
                                ttsEngine.value?.speak(sw[0], TtsEngine.QUEUE_FLUSH, "word_0")
                                for (i in 1 until sw.size) {
                                    ttsEngine.value?.playSilentUtterance(tts_pause_duration.value, TtsEngine.QUEUE_ADD, "pause_$i")
                                    ttsEngine.value?.speak(sw[i], TtsEngine.QUEUE_ADD, "word_$i")
                                }
                            } else {
                                ttsEngine.value?.speak(name, TtsEngine.QUEUE_FLUSH, "")
                            }
                        }
                    }
                    fun doAppend() {
                        val prevWord = inputboxselecteditems_text.lastOrNull()
                        val audioPath = if (menu_id != null && item_index != null)
                            MenuFinder(menu_id).custom_audio_paths.getOrNull(item_index) ?: "" else ""
                        val pron = if (menu_id != null && item_index != null)
                            MenuFinder(menu_id).pronunciation_overrides.getOrNull(item_index) ?: "" else ""
                        inputboxselecteditems_text      += name
                        inputboxselecteditems_has_symbol += true
                        inputboxselecteditems_audio      += audioPath
                        inputboxselecteditems_pron       += pron
                        if (prevWord != null) updateNgramModel(prevWord, name)
                    }
                    // ---- tts_type dispatch ----
                    if (tts_type == 0) {
                        if (ttsEngine.value?.isSpeaking == true) stopSentenceSequenced()
                        doSpeak()
                    }
                    if (tts_type == 1) {
                        doAppend()
                    }
                    if (tts_type == 2) {
                        if (ttsEngine.value?.isSpeaking == true) stopSentenceSequenced()
                        doSpeak()
                        doAppend()
                    }
                    if (!wordfinder_path_ids.isEmpty()) {
                        if (wordfinder_path_ids.size <= 1) {
                            wordfinder_highlight_index.intValue = -1
                            wordfinder_path_ids.removeAt(0)
                            wordfinder_path_ids.clear()
                            wordfinder_path_names.clear()
                            wordfinder_display_buttonguide.intValue = 0
                            createclonefolder.value = false
                            createclonesymbol.value = false
                        }
                    }
                })
        )
        var mod = Modifier.zIndex(1f)
        if (modifier != Modifier)
        {
            mod = Modifier.zIndex(1000f)
        }
        Text(text = name, color = Color.Black, modifier = mod
            .offset(x_offset, y_offset)
            .padding(item_text_padding)
            .height(height_dp.dp)
            .width(width_dp.dp)
            .align(Alignment.BottomCenter), textAlign = TextAlign.Center)
    }
}

@Composable
@NonSkippableComposable
fun Folder(Name: String, image_url: String, LinkedMenu: Int, Vertical_Stretch: Dp, x_offset: Dp = 0.dp, y_offset: Dp = 0.dp, modifier: Modifier = Modifier, menu_id: Int? = null, item_index: Int? = null) {
    if (x_offset > 0.dp || y_offset > 0.dp) {
        Row(modifier = Modifier.fillMaxSize())
        {
            if (wordfinder_display_buttonguide.intValue >= 1) {
                ButtonGuide_Wordfinder()
            }
        }
    }
    val name = Name.replaceFirstChar {
        if (it.isLowerCase())
            it.titlecase()
        else it.toString() }
    var height_dp = 16
    var width_dp = height_dp*3.0625
    Box(modifier = modifier)
    {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(image_url)
                .build(),
            "Picture of $name",
            modifier = Modifier
                .offset(x_offset, y_offset)
                .height(box_size + Vertical_Stretch + (box_padding * 3))
                .background(Color.White)
                .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
                .padding(box_padding)
                .scale(1f)
                .width(box_size)
                .clickable(onClick = {
                    if (editor_mode.value && menu_id != null && item_index != null) {
                        edit_target_menu_id.intValue = menu_id
                        edit_target_index.intValue = item_index
                        show_edit_item_dialog.value = true
                        return@clickable
                    }
                    if (!wordfinder_path_ids.isEmpty()) {
                        if (wordfinder_path_ids.size >= 2) {
                            if (LinkedMenu == wordfinder_path_ids[1]) {
                                wordfinder_path_ids.removeAt(0)
                                wordfinder_path_names.removeAt(0)
                                navigateTo(LinkedMenu)
                                wordfinder_manager()
                            }
                        }
                    } else {
                        navigateTo(LinkedMenu)
                    }
                    if (wordfinder_path_ids.size >= 1 && !wordfinder_target_is_symbol) {
                        wordfinder_highlight_index.intValue = -1
                        wordfinder_path_ids.removeAt(0)
                        wordfinder_path_ids.clear()
                        wordfinder_path_names.clear()
                        wordfinder_display_buttonguide.intValue = 0
                        createclonefolder.value = false
                        createclonesymbol.value = false
                        navigateTo(LinkedMenu)
                    }
                })
        )
        var mod = Modifier.zIndex(1f)
        if (modifier != Modifier)
        {
            mod = Modifier.zIndex(1000f)
        }
        Text(
            text = name,
            color = Color.Black,
            modifier = mod
                .offset(x_offset, y_offset)
                .padding(item_text_padding)
                .height(height_dp.dp)
                .width(width_dp.dp)
                .align(Alignment.BottomCenter),
            textAlign = TextAlign.Center
        )

        // Folded-corner indicator — marks this item as a folder, not a symbol
        val fold_size = (box_size.value * 0.25f).dp
        Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x_offset, y_offset)
                .padding(box_padding)
                .size(fold_size)
        ) {
            val fold = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(fold, Color.Black)
        }
    }
}

@Composable
fun MenuPlaceholder(vertical_stretch: Dp) {
    Box(
        modifier = Modifier
            .height(box_size + vertical_stretch + (box_padding * 3))
            .background(Color.White)
            .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
            .padding(box_padding)
            .scale(1f)
            .width(box_size)
    )
}

@Serializable
data class menutemplate(
    val id: Int, // ID of the current menu
    val title: String, // Title of the current menu
    val item_list: List<String>, // List of the names of all items, both folders and symbols
    val pointers: List<Int?>, // Pointers to be used in MenuFinder to find the corresponding menu for a folder to link to. Null if item is a symbol since it has no pointer.
    val tts: List<Int?>, // 0 is for appending to the input box without instantly playing, 1 is for instantly playing in tts engine without appending to input box, 2 is for both appending to text box and playing in tts engine instantly. If a value is null item is a folder that doesn't have tts.
    val item_type: List<Boolean>, // False is for folder, true is for symbol
    val image_urls: List<String> = emptyList(), // resolved OpenSymbols URLs
    val item_uuids: List<String> = emptyList(),   // one UUID per item
    val custom_image_paths: List<String> = emptyList(),
    val custom_audio_paths: List<String> = emptyList(), // absolute paths to custom recorded/imported audio
    val custom_audio_names: List<String> = emptyList(), // human-readable label for each audio clip
    val pronunciation_overrides: List<String> = emptyList() // alternate TTS text (overrides the item name)
)

fun parentsOf(menuId: Int): List<Int> {
    if (menuId == 0) return emptyList()
    return MenuList
        .filter { menu -> menu.pointers.contains(menuId) }
        .map { it.id }
}

fun primaryParentOf(menuId: Int): Int? {
    return parentsOf(menuId).firstOrNull()
}

fun allPathsToMenu(targetId: Int): List<List<Int>> {
    val results = mutableListOf<List<Int>>()

    fun dfs(currentId: Int, pathSoFar: List<Int>) {
        if (currentId in pathSoFar) return  // cycle guard
        val newPath = pathSoFar + currentId
        if (currentId == targetId) {
            results.add(newPath)
            return
        }
        val menu = MenuList.find { it.id == currentId } ?: return
        menu.pointers.filterNotNull().distinct().forEach { childId ->
            dfs(childId, newPath)
        }
    }

    dfs(0, emptyList())
    return results
}

data class SymbolPath(
    val menuPath: List<Int>,
    val menuNames: List<String>,
    val containingMenuId: Int,
    val itemIndex: Int
)

fun allPathsToSymbol(menuListElement: Int, itemIndex: Int): List<SymbolPath> {
    val containingMenuId = MenuList[menuListElement].id
    return allPathsToMenu(containingMenuId).map { menuIdPath ->
        SymbolPath(
            menuPath = menuIdPath,
            menuNames = menuIdPath.map { id -> MenuList.find { it.id == id }?.title ?: "?" },
            containingMenuId = containingMenuId,
            itemIndex = itemIndex
        )
    }
}

fun MenuFinder(menu_id: Int?): menutemplate {
    if (menu_id !is Int) {
        return home
    }
    for (i in 0 until MenuList.size) {
        if (MenuList[i].id == menu_id) {
            return MenuList[i]
        }
    }
    return home
}

@Composable
fun MenuKeyGen() {
    menukeylist.clear()
    for (i in 0 until MenuList.size) {
        menukeylist += MenuList[i].id
    }
}

@Composable
@NonSkippableComposable
fun MenuParser(menutemplate: menutemplate, modifier: Modifier = Modifier) {
    var totalitems = ((screenWidth - (button_boxes_width * 2))/(box_size + (box_padding*2)))*((screenHeight-(static_row_height+menu_static_row_height))/box_size)
    var total_box_size = box_size+(box_padding*2)
    val vertical_stretch = ((menu_height)-((((menu_height)/(total_box_size)).toInt())*total_box_size))
    var item_names = remember { mutableStateListOf<String>() }
    var item_urls = remember { mutableStateListOf<String>() }
    if (item_text_padding < 5.dp)
    {
        item_text_padding = 5.dp
    }
    LaunchedEffect(menutemplate.id, menutemplate.item_list, menutemplate.image_urls, menutemplate.custom_image_paths, switchmenuparser.value) {
        item_names.clear()
        item_urls.clear()

        val cachedUrls = menutemplate.image_urls
        val allResolved = cachedUrls.size == menutemplate.item_list.size &&
                cachedUrls.none { it.isBlank() }

        if (allResolved) {
            item_names.addAll(menutemplate.item_list)
            menutemplate.item_list.indices.forEach { idx ->
                val u = menutemplate.displayUrl(idx)
                println("SpeGen MenuParser idx=$idx custom=${menutemplate.custom_image_paths.getOrNull(idx)} -> $u")
                item_urls.add(u)
            }
        } else {
            getAccessToken()
            val resolved = mutableListOf<String>()
            menutemplate.item_list.forEachIndexed { index, query ->
                val existing = cachedUrls.getOrNull(index)
                val url = if (!existing.isNullOrBlank()) existing
                else useApiWithToken(accesstoken, query)?.image_url ?: ""
                item_names.add(query)
                resolved.add(url)
            }
            val menuIndex = MenuList.indexOfFirst { it.id == menutemplate.id }
            if (menuIndex >= 0 && resolved.any { it.isNotBlank() }) {
                MenuList[menuIndex] = MenuList[menuIndex].copy(image_urls = resolved)
                trigger_save.value = true
            }
            menutemplate.item_list.indices.forEach { idx ->
                val custom = menutemplate.custom_image_paths.getOrNull(idx)
                item_urls.add(
                    if (!custom.isNullOrBlank()) custom
                    else resolved.getOrNull(idx) ?: ""
                )
            }
        }
    }
    current_menu_id = menutemplate.id
    key(switchmenuparser.value, menutemplate.id) {
        val item_width = box_size + (box_padding * 2)
        val item_height_total = box_size + vertical_stretch + (box_padding * 3)
        val items_per_row = (menu_width.value / item_width.value).toInt().coerceAtLeast(1)
        val rows_per_page = (menu_height.value / item_height_total.value).toInt().coerceAtLeast(1)
        val items_per_page = (items_per_row * rows_per_page).coerceAtLeast(1)
        val total_items = menutemplate.item_list.size
        val page_count = ((total_items + items_per_page - 1) / items_per_page).coerceAtLeast(1)

        val pagerState = rememberPagerState(pageCount = { page_count })

        LaunchedEffect(wordfinder_highlight_index.intValue, items_per_page, page_count) {
            val idx = wordfinder_highlight_index.intValue
            if (idx >= 0) {
                val target_page = (idx / items_per_page).coerceIn(0, page_count - 1)
                if (pagerState.currentPage != target_page) {
                    pagerState.animateScrollToPage(target_page)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxWidth().fillMaxHeight()
        ) { page ->
            val startIndex = page * items_per_page
            val endIndex = minOf(startIndex + items_per_page, total_items)
            val empty_slots = items_per_page - (endIndex - startIndex)

            FlowRow(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Top
            ) {
                for (i in startIndex until endIndex) {
                    if (i >= item_names.size || i >= item_urls.size) {
                        MenuPlaceholder(vertical_stretch)
                        continue
                    }
                    val itemKey = "${menutemplate.id}-$i"
                    Box(modifier = Modifier.onGloballyPositioned { coords ->
                        item_positions[itemKey] = coords.positionInRoot()
                    }) {
                        if (menutemplate.item_type[i]) {
                            Symbol(item_names[i], item_urls[i], vertical_stretch,
                                menutemplate.tts[i]!!, menu_id = menutemplate.id, item_index = i)
                        } else {
                            Folder(item_names[i], item_urls[i], menutemplate.pointers[i]!!,
                                vertical_stretch, menu_id = menutemplate.id, item_index = i)
                        }
                    }
                }
                repeat(empty_slots) {
                    MenuPlaceholder(vertical_stretch)
                }
            }
        }
    }
}

@Composable
fun Menu(modifier: Modifier) {
    menu_height = (screenHeight - menu_static_row_height - static_row_height - input_box_height)
    menu_width = screenWidth - (button_boxes_width * 2)
    Column(
        modifier = Modifier.alpha(1f)
    ) {
        Column(
            modifier = modifier
                .width(menu_width)
                .height(menu_height)
                .offset(x = 0.dp, y = input_box_height)
        ) {
            MenuParser(MenuFinder(linked_menu.value))
        }
    }
}

@Composable
fun MenuRow(modifier: Modifier) {
    for (i in 0 until menu_terms_ids.size) // For loop to create modular number of boxes. Starts at zero due to X offset calculations and ends at the number of terms minus 1 since it starts at zero
    {
        Menurowbox(modifier, i, menu_terms_ids)
    }
}

@Composable
fun Menurowbox(modifier: Modifier, i: Int, menu_terms_ids: MutableList<Int>) {
    var text_color = Color.Black // Set as var to be able to be customized by user later
    var box_color = Color.White // Set as var to be able to be customized by user later
    var border_size = 2.dp // Set as var to be able to be customized by user later
    var border_color = Color.Black // Set as var to be able to be customized by user later
    var width =
        (screenWidth / menu_terms_ids.size) // Determine width of boxes by dividing screen width by total number of boxes which is equal to number of needed terms
    menu_static_row_height =
        screenHeight * (1f / 8f) // Fraction determined by base value of 70.dp then converted to fraction and applied to screen height to (hopefully) make box height scale with screen height
    var y_offset =
        (screenHeight - menu_static_row_height - static_row_height) // Determines Y offset by subtracting height from the total screen width
    var x_offset =
        (0).dp // Determines X offset. Not needed since the first box starts at the left edge of the screen.
    Column(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()) {
        Box(
            modifier = modifier
                .offset((x_offset + (width * i)), y_offset)
                .width(width)
                .height(menu_static_row_height)
                .background(color = box_color)
                .border(border = BorderStroke(border_size, border_color))
                .clickable(onClick = {
                    navigateTo(menu_terms_ids[i])
                })
        ) {
            Text(
                text = MenuFinder(menu_terms_ids[i]).title,
                color = text_color,
                fontSize = menu_row_text_size.floatValue.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(menu_row_text_padding.floatValue.dp)
            )
        }
    }
}

@Composable
fun WordFinder() {
    var showRow by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<String>() }
    val box_height = ((screenHeight.value - static_row_height.value) * (0.8)).dp
    val box_width = (screenWidth.value * 0.8).dp
    val row_height = 56.dp
    val flowrow_height_space = box_height-row_height
    val flowrow_width_space = box_width
    val scrollState = rememberScrollState()
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(red = 230, green = 227, blue = 227, alpha = 100))) {
        Box(
            modifier = Modifier
                .offset(
                    x = (screenWidth.value * 0.1).dp,
                    y = ((screenHeight.value - static_row_height.value) * 0.1).dp
                )
                .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
                .clip(RoundedCornerShape(40.dp))
                .height(box_height)
                .width(box_width)
                .background(Color.White)
                .padding(horizontal = 15.dp, vertical = 20.dp)
        ) {
            var text by remember { mutableStateOf("") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(row_height),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        wordfinder_display.intValue = 0
                        wordfinder_display_buttonguide.intValue = 0
                        wordfinder_path_ids.clear()
                        wordfinder_path_names.clear()
                        wordfinder_highlight_index.intValue = -1
                        createclonefolder.value = false
                        createclonesymbol.value = false
                        linked_menu.value = current_menu_id
                        switchmenuparser.value++
                    }
                ) {
                    Text(text = "Close", textAlign = TextAlign.Center)
                }

                TextField(
                    value = text,
                    onValueChange = { newText -> text = newText },
                    label = { Text("Image Search") },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )

                Button(
                    onClick = {
                        searchQuery = text
                        showRow = true
                    }
                ) {
                    Text(text = "Search", textAlign = TextAlign.Center)
                }
            }
            if (showRow) {
                Column {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .offset(y = row_height)
                            .verticalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown Menu for Suggestions
                        for (i in 0 until MenuList.size) {
                            for (a in 0 until MenuList[i].item_list.size) {
                                if (MenuList[i].item_list[a].lowercase().replace(" ", "") ==
                                    searchQuery.lowercase().replace(" ", "")) {
                                    val paths = allPathsToSymbol(i, a)
                                    paths.forEach { symbolPath ->
                                        WordFinder_Card(
                                            i,
                                            MenuList[i].item_type[a],
                                            a,
                                            symbolPath,
                                            flowrow_height_space,
                                            box_width
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getMenuPath(menuIndex: Int): String {
    val pathParts = mutableListOf<String>()
    val visited = mutableSetOf<Int>()
    var current: menutemplate? = MenuList[menuIndex]
    wordfinder_path_names.clear()
    while (current != null) {
        if (current.id in visited) break
        visited.add(current.id)
        pathParts.add(0, current.title)
        wordfinder_path_names.add(0, current.title)
        val parentId = primaryParentOf(current.id)
        current = if (parentId != null) MenuList.find { it.id == parentId } else null
    }
    return pathParts.joinToString(" > ")
}

fun setWordfinderPath(menuIndex: Int) {
    val visited = mutableSetOf<Int>()
    var current: menutemplate? = MenuList[menuIndex]
    wordfinder_path_ids.clear()
    while (current != null) {
        if (current.id in visited) break
        visited.add(current.id)
        wordfinder_path_ids.add(0, current.id)
        val parentId = primaryParentOf(current.id)
        current = if (parentId != null) MenuList.find { it.id == parentId } else null
    }
}

@Composable
fun WordFinder_Card(
    MenuList_element: Int,
    is_symbol: Boolean,
    item_position: Int,
    symbolPath: SymbolPath,
    total_avaliable_height: Dp,
    total_avaiable_width: Dp
) {
    var card_name by remember { mutableStateOf("") }
    var card_url by remember { mutableStateOf("") }
    val box_size = (total_avaliable_height / 4)
    val pathLabel = symbolPath.menuNames.joinToString(" > ")

    LaunchedEffect(Unit) {
        card_name = MenuList[MenuList_element].item_list[item_position]
        val res = useApiWithToken(accesstoken, card_name)
        card_url = res?.image_url ?: ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(4.dp, Color.Black, RoundedCornerShape(40.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(card_url).build(),
                contentDescription = "Picture of $card_name",
                modifier = Modifier.size(box_size).scale(1f)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = card_name.replaceFirstChar { it.titlecase() },
                    fontSize = (box_size.value / 3).sp
                )
                Text(
                    text = pathLabel,
                    fontSize = (box_size.value / 5).sp,
                    color = Color.Gray
                )
            }
            var showButtonGuide by remember { mutableStateOf(false) }
            Button(
                onClick = { showButtonGuide = true },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) { Text("Find") }

            if (showButtonGuide) {
                wordfinder_target_is_symbol = is_symbol
                // Set path state from this specific path
                wordfinder_path_ids.clear()
                wordfinder_path_ids.addAll(symbolPath.menuPath)
                wordfinder_path_names.clear()
                wordfinder_path_names.addAll(symbolPath.menuNames)
                val targetName = MenuList[MenuList_element].item_list[item_position]
                if (wordfinder_path_names.lastOrNull() != targetName) {
                    wordfinder_path_names.add(targetName)
                }
                navigateTo(symbolPath.menuPath.first())
                wordfinder_display_buttonguide.intValue += 1
                wordfinder_display.intValue = 0
                wordfinder_manager()
            }
        }
    }
}

fun wordfinder_manager() {
    if (wordfinder_path_ids.size > 1) {
        createclonesymbol.value = false
        createclonefolder.value = true
    } else {
        createclonefolder.value = !wordfinder_target_is_symbol
        createclonesymbol.value = wordfinder_target_is_symbol
    }
}

@Composable
fun ButtonGuide_Wordfinder() {
    Row(
        modifier = Modifier
            .zIndex(2f)
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.5f))
            .clickable {
                wordfinder_highlight_index.intValue = -1
                wordfinder_path_ids.clear()
                wordfinder_display_buttonguide.intValue = 0
                createclonefolder.value = false
            },
    )
    {}
}

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("UI", "Voice", "Backup", "Misc", "About")

    var done_clicked by remember { mutableStateOf(false) }
    var unsaved by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(done_clicked) {
        if (done_clicked) unsaved = hasStateChanged()
    }
    LaunchedEffect(unsaved) {
        if (done_clicked && unsaved == false) onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .zIndex(1500f)
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
        ) {
            // TAB STRIP
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = 2.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(if (isSelected) Color.White else Color(0xFFB0B0B0))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = Color.Black,
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            )
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color(0xFF404040),
                            fontSize = 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // CONTENT PANEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
                    .border(2.dp, Color.Black)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> UISettingsContent()
                    1 -> VoiceSettingsContent()
                    2 -> BackupSettingsContent()
                    3 -> MiscSettings()
                    4 -> AboutContent()
                }
            }

            // GLOBAL APPLY & DONE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = {
                    trigger_save.value = true
                    switchmenuparser.value++
                }) { Text("Apply") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { done_clicked = true }) { Text("Done") }
            }
        }
    }

    if (done_clicked && unsaved == true) {
        AlertDialog(
            onDismissRequest = {
                done_clicked = false
                unsaved = null
            },
            title = { Text("Unsaved Changes") },
            text = {
                Text(
                    "You have unsaved changes. Do you want to save them?",
                    fontSize = 14.sp,
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(onClick = {
                    trigger_save.value = true
                    switchmenuparser.value++
                    done_clicked = false
                    unsaved = null
                    onClose()
                }) { Text("Save Changes") }
            },
            dismissButton = {
                Button(onClick = {
                    trigger_load.value = true
                    switchmenuparser.value++
                    done_clicked = false
                    unsaved = null
                    onClose()
                }) { Text("Don't Save") }
            }
        )
    }
}

@Composable
fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (expanded) Color(0xFFD0D0D0) else Color(0xFFE8E8E8))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "▼" else "▶",
                modifier = Modifier.padding(end = 12.dp),
                fontSize = 14.sp
            )
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, end = 8.dp, bottom = 12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun UISettingsContent() {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        ExpandableSection("Static Symbol Row") { StaticSymbolRowSettings() }
        ExpandableSection("Menu Row") { MenuRowSettings() }
        ExpandableSection("Item Sizing") { ItemSizingSettings() }
        ExpandableSection("Edit Mode") { EditMode() }
    }
}

@Composable
fun EditMode()
{
    Column {
        Text("Edit symbols, folders, and menus by tapping them.",
            fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            editor_mode.value = true
            show_settings.value = false
        }) { Text("Enable editor mode") }
    }
}

@Composable
fun MiscSettings() {
    val scope = rememberCoroutineScope()
    val platformContext = LocalPlatformContext.current
    Column {
        Button(
            onClick = {
                scope.launch { resolveAndPrecacheAll(platformContext) }
            },
            enabled = !cache_running.value,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (cache_running.value)
                    "Caching ${cache_progress.value} / ${cache_total.value}…"
                else
                    "Download all images for offline use"
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Offline image caching happens automatically. Use this only if " +
                    "images aren't loading without an internet connection.",
            fontSize = 13.sp, color = Color.Gray
        )
    }
}

@Composable
fun BackupSettingsContent() {
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Save your SpeGen vocabulary to a file, or restore from a backup.",
            fontSize = 14.sp, color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        BackupSection { success, message ->
            statusMessage = message
            isError = !success
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Exports your vocabulary, settings, and any custom images into a single file that can be transferred to another device or kept as a backup.",
            fontSize = 13.sp, color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Accepts .spegen backup files. Importing replaces all current vocabulary and settings.",
            fontSize = 13.sp, color = Color.DarkGray
        )
        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = statusMessage,
                fontSize = 14.sp,
                color = if (isError) Color.Red else Color(0xFF2E7D32)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Note: Importing replaces all current data. Export first if you want to keep a copy.",
            fontSize = 12.sp, color = Color.Gray
        )
    }
}

@Composable
fun StaticSymbolRowSettings() {
    Column {
        Text("Words always visible at the bottom of the screen.", fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        ReorderableTermList(
            items = static_terms,
            dragContent = { _, term ->
                Text(term, fontSize = 16.sp)
            },
            trailingContent = { index, _ ->
                Button(onClick = { static_terms.removeAt(index) },
                    modifier = Modifier.padding(horizontal = 8.dp)) { Text("Remove") }
            }
        )
        var newTerm by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newTerm,
                onValueChange = { newTerm = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("New term") }
            )
            Button(
                onClick = {
                    val trimmed = newTerm.trim()
                    if (trimmed.isNotEmpty()) {
                        static_terms.add(trimmed)
                        newTerm = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Add") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Text size", fontSize = 16.sp)
            Text("${static_row_text_size.floatValue.toInt()} sp", fontSize = 14.sp, color = Color.Gray)
        }
        Slider(
            value = static_row_text_size.floatValue,
            onValueChange = { static_row_text_size.floatValue = it },
            valueRange = 8f..32f,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Text padding", fontSize = 16.sp)
            Text("${static_row_text_padding.floatValue.toInt()} dp", fontSize = 14.sp, color = Color.Gray)
        }
        Slider(
            value = static_row_text_padding.floatValue,
            onValueChange = { static_row_text_padding.floatValue = it },
            valueRange = 0f..24f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Preview", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val samples = static_terms.take(3).ifEmpty { listOf("Sample") }
                samples.forEach { term ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.White)
                            .border(2.dp, Color.Black)
                    ) {
                        Text(
                            text = term,
                            fontSize = static_row_text_size.floatValue.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(static_row_text_padding.floatValue.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuRowSettings() {
    Column {
        Text("Choose which menus appear in the bottom menu row.", fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        ReorderableTermList(
            menu_terms_ids,
            dragContent = { _, menuId ->
                Text(text = MenuFinder(menuId).title, modifier = Modifier.weight(1f), fontSize = 16.sp)
            },
            trailingContent = { _, menuId ->
                Button(onClick = {
                    menu_terms_ids.remove(menuId)
                    if (menu_terms_ids.isEmpty()) {
                        menu_static_row_height = 0.dp
                        screen_display.value = !screen_display.value
                    }
                }) { Text("Remove") }
                Spacer(modifier = Modifier.width(8.dp))
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Text size", fontSize = 16.sp)
            Text("${menu_row_text_size.floatValue.toInt()} sp", fontSize = 14.sp, color = Color.Gray)
        }
        Slider(
            value = menu_row_text_size.floatValue,
            onValueChange = { menu_row_text_size.floatValue = it },
            valueRange = 8f..32f,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Text padding", fontSize = 16.sp)
            Text("${menu_row_text_padding.floatValue.toInt()} dp", fontSize = 14.sp, color = Color.Gray)
        }
        Slider(
            value = menu_row_text_padding.floatValue,
            onValueChange = { menu_row_text_padding.floatValue = it },
            valueRange = 0f..24f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Preview", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val samples = MenuList.take(3).map { it.title }.ifEmpty { listOf("Menu") }
                samples.forEach { title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.White)
                            .border(2.dp, Color.Black)
                    ) {
                        Text(
                            text = title,
                            fontSize = menu_row_text_size.floatValue.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(menu_row_text_padding.floatValue.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemSizingSettings() {
    var preview_size by remember { mutableFloatStateOf(box_size.value) }

    val item_total_width = preview_size + box_padding.value * 2
    val item_total_height = preview_size + box_padding.value * 3
    val items_per_row = (menu_width.value / item_total_width ).toInt().coerceAtLeast(1)
    val rows_per_page = (menu_height.value / item_total_height).toInt().coerceAtLeast(1)
    val items_per_page = items_per_row * rows_per_page

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Item size", fontSize = 16.sp)
            Text(
                "${preview_size.toInt()} dp",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = preview_size,
            onValueChange = {
                preview_size = it
                box_size = it.dp
            },
            valueRange = 40f..180f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "$items_per_row per row· $rows_per_page rows· $items_per_page per page",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Preview", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))

        val currentMenu = MenuFinder(linked_menu.value)
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
                .verticalScroll(scrollState)
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val preview_count = (items_per_row * minOf(rows_per_page + 1, 4))
                    .coerceAtMost(24)

                repeat(preview_count) { index ->
                    Box(
                        modifier = Modifier
                            .height(item_total_height.dp)
                            .width(preview_size.dp)
                            .background(Color.White)
                            .border(
                                2.dp,
                                Color.Black,
                                RoundedCornerShape(
                                    (40f * (preview_size / 100f)).coerceIn(4f, 40f).dp
                                )
                            )
                    ) {
                        val label = if (index < currentMenu.item_list.size)
                            currentMenu.item_list[index].replaceFirstChar { it.titlecase() }
                        else "Word"

                        Text(
                            text = label,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 2.dp, vertical = 1.dp),
                            fontSize = (preview_size / 7f).coerceAtLeast(8f).sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    preview_size = 100f
                    box_size = 100.dp
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
            ) { Text("Reset") }
        }
    }
}
@Composable
fun VoiceSettingsContent() {
    val example_sentence = "The quick brown fox jumps over the lazy dog."

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {

        // Speech Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Speech rate", fontSize = 16.sp)
            Text(
                when {
                    tts_speech_rate.value < 0.6f -> "Very slow"
                    tts_speech_rate.value < 0.9f -> "Slow"
                    tts_speech_rate.value < 1.1f -> "Normal"
                    tts_speech_rate.value < 1.5f -> "Fast"
                    else -> "Very fast"
                } + "  (${tts_speech_rate.value.oneDecimal()}×)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Slider(
            value = tts_speech_rate.value,
            onValueChange = {
                tts_speech_rate.value = it
                ttsEngine.value?.setSpeechRate(it)
            },
            valueRange = 0.25f..2.0f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pitch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Pitch", fontSize = 16.sp)
            Text(
                when {
                    tts_pitch.value < 0.7f -> "Low"
                    tts_pitch.value < 0.9f -> "Slightly low"
                    tts_pitch.value < 1.1f -> "Normal"
                    tts_pitch.value < 1.4f -> "Slightly high"
                    else -> "High"
                } + "  (${tts_pitch.value.oneDecimal()}×)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Slider(
            value = tts_pitch.value,
            onValueChange = {
                tts_pitch.value = it
                ttsEngine.value?.setPitch(it)
            },
            valueRange = 0.5f..2.0f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pause between words
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { tts_pause_between_words.value = !tts_pause_between_words.value }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = tts_pause_between_words.value,
                onCheckedChange = { tts_pause_between_words.value = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Pause between words", fontSize = 16.sp)
                Text(
                    "Inserts a short silence between each word when speaking a sentence.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        if (tts_pause_between_words.value) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pause duration", fontSize = 14.sp)
                Text(
                    "${(tts_pause_duration.longValue / 1000f).oneDecimal()}s",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Slider(
                value = tts_pause_duration.longValue.toFloat(),
                onValueChange = { tts_pause_duration.longValue = it.toLong() },
                valueRange = 100f..2000f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test voice
        Button(
            onClick = {
                if (tts_pause_between_words.value) {
                    val words = example_sentence.split(" ")
                    ttsEngine.value?.speak(words[0], TtsEngine.QUEUE_FLUSH, "word_0")
                    for (i in 1 until words.size) {
                        ttsEngine.value?.playSilentUtterance(
                            tts_pause_duration.longValue, TtsEngine.QUEUE_ADD, "pause_$i"
                        )
                        ttsEngine.value?.speak(words[i], TtsEngine.QUEUE_ADD, "word_$i")
                    }
                } else {
                    ttsEngine.value?.speak(example_sentence, TtsEngine.QUEUE_FLUSH, "preview")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) { Text("▶ Test voice") }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Voice engine & language", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Install voice packs, switch engines (if available), or change language in your device's TTS settings.",
            fontSize = 13.sp, color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { openTtsSettings() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open device TTS settings") }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                tts_speech_rate.value = 1.0f
                tts_pitch.value = 1.0f
                tts_pause_between_words.value = false
                tts_pause_duration.longValue = 500L
                ttsEngine.value?.setSpeechRate(1.0f)
                ttsEngine.value?.setPitch(1.0f)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
        ) { Text("Reset to defaults") }
    }
}

@Composable
fun AboutContent() {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        Text("SpeGen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                append("An open-source AAC app developed by Harper Klein Keane. SpeGen's website can be found at the following URL: ")
                withLink(
                    LinkAnnotation.Url(
                        url = "https://hkleinkeane.github.io/spegen/",
                        styles = TextLinkStyles(style = SpanStyle(color = Color.Blue))
                    )
                ) {
                    append("https://hkleinkeane.github.io/spegen/")
                }
                append(".")
            },
            fontSize = 14.sp)
        Text("License: GNU General Public License v3.0", fontSize = 14.sp, color = Color.Gray)
    }
}


@Composable
fun Buttonboxes() {
    val a = remember {mutableIntStateOf(0)}
    button_boxes_width = (screenHeight.value*(1f/8f)).dp
    val x_offset = ((screenWidth - button_boxes_width).value).dp
    val y_offset = 0.dp
    var showKeyboard by remember { mutableStateOf(false)}
    if (wordfinder_display.value == a.value) {
        //TOP RIGHT
        Column() {
            Box(
                modifier = Modifier
                    .offset(x_offset, y_offset)
                    .size(button_boxes_width)
                    .background(color = Color.White)
                    .border(border = BorderStroke(2.dp, Color.Black))
                    .clickable(onClick = {
                        show_settings.value = true
                    })
            ) {
                Text(text = "Settings", color = Color.Black, modifier = Modifier
                    .align(Alignment.Center)
                    .padding(3.dp))
            }
        }
        //BOTTOM RIGHT
        Column() {
            Box(
                modifier = Modifier
                    .offset(x_offset, y_offset + button_boxes_width * 2)
                    .size(button_boxes_width)
                    .background(color = Color.White)
                    .border(border = BorderStroke(2.dp, Color.Black))
                    .clickable(onClick = {
                        if (ttsEngine.value?.isSpeaking == true) {
                            ttsEngine.value?.stop()
                        }
                    })
            ) {
                Text(text = "Stop", color = Color.Black, modifier = Modifier
                    .align(Alignment.Center)
                    .padding(3.dp))
            }
        }
        //TOP LEFT
        Column() {
            Box(
                modifier = Modifier
                    .offset(x_offset - button_boxes_width)
                    .size(button_boxes_width)
                    .background(color = Color.White)
                    .border(border = BorderStroke(2.dp, Color.Black))
                    .clickable(onClick = {
                        showKeyboard = true
                    })
            ) {
                Text(text = "Keyboard", color = Color.Black, modifier = Modifier
                    .align(Alignment.Center)
                    .padding(3.dp))
            }}
        //MIDDLE LEFT
        Column() {
            Box(
                modifier = Modifier
                    .offset(x_offset - button_boxes_width, y_offset + button_boxes_width * 2)
                    .size(button_boxes_width)
                    .background(color = Color.White)
                    .border(border = BorderStroke(2.dp, Color.Black))
                    .clickable(onClick = {
                        inputboxselecteditems_text.clear()
                        inputboxselecteditems_has_symbol.clear()
                        inputboxselecteditems_audio.clear()
                        inputboxselecteditems_pron.clear()
                        show_autocomplete.value = false
                    })
            ) {
                Text(text = "Clear", color = Color.Black, modifier = Modifier
                    .align(Alignment.Center)
                    .padding(3.dp))
            }

        }
        //BOTTOM LEFT
        Column() {
            Box(
                modifier = Modifier
                    .offset(x_offset - button_boxes_width, y_offset + button_boxes_width)
                    .size(button_boxes_width)
                    .background(color = Color.White)
                    .border(border = BorderStroke(2.dp, Color.Black))
                    .clickable(onClick = {
                        if (inputboxselecteditems_text.isNotEmpty() && inputboxselecteditems_has_symbol.isNotEmpty()) {
                            inputboxselecteditems_text.removeAt(inputboxselecteditems_text.lastIndex)
                            inputboxselecteditems_has_symbol.removeAt(inputboxselecteditems_has_symbol.lastIndex)
                            if (inputboxselecteditems_audio.isNotEmpty())
                                inputboxselecteditems_audio.removeAt(inputboxselecteditems_audio.lastIndex)
                            if (inputboxselecteditems_pron.isNotEmpty())
                                inputboxselecteditems_pron.removeAt(inputboxselecteditems_pron.lastIndex)
                        }
                    })
            ) {
                Text(text = "Delete", color = Color.Black, modifier = Modifier
                    .align(Alignment.Center)
                    .padding(3.dp))
            }
        }
        //AUTOCOMPLETE TOGGLE (row 4, below the Back button)
        Column() {
            Box(
                modifier = Modifier
                    .offset(x_offset - button_boxes_width, y_offset + button_boxes_width * 4)
                    .size(button_boxes_width)
                    .background(color = if (show_autocomplete.value) Color(0xFFBBDEFB) else Color.White)
                    .border(border = BorderStroke(2.dp, Color.Black))
                    .clickable { show_autocomplete.value = !show_autocomplete.value }
            ) {
                Text(text = "Predict", color = Color.Black, modifier = Modifier
                    .align(Alignment.Center)
                    .padding(3.dp))
            }
        }
    }
    //MIDDLE RIGHT
    Column() {
        Box(
            modifier = Modifier
                .offset(x_offset, y_offset + button_boxes_width)
                .size(button_boxes_width)
                .background(color = Color.White)
                .border(border = BorderStroke(2.dp, Color.Black))
                .clickable(onClick = {
                    wordfinder_display.intValue = 1
                })
        ) {
            Text(text = "Search", color = Color.Black, modifier = Modifier
                .align(Alignment.Center)
                .padding(3.dp))
        }
    }
    Column() {
        Box(
            modifier = Modifier
                .offset(x_offset - button_boxes_width, y_offset + button_boxes_width * 3)
                .width(button_boxes_width * 2)
                .height(button_boxes_width)
                .background(color = Color.White)
                .border(border = BorderStroke(2.dp, Color.Black))
                .clickable {
                    if (menu_history.isNotEmpty()) {
                        navigateTo(menu_history.removeAt(menu_history.lastIndex))
                    }
                }
        ) {
            Text(text = "Back", color = Color.Black, modifier = Modifier
                .align(Alignment.Center)
                .padding(3.dp))
        }
    }
    if (showKeyboard)
    {
        var typedText by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        fun submit() {
            val trimmed = typedText.trim()
            if (trimmed.isNotEmpty()) {
                inputboxselecteditems_text += trimmed
                inputboxselecteditems_has_symbol += false
            }
            showKeyboard = false
        }

        AlertDialog(
            onDismissRequest = { showKeyboard = false },
            title = { Text("Add text") },
            text = {
                TextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            },
            confirmButton = { Button(onClick = { submit() }) { Text("Add") } },
            dismissButton = { Button(onClick = { showKeyboard = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun EditorToolbar() {
    var exit_clicked by remember { mutableStateOf(false) }
    var unsaved by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(exit_clicked) {
        if (exit_clicked) unsaved = hasStateChanged()
    }
    // No changes -> just exit
    LaunchedEffect(unsaved) {
        if (exit_clicked && unsaved == false) {
            trigger_load.value = true
            editor_mode.value = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE082))
            .border(2.dp, Color(0xFFFF8F00))
            .statusBarsPadding()
            .height(48.dp)
            .zIndex(800f)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("EDITOR MODE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { show_add_item_dialog.value = true }) { Text("+ Item") }
        Button(onClick = { show_new_menu_dialog.value = true }) { Text("+ Menu") }
        Spacer(modifier = Modifier.width(20.dp))
        Button(onClick = { show_delete_menu_dialog.value = true }) { Text("- Menu") }
        Button(onClick = { show_goto_menu_dialog.value = true}) { Text("Go To Menu") }
        Spacer(modifier = Modifier.width(20.dp))
        Button(
            onClick = {
                trigger_save.value = true
                      },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
        ) { Text("Apply Changes") }
        Spacer(modifier = Modifier.width(20.dp))
        Button(
            onClick = { exit_clicked = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
        ) { Text("Exit") }
    }

    if (exit_clicked && unsaved == true) {
        AlertDialog(
            onDismissRequest = {
                exit_clicked = false
                unsaved = null
            },
            title = { Text("Unsaved Changes") },
            text = {
                Text(
                    "You have unsaved changes, do you want to save them?",
                    fontSize = 14.sp,
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(onClick = {
                    trigger_save.value = true
                    exit_clicked = false
                    unsaved = null
                    editor_mode.value = false
                }) { Text("Save Changes") }
            },
            dismissButton = {
                Button(onClick = {
                    trigger_load.value = true
                    exit_clicked = false
                    unsaved = null
                    editor_mode.value = false
                }) { Text("Don't Save") }
            }
        )
    }
}

@Composable
fun GotoMenuDialogue() {
    AlertDialog(
        onDismissRequest = { show_goto_menu_dialog.value = false },
        title = { Text("Go to a menu") },
        text = {
            Column(modifier = Modifier
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())) {
                Text(
                    "Lets you jump to a folder in editor mode.",
                    fontSize = 12.sp, color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                MenuList.forEach { menu ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(menu.title, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        Button(
                            onClick = {
                                linked_menu.value = menu.id
                                switchmenuparser.value++ },
                        ) { Text("Go To") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { show_goto_menu_dialog.value = false }) { Text("Done") }
        }
    )
}

@Composable
fun DeleteMenuDialog() {
    AlertDialog(
        onDismissRequest = { show_delete_menu_dialog.value = false },
        title = { Text("Delete a menu") },
        text = {
            Column(modifier = Modifier
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())) {
                Text(
                    "Folders that open a deleted menu will also be removed.",
                    fontSize = 12.sp, color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                MenuList.filter { it.id != 0 }.forEach { menu ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(menu.title, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        Button(
                            onClick = { deleteMenu(menu.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F))
                        ) { Text("Delete") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { show_delete_menu_dialog.value = false }) { Text("Done") }
        }
    )
}

fun deleteMenu(menuId: Int) {
    if (menuId == 0) return

    // Remove the menu
    MenuList.removeAll { it.id == menuId }

    // Any folder pointing at the deleted menu becomes a dead link which is then removed
    for (i in MenuList.indices) {
        val m = MenuList[i]
        val deadIndices = m.pointers.mapIndexedNotNull { idx, ptr ->
            if (ptr == menuId) idx else null
        }
        if (deadIndices.isNotEmpty()) {
            MenuList[i] = m.copy(
                item_list  = m.item_list.filterIndexed  { idx, _ -> idx !in deadIndices },
                pointers   = m.pointers.filterIndexed   { idx, _ -> idx !in deadIndices },
                tts        = m.tts.filterIndexed        { idx, _ -> idx !in deadIndices },
                item_type  = m.item_type.filterIndexed  { idx, _ -> idx !in deadIndices },
                image_urls = if (m.image_urls.size == m.item_list.size)
                    m.image_urls.filterIndexed { idx, _ -> idx !in deadIndices }
                else m.image_urls,
                item_uuids = if (m.item_uuids.size == m.item_list.size)
                    m.item_uuids.filterIndexed { idx, _ -> idx !in deadIndices }
                else m.item_uuids,
            )
        }
    }

    // Delete menu from menu row
    menu_terms_ids.remove(menuId)

    // Remove dangling pointers
    killDanglingPointers()

    // If user is on deleted menu, send them to the root menu.
    if (linked_menu.value == menuId) linked_menu.value = 0
    if (current_menu_id == menuId) current_menu_id = 0

    switchmenuparser.value++
}

fun killDanglingPointers() {
    val liveIds = MenuList.map { it.id }.toSet()
    for (i in MenuList.indices) {
        val m = MenuList[i]
        val dead = m.pointers.mapIndexedNotNull { idx, ptr ->
            if (ptr != null && ptr !in liveIds) idx else null
        }
        if (dead.isNotEmpty()) {
            fun <T> List<T>.filterDead() = if (size == m.item_list.size) filterIndexed { idx, _ -> idx !in dead } else this
            MenuList[i] = m.copy(
                item_list               = m.item_list.filterDead(),
                pointers                = m.pointers.filterDead(),
                tts                     = m.tts.filterDead(),
                item_type               = m.item_type.filterDead(),
                image_urls              = m.image_urls.filterDead(),
                item_uuids              = m.item_uuids.filterDead(),
                custom_image_paths      = m.custom_image_paths.filterDead(),
                custom_audio_paths      = m.custom_audio_paths.filterDead(),
                custom_audio_names      = m.custom_audio_names.filterDead(),
                pronunciation_overrides = m.pronunciation_overrides.filterDead()
            )
        }
    }
}

@Composable
fun EditItemDialog() {
    val menu = MenuFinder(edit_target_menu_id.intValue)
    val idx = edit_target_index.intValue
    if (idx < 0 || idx >= menu.item_list.size) {
        show_edit_item_dialog.value = false
        return
    }

    val platformContext = LocalPlatformContext.current
    var name by remember { mutableStateOf(menu.item_list[idx]) }
    val originalIsSymbol = menu.item_type[idx]
    var ttsType by remember { mutableIntStateOf(menu.tts[idx] ?: 2) }
    var currentCustomPath by remember {
        mutableStateOf(menu.custom_image_paths.getOrNull(idx) ?: "")
    }
    var currentAudioPath by remember {
        mutableStateOf(menu.custom_audio_paths.getOrNull(idx) ?: "")
    }
    var currentPronOverride by remember {
        mutableStateOf(menu.pronunciation_overrides.getOrNull(idx) ?: "")
    }
    var isRecording by remember { mutableStateOf(false) }
    @OptIn(ExperimentalUuidApi::class)
    val itemUuid by remember {
        mutableStateOf(
            menu.item_uuids.getOrNull(idx)?.takeIf { it.isNotBlank() }
                ?: Uuid.random().toString()
        )
    }
    val pickImage = rememberPlatformImagePicker(itemUuid) { path ->
        currentCustomPath = path
    }
    val pickAudio = rememberPlatformAudioPicker(itemUuid) { path ->
        currentAudioPath = path
    }

    // Commits dialog state into MenuList
    fun commitChanges() {
        val menuIndex = MenuList.indexOfFirst { it.id == menu.id }
        if (menuIndex >= 0) {
            val n = menu.item_list.size
            fun <T> MutableList<T>.padTo(value: T) { while (size < n) add(value) }

            val customPaths = menu.custom_image_paths.toMutableList().also { it.padTo(""); it[idx] = currentCustomPath }
            val audioPaths  = menu.custom_audio_paths.toMutableList().also { it.padTo(""); it[idx] = currentAudioPath }
            val audioNames  = menu.custom_audio_names.toMutableList().also { it.padTo("") }
            val pronOvr     = menu.pronunciation_overrides.toMutableList().also { it.padTo(""); it[idx] = currentPronOverride }
            println("SpeGen commitChanges idx=$idx customPath='$currentCustomPath' audioPath='$currentAudioPath' pron='$currentPronOverride'")
            val uuids = menu.item_uuids.toMutableList()
            @OptIn(ExperimentalUuidApi::class)
            while (uuids.size < n) uuids.add(Uuid.random().toString())
            uuids[idx] = itemUuid

            MenuList[menuIndex] = menu.copy(
                item_list               = menu.item_list.toMutableList().also { it[idx] = name.trim() },
                tts                     = if (originalIsSymbol) menu.tts.toMutableList().also { it[idx] = ttsType } else menu.tts,
                custom_image_paths      = customPaths,
                custom_audio_paths      = audioPaths,
                custom_audio_names      = audioNames,
                pronunciation_overrides = pronOvr,
                item_uuids              = uuids
            )
            switchmenuparser.value++
        }
    }

    val previewUrl = currentCustomPath.ifBlank { menu.image_urls.getOrNull(idx) ?: "" }
    var possible_images_count by remember { mutableIntStateOf(1) }
    var urls = remember { mutableStateListOf(previewUrl) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadMoreTrigger by remember { mutableIntStateOf(0) }
    var loadMore by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = {
            commitChanges()
            show_edit_item_dialog.value = false
        },
        title = { Text("Edit ${if (originalIsSymbol) "symbol" else "folder"}") },
        text = {
            Row {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text("Item Preview", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    image_preview(name, platformContext, previewUrl, true)
                }
                Column {
                    Text("Name", fontSize = 14.sp)
                    TextField(value = name, onValueChange = { name = it }, singleLine = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Image", fontSize = 14.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 300.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(urls.size) { index ->
                            val isSelected = urls[index] == currentCustomPath
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .border(
                                        if (isSelected) 4.dp else 2.dp,
                                        if (isSelected) Color(0xFF1976D2) else Color.Black,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        currentCustomPath = urls[index]
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(platformContext)
                                        .data(urls[index])
                                        .memoryCachePolicy(
                                            if (urls[index].startsWith("/")) CachePolicy.DISABLED
                                            else CachePolicy.ENABLED
                                        )
                                        .build(),
                                    contentDescription = name,
                                    modifier = Modifier.padding(4.dp).fillMaxSize()
                                )
                            }
                        }
                        if (loadMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(Color.White)
                                        .border(4.dp, Color.Black, RoundedCornerShape(20.dp))
                                        .clickable(enabled = !loadingMore) {
                                            loadMoreTrigger++
                                        }
                                ) {
                                    if (loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center).size(32.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Load More",
                                            modifier = Modifier.align(Alignment.Center)
                                                .padding(4.dp),
                                            textAlign = TextAlign.Center, fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    LaunchedEffect(loadMoreTrigger) {
                        if (loadMoreTrigger == 0) return@LaunchedEffect
                        loadingMore = true
                        val results = useApiMultipleWithToken(
                            accesstoken,
                            name,
                            9,
                            possible_images_count + 9
                            )
                        // Add only the newly fetched ones
                        for (i in possible_images_count until (results?.size ?: 0)) {
                            val url: String? = results?.get(i)?.image_url
                            if (url?.isNotBlank() == true && url !in urls) {
                                urls.add(url)
                            }
                        }
                        possible_images_count = urls.size
                        loadingMore = false
                        loadMore = false
                    }
                    Button(onClick = pickImage) {
                        Text("Choose custom image")
                    }
                    if (currentCustomPath.isNotBlank()) {
                        Button(
                            onClick = { currentCustomPath = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF757575))
                        ) { Text("Reset to default") }
                    }
                    if (originalIsSymbol) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("TTS behavior", fontSize = 14.sp)
                        Row {
                            listOf("Type only" to 0, "Speak only" to 1, "Both" to 2)
                                .forEach { (label, value) ->
                                    Row(
                                        modifier = Modifier
                                            .clickable { ttsType = value }.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (ttsType == value) "● $label" else "○ $label",
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                        }

                        // ---- Audio ----
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Custom audio", fontSize = 14.sp)
                        if (currentAudioPath.isNotBlank()) {
                            Text(
                                "Audio: ${currentAudioPath.substringAfterLast('/')}",
                                fontSize = 12.sp, color = Color.Gray
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { playAudioFile(currentAudioPath) }) {
                                    Text("Preview")
                                }
                                Button(
                                    onClick = { currentAudioPath = "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
                                ) { Text("Clear audio") }
                            }
                        } else {
                            Text("No custom audio", fontSize = 12.sp, color = Color.Gray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = pickAudio) { Text("Import audio") }
                            Button(
                                onClick = {
                                    if (isRecording) {
                                        val path = stopRecording()
                                        if (path.isNotBlank()) currentAudioPath = path
                                        isRecording = false
                                    } else {
                                        startRecording(itemUuid)
                                        isRecording = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) Color(0xFFD32F2F) else Color(0xFF388E3C)
                                )
                            ) { Text(if (isRecording) "Stop recording" else "Record") }
                        }

                        // ---- Pronunciation override ----
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Pronunciation override", fontSize = 14.sp)
                        Text(
                            "Leave blank to speak the item name. Fill in an alternate spelling the TTS engine pronounces correctly.",
                            fontSize = 11.sp, color = Color.Gray
                        )
                        TextField(
                            value = currentPronOverride,
                            onValueChange = { currentPronOverride = it },
                            singleLine = true,
                            placeholder = { Text("e.g. \"loo-goo-bree-us\"") }
                        )
                        if (currentPronOverride.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    ttsEngine.value?.speak(currentPronOverride, TtsEngine.QUEUE_FLUSH, "pron_preview")
                                }) { Text("Preview") }
                                Button(
                                    onClick = { currentPronOverride = "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
                                ) { Text("Clear") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                commitChanges()
                show_edit_item_dialog.value = false
            }) { Text("Done") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val menuIndex = MenuList.indexOfFirst { it.id == menu.id }
                        if (menuIndex >= 0) {
                            val updated = menu.copy(
                                item_list = menu.item_list.toMutableList().also { it.removeAt(idx) },
                                pointers = menu.pointers.toMutableList().also { it.removeAt(idx) },
                                tts = menu.tts.toMutableList().also { it.removeAt(idx) },
                                item_type = menu.item_type.toMutableList().also { it.removeAt(idx) }
                            )
                            MenuList[menuIndex] = updated
                            switchmenuparser.value++
                        }
                        show_edit_item_dialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Delete") }
            }
        }
    )
}

@Composable
fun image_preview(name: String, platformContext: PlatformContext, previewUrl: String, has_text: Boolean)
{
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color.White)
            .border(4.dp, Color.Black, RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(platformContext).data(previewUrl).build(),
            contentDescription = "Preview of $name",
            modifier = Modifier.padding(8.dp).fillMaxSize()
        )
        if (has_text) {
            Text(
                text = name.replaceFirstChar { it.titlecase() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp),
                textAlign = TextAlign.Center, fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AddItemDialog() {
    var name by remember { mutableStateOf("") }
    var isSymbol by remember { mutableStateOf(true) }
    var ttsType by remember { mutableIntStateOf(2) }
    var folderTarget by remember { mutableIntStateOf(2) }

    AlertDialog(
        onDismissRequest = { show_add_item_dialog.value = false },
        title = { Text("Add item to ${MenuFinder(current_menu_id).title}") },
        text = {
            Column {
                Text("Name", fontSize = 14.sp)
                TextField(value = name, onValueChange = { name = it }, singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Row(modifier = Modifier
                        .clickable { isSymbol = true }
                        .padding(8.dp)) {
                        Text(if (isSymbol) "● Symbol" else "○ Symbol")
                    }
                    Row(modifier = Modifier
                        .clickable { isSymbol = false }
                        .padding(8.dp)) {
                        Text(if (!isSymbol) "● Folder" else "○ Folder")
                    }
                }
                if (!isSymbol) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Folder target menu", fontSize = 14.sp)
                    Column(modifier = Modifier
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState())) {
                        MenuList.forEach { m ->
                            Row(modifier = Modifier
                                .clickable { folderTarget = m.id }
                                .padding(4.dp)) {
                                Text(if (folderTarget == m.id) "● ${m.title}" else "○ ${m.title}",
                                    fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val menuIndex = MenuList.indexOfFirst { it.id == current_menu_id }
                    if (menuIndex >= 0) {
                        val m = MenuList[menuIndex]
                        @OptIn(ExperimentalUuidApi::class)
                        val newUuid = Uuid.random().toString()
                        val updated = m.copy(
                            item_list = m.item_list + name.trim(),
                            pointers = m.pointers + (if (isSymbol) null else folderTarget),
                            tts = m.tts + (if (isSymbol) ttsType else null),
                            item_type = m.item_type + isSymbol,
                            item_uuids = m.item_uuids + newUuid
                        )
                        MenuList[menuIndex] = updated
                        switchmenuparser.value++
                    }
                }
                show_add_item_dialog.value = false
            }) { Text("Add") }
        },
        dismissButton = {
            Button(onClick = { show_add_item_dialog.value = false }) { Text("Cancel") }
        }
    )
}

@Composable
fun NewMenuDialog() {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { show_new_menu_dialog.value = false },
        title = { Text("Create new menu") },
        text = {
            Column {
                Text("Menu title", fontSize = 14.sp)
                TextField(value = title, onValueChange = { title = it }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                Text("The menu will be created with no items. You can add items by entering it and using '+ Item'.",
                    fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    val newId = (MenuList.maxOfOrNull { it.id } ?: 0) + 1
                    MenuList.add(menutemplate(
                        id = newId,
                        title = title.trim(),
                        item_list = emptyList(),
                        pointers = emptyList(),
                        tts = emptyList(),
                        item_type = emptyList()
                    ))
                    switchmenuparser.value++
                }
                show_new_menu_dialog.value = false
            }) { Text("Create") }
        },
        dismissButton = {
            Button(onClick = { show_new_menu_dialog.value = false }) { Text("Cancel") }
        }
    )
}

@Composable
fun EditorOverlay()
{
    val dim = Color.Gray.copy(alpha = 0.5f)

    // Covers inputbox
    Box(
        modifier = Modifier
            .offset(0.dp, WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .width(screenWidth)
            .height(input_box_height-WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .background(dim)
            .zIndex(700f)
            .clickable(onClick = {})
    )
    // Covers MenuRow and Static_Row_Needs
    Box(
        modifier = Modifier
            .offset(0.dp, input_box_height + menu_height)
            .width(screenWidth)
            .height(menu_static_row_height + static_row_height)
            .background(dim)
            .zIndex(700f)
            .clickable(onClick = {})
    )
    // Covers buttonboxes
    Box(
        modifier = Modifier
            .offset(screenWidth - (button_boxes_width * 2), input_box_height)
            .width(button_boxes_width * 2)
            .height(menu_height)
            .background(dim)
            .zIndex(700f)
            .clickable(onClick = {})
    )
    Box(
        modifier = Modifier
            .offset(0.dp, input_box_height)
            .width(screenWidth - (button_boxes_width * 2))
            .height(menu_height)
            .border(3.dp, Color(0xFFFF8F00))
            .zIndex(750f)
    )
}

/**
 * Shows predicted next words derived from the bigram model, replacing the symbol grid
 * when [show_autocomplete] is true. Each prediction displays its cached symbol image.
 */
@Composable
fun AutocompleteMenu() {
    val lastWord = inputboxselecteditems_text.lastOrNull()?.lowercase()?.trim() ?: ""
    val predictions = remember(lastWord, ngram_model.value) {
        ngram_model.value.bigrams[lastWord]
            ?.entries
            ?.sortedByDescending { it.value }
            ?.take(8)
            ?.map { it.key }
            ?: emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(2.dp, Color(0xFF90CAF9))
            .padding(8.dp)
    ) {
        if (predictions.isEmpty()) {
            Text(
                "No predictions yet — tap symbols to build your vocabulary.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(predictions.size) { i ->
                    val word = predictions[i]
                    val displayName = word.replaceFirstChar { it.titlecase() }
                    var imageUrl by remember(word) { mutableStateOf(findCachedUrl(word)) }
                    LaunchedEffect(word) {
                        if (imageUrl.isBlank()) {
                            imageUrl = useApiWithToken(accesstoken, word)?.image_url ?: ""
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(button_boxes_width * 1.2f)
                            .background(Color.White)
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                            .clickable {
                                val prevWord = inputboxselecteditems_text.lastOrNull()
                                inputboxselecteditems_text      += displayName
                                inputboxselecteditems_has_symbol += true
                                inputboxselecteditems_audio      += ""
                                inputboxselecteditems_pron       += ""
                                if (prevWord != null) updateNgramModel(prevWord, word)
                                ttsEngine.value?.speak(displayName, TtsEngine.QUEUE_FLUSH, "autocomplete")
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(imageUrl).build(),
                            contentDescription = displayName,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                        Text(
                            text = displayName,
                            fontSize = 10.sp,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(2.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Screen() {
    if (screen_display.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            val a = remember { mutableIntStateOf(0) }
            GetScreenDimensions()
            Static_Row_Needs()
            if (wordfinder_display.intValue != a.intValue) {
                WordFinder()
            } else {
                if (editor_mode.value) {
                    EditorOverlay()
                    EditorToolbar()
                }
                Buttonboxes()
                MenuRow(Modifier)
                InputBox(Modifier)
                if (show_autocomplete.value) {
                    AutocompleteMenu()
                } else {
                    Menu(Modifier)
                }
            }
        }
    }
    if (!screen_display.value) {
        screen_display.value = !screen_display.value
    }
}