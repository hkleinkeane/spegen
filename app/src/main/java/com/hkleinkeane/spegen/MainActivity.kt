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

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val DEMO_MODE = false

val Context.spegen_datastore by preferencesDataStore(name = "spegen_settings")
private val APP_STATE_KEY = stringPreferencesKey("app_state")

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

var tts: MutableState<TextToSpeech?> = mutableStateOf(null)

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

val item_sizes = mutableStateMapOf<String, IntSize>()

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
val ngram_model = mutableStateOf(seedNgramModel())
val show_autocomplete = mutableStateOf(false)
private var mediaPlayer: android.media.MediaPlayer? = null
private var recorder: android.media.MediaRecorder? = null
private var recordingPath: String = ""
var inputboxselecteditems_audio = mutableStateListOf<String>()
var inputboxselecteditems_pron = mutableStateListOf<String>()
private var seqPlayer: android.media.MediaPlayer? = null

val fitzgerald_overrides = mutableStateMapOf<String, String>()
val tutorial_scroll_to_index = mutableIntStateOf(-1)

class MainActivity : ComponentActivity(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        linked_menu.value = 0
        menu_history.clear()
        current_menu_id = 0
        runBlocking {
            val hadSave = loadAllPreferences(this@MainActivity)
            if (!hadSave) show_tutorial.value = true
        }
        setContent {
            tts = rememberTextToSpeech()
            MenuKeyGen()
            Screen()
            Box()
            {
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
                            val itemColor = menu.colors.getOrNull(index)?.toComposeColor() ?: Color.White
                            Surface(color = Color.Transparent) {
                                Folder(
                                    folder_name,
                                    folder_image_url,
                                    folder_menu!!,
                                    vertical_stretch,
                                    x_offset,
                                    y_offset,
                                    Modifier.zIndex(1000f),
                                    bgColor = itemColor
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
                        val itemColor = menu.colors.getOrNull(index)?.toComposeColor() ?: Color.White
                        Surface(color = Color.Transparent) {
                            Symbol(
                                symbol_name,
                                symbol_image_url,
                                vertical_stretch,
                                tts_type,
                                x_offset,
                                y_offset,
                                Modifier.zIndex(100f),
                                bgColor = itemColor
                            )
                        }
                    }
                }
            }
            if (show_tutorial.value)
            {
                TutorialOverlay(onFinish = {
                    tutorial_scroll_to_index.intValue = -1
                    show_tutorial.value = false
                    trigger_save.value = true
                })
            }
            if (show_settings.value) {
                SettingsScreen(this@MainActivity, onClose = {
                    show_settings.value = false
                })
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
                    !show_tutorial.value &&
                            isOnline(this@MainActivity) &&
                            hasUncachedImages(this@MainActivity)
            }
            if (show_cache_prompt.value) {
                CachePrompt(this@MainActivity)
            }
            LaunchedEffect(trigger_save.value) {
                if (trigger_save.value) {
                    try {
                        saveAllPreferences(this@MainActivity)
                    } finally {
                        cleanOrphanedCustomImages(this@MainActivity)
                        trigger_save.value = false
                    }
                }
            }
            LaunchedEffect(trigger_load.value) {
                if (trigger_load.value) {
                    try {
                        loadAllPreferences(this@MainActivity)
                    } finally {
                        cleanOrphanedCustomImages(this@MainActivity)
                        switchmenuparser.value++
                        trigger_load.value = false
                    }
                }
            }
            LaunchedEffect(trigger_state_change_check.value) {
                if (trigger_state_change_check.value) {
                    state_has_changed.value = hasStateChanged(this@MainActivity)
                    trigger_state_change_check.value = false
                }
            }
        }
    }
    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            saveAllPreferences(this@MainActivity)
        }
    }
    // Detect when user hits the home button, meant for use with kiosk demo mode only
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (DEMO_MODE) {
            resetToDefaults()
        }
    }
}

fun resetToDefaults() {
    // Clear composed sentence
    inputboxselecteditems_text.clear()
    inputboxselecteditems_has_symbol.clear()
    inputboxselecteditems_audio.clear()
    inputboxselecteditems_pron.clear()

    // Clear all wordfinder state
    wordfinder_display.intValue = 0
    wordfinder_display_buttonguide.intValue = 0
    wordfinder_path_ids.clear()
    wordfinder_path_names.clear()
    createclonefolder.value = false
    createclonesymbol.value = false
    wordfinder_highlight_index.intValue = -1

    // Clear custom Fitzgerald overrides
    fitzgerald_overrides.clear()

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
    show_autocomplete.value = false

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

    // Refresh screen
    screen_display.value = false

    // Reset row variables
    static_row_text_size.floatValue = 16f
    static_row_text_padding.floatValue = 4f
    menu_row_text_size.floatValue = 16f
    menu_row_text_padding.floatValue = 4f
}

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
    val ngram_model: NgramModel = NgramModel(),
    val fitzgerald_overrides: Map<String, String> = emptyMap(),
    val fitzgeraldKey: List<FitzgeraldCategory> = emptyList()
)

fun PersistedState.withPaddedLists(): PersistedState = copy(
    menu_list = menu_list.map { menu ->
        val n = menu.item_list.size
        val uuids = menu.item_uuids.toMutableList()
        val urls  = menu.image_urls.toMutableList()
        val custom = menu.custom_image_paths.toMutableList()
        val custom_audio_paths = menu.custom_audio_paths.toMutableList()
        val custom_audio_names = menu.custom_audio_names.toMutableList()
        val pronunciation_overrides = menu.pronunciation_overrides.toMutableList()
        val colors = menu.colors.toMutableList()
        while (colors.size < n) colors.add("")
        while (uuids.size  < n) uuids.add(java.util.UUID.randomUUID().toString())
        while (urls.size   < n) urls.add("")
        while (custom.size < n) custom.add("")
        while (custom_audio_paths.size < n) custom_audio_paths.add("")
        while (custom_audio_names.size < n) custom_audio_names.add("")
        while (pronunciation_overrides.size < n) pronunciation_overrides.add("")
        menu.copy(item_uuids = uuids, image_urls = urls, custom_image_paths = custom, custom_audio_paths = custom_audio_paths, custom_audio_names = custom_audio_names, pronunciation_overrides = pronunciation_overrides, colors = colors)
    }
)

fun load_vars(state: PersistedState) {
    box_size = state.box_size_dp.dp
    box_padding = state.box_padding_dp.dp
    input_box_height = state.input_box_height_dp.dp
    item_text_padding = state.item_text_padding_dp.dp
    tts_data_found.value = state.tts_data_found
    ngram_model.value =
        if (state.ngram_model.bigrams.isEmpty()) seedNgramModel()
        else state.ngram_model

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

    if (state.fitzgeraldKey.isNotEmpty()) {
        fitzgeraldKey.clear()
        fitzgeraldKey.addAll(state.fitzgeraldKey)
    }

    fitzgerald_overrides.clear()
    fitzgerald_overrides.putAll(state.fitzgerald_overrides)

    MenuList.clear()
    MenuList.addAll(state.menu_list)
}

suspend fun loadAllPreferences(context: Context): Boolean {
    val prefs = context.spegen_datastore.data.first()
    val json = prefs[APP_STATE_KEY] ?: return false // Fresh install
    val jsonignoreunknownkeys = Json { ignoreUnknownKeys = true }
    val state = try {
        jsonignoreunknownkeys.decodeFromString<PersistedState>(json).withPaddedLists()
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
    ngram_model = ngram_model.value,
    fitzgerald_overrides = fitzgerald_overrides.toMap(),
    fitzgeraldKey = fitzgeraldKey
)

suspend fun saveAllPreferences(context: Context) {
    killDanglingPointers()
    context.spegen_datastore.edit { prefs ->
        prefs[APP_STATE_KEY] = Json.encodeToString(currentPersistedState().withPaddedLists())
    }
}

fun cleanOrphanedCustomImages(context: Context) {
    val imageDir = File(context.filesDir, "custom_images")
    if (!imageDir.exists()) return

    val referenced = MenuList
        .flatMap { it.custom_image_paths }
        .filter { it.isNotBlank() }
        .map { File(it).name }
        .toSet()

    imageDir.listFiles()?.forEach { file ->
        if (file.name !in referenced) {
            file.delete()
        }
    }
}

fun menutemplate.displayUrl(idx: Int): String {
    val custom = custom_image_paths.getOrNull(idx)
    if (!custom.isNullOrBlank()) return custom
    return image_urls.getOrNull(idx) ?: ""
}

fun PersistedState.normalizedForComparison() = copy(
    menu_list = menu_list.map { it.copy(image_urls = emptyList()) },
    ngram_model = NgramModel()
)
suspend fun hasStateChanged(context: Context): Boolean {
    val prefs = context.spegen_datastore.data.first()
    val savedJson = prefs[APP_STATE_KEY] ?: return true
    val savedState = try {
        Json.decodeFromString<PersistedState>(savedJson)
    } catch (e: Exception) { return true }

    return currentPersistedState().normalizedForComparison() != savedState.normalizedForComparison()
}

suspend fun precacheAllImages(context: Context) {
    if (cache_running.value) return

    val urls = MenuList
        .flatMap { it.image_urls }
        .filter { it.isNotBlank() }
        .distinct()

    if (urls.isEmpty()) return

    cache_running.value = true
    cache_progress.value = 0        // always reset before starting
    cache_total.value = urls.size

    val loader = SingletonImageLoader.get(context)

    for (url in urls) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        loader.execute(request)
        cache_progress.value += 1
    }

    cache_running.value = false
}

suspend fun resolveAndPrecacheAll(context: Context) {
    if (cache_running.value) return
    getAccessToken()
    // Resolve URLs for any menu that doesn't have a full set yet
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
    saveAllPreferences(context)
    precacheAllImages(context)
}

suspend fun hasUncachedImages(context: Context): Boolean = withContext(Dispatchers.IO) {
    val loader = SingletonImageLoader.get(context)
    val disk = loader.diskCache ?: return@withContext false
    for (menu in MenuList) {
        // URLs never fully resolved for this menu (fresh install / partial)
        if (menu.image_urls.size != menu.item_list.size) return@withContext true
        for (url in menu.image_urls) {
            if (url.isBlank()) continue // resolved to nothing
            val snapshot = disk.openSnapshot(url)
            if (snapshot == null) return@withContext true // resolved but not on disk
            snapshot.close()
        }
    }
    false
}

fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
fun CachePrompt(context: Context) {
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current as? ComponentActivity
    var show_error_msg by remember { mutableStateOf(false) }

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
                if (show_error_msg)
                {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp))
                    {
                        Text(
                            "SpeGen found symbols that haven't been downloaded to this " +
                                    "device yet. Saving them now lets the app work fully without " +
                                    "an internet connection. This may take a moment.\n\n" +
                                    "You can also do this any time from Settings.",
                            fontSize = 14.sp
                        )
                        Text(
                            "Error: Not connected to the internet!",
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                    }
                }
                else {
                    Text(
                        "SpeGen found symbols that haven't been downloaded to this " +
                                "device yet. Saving them now lets the app work fully without " +
                                "an internet connection. This may take a moment.\n\n" +
                                "You can also do this any time from Settings.",
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            // Buttons only exist before caching starts
            if (!cache_running.value) {
                Button(onClick = {
                    if (isOnline(context)) {
                        show_error_msg = false
                        scope.launch {
                            resolveAndPrecacheAll(context)
                            show_cache_prompt.value = false // auto-close when done
                        }
                    }
                    else {
                        show_error_msg = true
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

fun copyImageToPrivateStorage(context: Context, uri: Uri, itemKey: String): String {
    val dir = File(context.filesDir, "custom_images").also { it.mkdirs() }
    val dest = File(dir, "$itemKey.webp")

    // Decode to bitmap then re-encode as WebP - best for image storage efficiency
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }
    } else {
        @Suppress("DEPRECATION")
        android.graphics.BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(uri)
        )
    }

    bitmap?.let {
        dest.outputStream().use { out ->
            it.compress(Bitmap.CompressFormat.WEBP, 90, out)
        }
    }
    return dest.absolutePath
}

fun exportToZip(context: Context, outputUri: Uri) {
    val audioDir = File(context.filesDir, "custom_audio")
    val imageDir = File(context.filesDir, "custom_images")
    val exportState = currentPersistedState().copy(
        menu_list = MenuList.map { menu ->
            menu.copy(
                custom_image_paths = menu.custom_image_paths.map { path ->
                    if (path.isNotBlank() && File(path).exists())
                        "custom_images/${File(path).name}"
                    else path
                },
                custom_audio_paths = menu.custom_audio_paths.map { path ->
                    if (path.isNotBlank() && File(path).exists())
                        "custom_audio/${File(path).name}"
                    else path
                }
            )
        }
    )
    context.contentResolver.openOutputStream(outputUri)?.use { out ->
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("state.json"))
            zip.write(Json.encodeToString(exportState).toByteArray())
            zip.closeEntry()

            imageDir.listFiles()?.forEach { f ->
                zip.putNextEntry(ZipEntry("custom_images/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            audioDir.listFiles()?.forEach { f ->
                zip.putNextEntry(ZipEntry("custom_audio/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}

fun importFromZip(context: Context, inputUri: Uri) {
    val imageDir = File(context.filesDir, "custom_images").also { it.mkdirs() }
    val audioDir = File(context.filesDir, "custom_audio").also { it.mkdirs() }

    val input = context.contentResolver.openInputStream(inputUri)
        ?: throw Exception("Could not open the selected file.")

    input.use { inStream ->
        ZipInputStream(inStream.buffered()).use { zip ->
            var loadedState: PersistedState? = null
            val lenient = Json { ignoreUnknownKeys = true }

            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "state.json" -> {
                        loadedState = lenient.decodeFromString<PersistedState>(
                            zip.readBytes().decodeToString()
                        )
                    }
                    entry.name.startsWith("custom_images/") -> {
                        val fileName = entry.name.removePrefix("custom_images/")
                        if (fileName.isNotBlank()) {
                            File(imageDir, fileName).outputStream()
                                .use { zip.copyTo(it) }
                        }
                    }
                    entry.name.startsWith("custom_audio/") -> {
                        val fileName = entry.name.removePrefix("custom_audio/")
                        if (fileName.isNotBlank()) {
                            File(audioDir, fileName).outputStream()
                                .use { zip.copyTo(it) }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            val state = loadedState
                ?: throw Exception("Backup file has no state.json — not a valid SpeGen backup.")

            val padded = state.withPaddedLists()
            val resolved = padded.copy(
                menu_list = padded.menu_list.map { menu ->
                    menu.copy(
                        custom_image_paths = menu.custom_image_paths.map { path ->
                            if (path.startsWith("custom_images/")) {
                                val dest = File(imageDir, path.removePrefix("custom_images/"))
                                if (dest.exists()) dest.absolutePath else ""
                            } else path
                        },
                        custom_audio_paths = menu.custom_audio_paths.map { path ->
                            if (path.startsWith("custom_audio/")) {
                                val dest = File(audioDir, path.removePrefix("custom_audio/"))
                                if (dest.exists()) dest.absolutePath else ""
                            } else path
                        }
                    )
                }
            )
            load_vars(resolved)
            linked_menu.value = 0
            switchmenuparser.value++
        }
    }
}

@Composable
fun GetScreenDimensions() {
    // Function that gets the dimensions of the screen for later use in UI scaling
    var configuration = LocalConfiguration.current
    screenWidth = configuration.screenWidthDp.dp
    screenHeight = configuration.screenHeightDp.dp
    configuration = LocalConfiguration.current
    isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    static_row_height = screenHeight * (1f / 8f) // Fraction deterxmined by base value of 70.dp then converted to fraction and applied to screen height to (hopefully) make box height scale with screen height
    menu_static_row_height = screenHeight * (1f / 8f) // Fraction determined by base value of 70.dp then converted to fraction and applied to screen height to (hopefully) make box height scale with screen height
    button_boxes_width = (screenHeight.value * (1f/8f)).dp
}

@Serializable
data class NgramModel(
    // last word -> (next word -> count)
    val bigrams: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
) {
    fun record(sentence: List<String>) {
        for (i in 0 until sentence.size - 1) {
            val cur = sentence[i].lowercase().trim()
            val next = sentence[i + 1].lowercase().trim()
            if (cur.isBlank() || next.isBlank()) continue
            val row = bigrams.getOrPut(cur) { mutableMapOf() }
            row[next] = (row[next] ?: 0) + 1
        }
    }

    fun predict(lastWord: String, limit: Int = 8): List<String> {
        val row = bigrams[lastWord.lowercase().trim()] ?: return emptyList()
        return row.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }
}

fun seedNgramModel(): NgramModel {
    // Hardcoded NgramModel for usage with demo board/if NgramModel doesn't exist
    val m = NgramModel()
    val seedSentences = listOf(
        // wants & needs
        "i want more", "i want to play", "i want to go", "i want to eat",
        "i want to watch", "i want that", "i want it", "i want a snack",
        "i need help", "i need to go", "i need a break", "i need water",
        "i need more time", "i would like more", "i would like to play",
        // feelings
        "i feel happy", "i feel sad", "i feel sick", "i feel tired",
        "i feel scared", "i feel hungry", "i feel angry", "i feel good",
        "i am happy", "i am sad", "i am tired", "i am hungry", "i am done",
        "i am okay", "i am not okay", "i am excited",
        // likes
        "i like that", "i like it", "i like this", "i like you",
        "i do not like that", "i do not want that", "i do not know",
        // actions
        "let us go", "let us play", "go home", "go outside", "go to bed",
        "come here", "stop it", "stop please", "all done", "more please",
        "play with me", "read a book", "watch a show", "listen to music",
        // social
        "thank you", "thank you very much", "i love you", "good morning",
        "good night", "see you later", "how are you", "i am fine",
        "yes please", "no thank you", "excuse me", "i am sorry",
        // food & drink
        "i want water", "i want milk", "i want a cookie", "i want pizza",
        "more food please", "i am thirsty", "i am still hungry",
        // questions
        "can i have more", "can i go", "can we play", "what is that",
        "where is it", "i want to know", "help me please",
        // people
        "where is mom", "where is dad", "i want mom", "i want dad",
        "my turn", "your turn", "with my friend"
    )
    seedSentences.forEach { sentence ->
        m.record(sentence.split(" "))
    }
    return m
}

@Composable
fun rememberTextToSpeech(): MutableState<TextToSpeech?> {
    // Handles TTS and its properties
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.value?.setLanguage(Locale.getDefault())
                if ((result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) && !tts_data_found.value)
                {
                    val installIntent = Intent().apply {
                        action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(installIntent)
                }
                if (!(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED))
                {
                    tts_data_found.value = true
                }
                tts.value?.setSpeechRate(tts_speech_rate.value)
                tts.value?.setPitch(tts_pitch.value)
            }
        }

        tts.value = textToSpeech

        onDispose {
            textToSpeech.stop()
        }
    }
    return tts
}

data class AccessTokenResponse(
    // Data class for getAccessToken to allow to parse the response data
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
    // Gets a new access token using the shared secret
    return withContext(Dispatchers.IO) {
        val params = listOf(
            "secret" to CLIENT_SECRET
        )
        val (_, _, result) = Fuel.post("https://www.opensymbols.org/api/v2/token", params)
            .responseObject<AccessTokenResponse>()

        when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                println("Failed to get access token: ${ex.message}")
                null
            }

            is Result.Success -> {
                val tokenResponse = result.get()
                accesstoken = tokenResponse.access_token
                tokenResponse
            }
        }
    }
}


suspend fun useApiWithToken(token: String?, search: String): ApiSymbolResponse? {
    return withContext(Dispatchers.IO) {
        val params = listOf(
            "q" to search,
            "locale" to "en",
            "safe" to "0",
            "access_token" to token
        )

        val (_, _, result) = Fuel.get("https://www.opensymbols.org/api/v2/symbols", params)
            .responseString()

        when (result) {
            is Result.Failure -> {
                println("API call failed: ${result.getException().message}")
                null
            }
            is Result.Success -> {
                var symbolstring = (result.get()).replace("[", "").replace("]", "").split("},")[0]

                if (symbolstring.length > 1) {
                    symbolstring += "}"
                }

                if (symbolstring.contains("}")) {
                    // Clean up the string to ensure valid JSON for a single object
                    symbolstring = symbolstring.dropLast(symbolstring.count { it == '}' } - 1)

                    // Return the decoded object
                    Json.decodeFromString<ApiSymbolResponse>(symbolstring)
                } else {
                    null // Return null if no valid symbol found
                }
            }
        }
    }
}

suspend fun useApiMultipleWithToken(token: String?, search: String, count: Int, index: Int): List<ApiSymbolResponse>? {
    return withContext(Dispatchers.IO) {
        val params = listOf(
            "q" to search,
            "locale" to "en",
            "safe" to "0",
            "access_token" to token
        )

        var listofsymbols = mutableListOf<ApiSymbolResponse>()

        val (_, _, result) = Fuel.get("https://www.opensymbols.org/api/v2/symbols", params)
            .responseString()

        when (result) {
            is Result.Failure -> {
                println("API call failed: ${result.getException().message}")
                null
            }
            is Result.Success -> {
                for (i in 0 until count) {
                    var symbolstring = (result.get()).replace("[", "").replace("]", "").split("},")[i+index]
                    if (symbolstring.length > 1) {
                        symbolstring += "}"
                    }
                    if (symbolstring.contains("}")) {
                        // Clean up the string to ensure valid JSON for a single object
                        symbolstring = symbolstring.dropLast(symbolstring.count { it == '}' } - 1)

                        // Return the decoded object
                        listofsymbols += Json.decodeFromString<ApiSymbolResponse>(symbolstring)
                    } else {
                        null // Return null if no valid symbol found
                    }
                }
                listofsymbols
            }
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

fun copyAudioToStorage(context: Context, uri: Uri, itemKey: String): String {
    return try {
        val dir = File(context.filesDir, "custom_audio").also { it.mkdirs() }
        // keep original extension; don't transcode audio
        val ext = context.contentResolver.getType(uri)
            ?.substringAfter("/") ?: "m4a"
        val dest = File(dir, "$itemKey.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: return ""
        dest.absolutePath
    } catch (e: Exception) {
        println("copyAudioToStorage failed: ${e.message}")
        ""
    }
}

fun playAudioFile(path: String) {
    try {
        mediaPlayer?.release()
        mediaPlayer = android.media.MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
        }
    } catch (e: Exception) {
        println("playAudioFile failed: ${e.message}")
    }
}

fun startRecording(context: Context, itemKey: String) {
    try {
        val dir = File(context.filesDir, "custom_audio").also { it.mkdirs() }
        val dest = File(dir, "$itemKey.m4a")
        recordingPath = dest.absolutePath
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            android.media.MediaRecorder(context)
        else
            @Suppress("DEPRECATION") android.media.MediaRecorder()
                ).apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingPath)
                prepare()
                start()
            }
    } catch (e: Exception) {
        println("startRecording failed: ${e.message}")
        recordingPath = ""
    }
}

fun stopRecording(): String {
    return try {
        recorder?.apply { stop(); release() }
        recorder = null
        recordingPath
    } catch (e: Exception) {
        println("stopRecording failed: ${e.message}")
        ""
    }
}

fun speakItem(menuId: Int?, itemIndex: Int?, fallbackName: String) {
    val menu = MenuFinder(menuId)
    val audioPath = menu.custom_audio_paths.getOrNull(itemIndex ?: -1) ?: ""
    if (audioPath.isNotBlank()) {
        playAudioFile(audioPath)
        return
    }
    val pron = menu.pronunciation_overrides.getOrNull(itemIndex ?: -1) ?: ""
    val toSpeak = pron.ifBlank { fallbackName }

    if (tts.value?.isSpeaking == true) tts.value?.stop()

    if (tts_pause_between_words.value) {
        val words = toSpeak.split(" ")
        tts.value?.speak(words[0], TextToSpeech.QUEUE_FLUSH, null, "word_0")
        for (i in 1 until words.size) {
            tts.value?.playSilentUtterance(
                tts_pause_duration.value, TextToSpeech.QUEUE_ADD, "pause_$i"
            )
            tts.value?.speak(words[i], TextToSpeech.QUEUE_ADD, null, "word_$i")
        }
    } else {
        tts.value?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "")
    }
}
fun playSentenceSequenced(
    tts: TextToSpeech?,
    words: List<String>,
    audioPaths: List<String>,
    pauseMs: Long,
    onFinished: () -> Unit = {}
) {
    if (words.isEmpty()) { onFinished(); return }

    tts?.stop()
    seqPlayer?.release()
    seqPlayer = null

    var i = 0

    // forward declaration so the callbacks can call playNext()
    lateinit var playNext: () -> Unit

    val ttsListener = object : android.speech.tts.UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onError(utteranceId: String?) {
            i++; playNext()
        }
        override fun onDone(utteranceId: String?) {
            i++
            android.os.Handler(android.os.Looper.getMainLooper()).post { playNext() }
        }
    }
    tts?.setOnUtteranceProgressListener(ttsListener)

    playNext = {
        if (i >= words.size) {
            seqPlayer?.release()
            seqPlayer = null
            onFinished()
        } else {
            val audio = audioPaths.getOrNull(i) ?: ""
            if (audio.isNotBlank()) {
                try {
                    seqPlayer?.release()
                    seqPlayer = android.media.MediaPlayer().apply {
                        setDataSource(audio)
                        setOnCompletionListener {
                            i++
                            playNext()
                        }
                        setOnErrorListener { _, _, _ ->
                            i++; playNext(); true
                        }
                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    println("sequencer audio failed at $i: ${e.message}")
                    i++; playNext()
                }
            } else {
                tts?.speak(
                    words[i],
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "seq_$i"
                )
            }
        }
    }

    playNext()
}

fun stopSentenceSequenced(tts: TextToSpeech?) {
    tts?.stop()
    seqPlayer?.release()
    seqPlayer = null
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

fun Modifier.verticalScrollbar(
    state: ScrollState,
    color: Color = Color.DarkGray,
    width: Dp = 4.dp,
    minThumb: Dp = 24.dp
): Modifier = drawWithContent {
    drawContent()
    val max = state.maxValue.toFloat()
    if (max <= 0f) return@drawWithContent
    val viewport = size.height
    val total = viewport + max
    val thumbH = ((viewport * viewport) / total).coerceAtLeast(minThumb.toPx())
    val thumbY = (state.value / max) * (viewport - thumbH)
    drawRoundRect(
        color = color.copy(alpha = 0.6f),
        topLeft = Offset(size.width - width.toPx() - 2.dp.toPx(), thumbY),
        size = Size(width.toPx(), thumbH),
        cornerRadius = CornerRadius(width.toPx() / 2f)
    )
}

fun Modifier.horizontalScrollbar(
    state: ScrollState,
    color: Color = Color.DarkGray,
    height: Dp = 4.dp,
    minThumb: Dp = 24.dp
): Modifier = drawWithContent {
    drawContent()
    val max = state.maxValue.toFloat()
    if (max <= 0f) return@drawWithContent
    val viewport = size.width
    val total = viewport + max
    val thumbW = ((viewport * viewport) / total).coerceAtLeast(minThumb.toPx())
    val thumbX = (state.value / max) * (viewport - thumbW)
    drawRoundRect(
        color = color.copy(alpha = 0.6f),
        topLeft = Offset(thumbX, size.height - height.toPx() - 2.dp.toPx()),
        size = Size(thumbW, height.toPx()),
        cornerRadius = CornerRadius(height.toPx() / 2f)
    )
}

fun navigateTo(menuId: Int) {
    menu_history.add(linked_menu.value)
    linked_menu.value = menuId
    switchmenuparser.value++
}

data class TutorialHighlight(val x: Dp, val y: Dp, val width: Dp, val height: Dp)

data class TutorialSlide(
      val title: String,
      val body: String,
      val highlight: TutorialHighlight? = null
)

@Composable
fun TutorialOverlay(onFinish: () -> Unit) {
      val density = LocalDensity.current

    fun findFirstItemKey(isSymbol: Boolean): String? {
        val home = MenuFinder(0)
        for (i in home.item_list.indices)
        {
            if (home.item_type[i] == isSymbol) return "0-$i"
        }
        return null
    }

      fun itemHighlight(isSymbol: Boolean): TutorialHighlight? {
            val key = findFirstItemKey(isSymbol) ?: return null
            val pos = item_positions[key] ?: return null
          val sz = item_sizes[key]
            if (pos != null) {
              return with(density) {
                TutorialHighlight(
                  x = pos.x.toDp(),
                  y = pos.y.toDp(),
                  width = sz?.width?.toDp() ?: box_size,
                  height = sz?.height?.toDp() ?: (box_size + box_padding*3)
                )
              }
            }
            return TutorialHighlight(0.dp, input_box_height, menu_width, menu_height)
          }

    data class TutorialSlide(
        val title: String,
        val body: String,
        val highlight: TutorialHighlight? = null,
        val lookupSymbol: Boolean? = null
    )

      val xRight = screenWidth - button_boxes_width
      val xLeft = xRight - button_boxes_width

      val slides = remember(screenWidth, screenHeight, button_boxes_width,
                 input_box_height, menu_static_row_height, static_row_height) {
            listOf(
              TutorialSlide("Welcome to SpeGen",
                "This is a short tutorial to teach you the basic UI of the app. " +
                "You can access this tutorial later from settings."),
              TutorialSlide("Folders",
                "Tap a folder to go to another menu with more " +
                "symbols and folders. Folders always have a black fold in the top right corner.",
                lookupSymbol = false),
              TutorialSlide("Symbols",
                "Tap a symbol to add it to the input box.",
                lookupSymbol = true),
              TutorialSlide("Input Box",
                "The input box is where you compose sentences. Tap it to play the constructed sentence.",
                TutorialHighlight(0.dp, 0.dp, screenWidth - button_boxes_width * 2, input_box_height)),
              TutorialSlide("Menu Row",
                "These are a list of buttons that will automatically redirect you to the associated menu.",
                TutorialHighlight(0.dp,
                  screenHeight - static_row_height - menu_static_row_height,
                  screenWidth, menu_static_row_height)),
              TutorialSlide("Static Words Row",
                "These are a list of terms that are always at the bottom of the screen regardless " +
                "of where you are in the application. Tap it to instantly play the word using text-to-speech.",
                TutorialHighlight(0.dp, screenHeight - static_row_height, screenWidth, static_row_height)),
              TutorialSlide("Keyboard",
                "Opens up a dialog that lets you use your device's keyboard to add items to the input box.",
                TutorialHighlight(xLeft, 0.dp, button_boxes_width, button_boxes_width)),
              TutorialSlide("Delete",
                "Deletes the last term in the input box.",
                TutorialHighlight(xLeft, button_boxes_width, button_boxes_width, button_boxes_width)),
              TutorialSlide("Clear",
                "Clears the input box of all terms.",
                TutorialHighlight(xLeft, button_boxes_width * 2, button_boxes_width, button_boxes_width)),
              TutorialSlide("Stop",
                "Stops any currently playing text-to-speech.",
                TutorialHighlight(xRight, button_boxes_width * 2, button_boxes_width, button_boxes_width)),
              TutorialSlide("Search",
                "Search for any terms in any existing menus. It guides you to the term by showing " +
                "you what buttons you need to press to get to the term.",
                TutorialHighlight(xRight, button_boxes_width, button_boxes_width, button_boxes_width)),
              TutorialSlide("Settings",
                "Brings you to a settings menu. Links to various options such as: editing UI, " +
                "backing up settings, or changing the text-to-speech voice settings, and more.",
                TutorialHighlight(xRight, 0.dp, button_boxes_width, button_boxes_width)),
              TutorialSlide("Back",
                "Sends you back to the previous menu you were on.",
                TutorialHighlight(xLeft, button_boxes_width * 3, button_boxes_width * 2, button_boxes_width)),
              TutorialSlide("Autocomplete",
                "Replaces the menu and opens a list of autocomplete options. Autocomplete learns " +
                "from the patterns in symbols you select to get more accurate and helpful over time. " +
                "Data is not sent off of your device without your consent.",
                TutorialHighlight(xLeft, button_boxes_width * 4, button_boxes_width * 2,
                  screenHeight - (button_boxes_width * 4) - menu_static_row_height - static_row_height))
            )
          }

      var currentSlide by remember { mutableIntStateOf(0) }
      val maxSlide = slides.size - 1
      val current = slides[currentSlide]
      val highlight = when (val s = current.lookupSymbol) {
          null -> current.highlight
          else -> itemHighlight(s) ?: TutorialHighlight(0.dp, input_box_height, menu_width, menu_height)
      }

      val cardWidth = (screenWidth * 0.85f).coerceAtMost(420.dp)
      val cardEstimatedHeight = 220.dp
      val (cardX, cardY) = remember(highlight, currentSlide) {
            if (highlight == null) {
              (screenWidth - cardWidth) / 2 to (screenHeight - cardEstimatedHeight) / 2
            } else {
              val belowY = highlight.y + highlight.height + 16.dp
              val aboveY = highlight.y - cardEstimatedHeight - 16.dp
              val y = when {
                belowY + cardEstimatedHeight < screenHeight - 16.dp -> belowY
                aboveY >= 16.dp -> aboveY
                else -> (screenHeight - cardEstimatedHeight) / 2
              }
              val rawX = (highlight.x + highlight.width / 2) - cardWidth / 2
              val x = rawX.coerceIn(8.dp, screenWidth - cardWidth - 8.dp)
              x to y
            }
          }

    LaunchedEffect(currentSlide) {
        val target = when (current.lookupSymbol)
        {
            false -> findFirstItemKey(false)?.removePrefix("0-")?.toIntOrNull() ?: -1
            true -> findFirstItemKey(true)?.removePrefix("0-")?.toIntOrNull() ?: -1
            else -> -1
        }
        tutorial_scroll_to_index.intValue = target
    }

      Box(modifier = Modifier.fillMaxSize().zIndex(2000f)) {
            val dim = Color.Black.copy(alpha = 0.80f)
            if (highlight == null) {
              Box(modifier = Modifier.fillMaxSize().background(dim).clickable(onClick = {}))
            } else {
              // Top strip
              Box(Modifier.offset(0.dp, 0.dp).width(screenWidth).height(highlight.y)
                .background(dim).clickable(onClick = {}))
              // Bottom strip
              Box(Modifier.offset(0.dp, highlight.y + highlight.height).width(screenWidth)
                .height(screenHeight - (highlight.y + highlight.height))
                .background(dim).clickable(onClick = {}))
              // Left strip
              Box(Modifier.offset(0.dp, highlight.y).width(highlight.x).height(highlight.height)
                .background(dim).clickable(onClick = {}))
              // Right strip
              Box(Modifier.offset(highlight.x + highlight.width, highlight.y)
                .width(screenWidth - (highlight.x + highlight.width)).height(highlight.height)
                .background(dim).clickable(onClick = {}))
              // Ring
                Box(Modifier.offset(highlight.x, highlight.y).width(highlight.width).height(highlight.height)
                .border(4.dp, Color(0xFFFFCC02), RoundedCornerShape(8.dp)))
                Box(Modifier.offset(highlight.x, highlight.y).width(highlight.width).height(highlight.height).clickable(onClick = {}))
            }

            Box(modifier = Modifier.offset(cardX, cardY).width(cardWidth)) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(2.dp, Color(0xFFFFCC02))
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(current.title, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
                  Spacer(Modifier.height(8.dp))
                  Text(current.body, fontSize = 14.sp, textAlign = TextAlign.Center)
                  Spacer(Modifier.height(12.dp))

                  // Progress dots
                  Row(horizontalArrangement = Arrangement.Center) {
                    repeat(slides.size) { i ->
                      Box(modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (i == currentSlide) 8.dp else 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i == currentSlide) Color.Black
                              else Color.Gray.copy(alpha = 0.4f)))
                    }
                  }
                  Spacer(Modifier.height(12.dp))

                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentSlide > 0) {
                      Button(
                        onClick = { currentSlide -= 1 },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                      ) { Text("Back") }
                    }
                    Button(
                      onClick = {
                        if (currentSlide < maxSlide) currentSlide += 1 else onFinish()
                      },
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                      Text(if (currentSlide < maxSlide) "Next" else "Done")
                    }
                    if (currentSlide < maxSlide) {
                      Button(
                        onClick = onFinish,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
                      ) { Text("Skip") }
                    }
                  }
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
                        if (tts.value?.isSpeaking == true) {
                            tts.value?.stop()
                        } else tts.value?.speak(
                            text, TextToSpeech.QUEUE_FLUSH, null, ""
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
                    if (tts.value?.isSpeaking == true || false) {
                        stopSentenceSequenced(tts.value)
                    } else {
                        val words = inputboxselecteditems_text.mapIndexed { i, w ->
                            inputboxselecteditems_pron.getOrNull(i)?.takeIf { it.isNotBlank() } ?: w
                        }
                        playSentenceSequenced(
                            tts = tts.value,
                            words = words,
                            audioPaths = inputboxselecteditems_audio.toList(),
                            pauseMs = tts_pause_duration.value
                        )
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
            model = ImageRequest.Builder(LocalContext.current)
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
            autoSize = TextAutoSize.StepBased()
        )
    }
}

@Composable
@NonSkippableComposable
fun Symbol(Name: String, image_url: String, Vertical_Stretch: Dp, tts_type: Int, x_offset: Dp = 0.dp, y_offset: Dp = 0.dp, modifier: Modifier = Modifier, menu_id: Int? = null, item_index: Int? = null, bgColor: Color) {
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
    Box()  {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image_url)
                .build(),
            "Picture of $Name",
            modifier = modifier
                .offset(x_offset, y_offset)
                .height(box_size + Vertical_Stretch + (box_padding * 3))
                .width(box_size)
                .clip(RoundedCornerShape(40.dp))
                .background(bgColor, RoundedCornerShape(40.dp))
                .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
                .padding(box_padding)
                .scale(1f)
                .clickable(onClick = {
                    if (editor_mode.value && menu_id != null && item_index != null) {
                        edit_target_menu_id.intValue = menu_id
                        edit_target_index.intValue = item_index
                        show_edit_item_dialog.value = true
                        return@clickable
                    }

                    when (tts_type) {
                        0 -> {  // add to input
                            val menu = MenuFinder(menu_id)
                            val audioPath = menu.custom_audio_paths.getOrNull(item_index ?: -1) ?: ""
                            val pron = menu.pronunciation_overrides.getOrNull(item_index ?: -1) ?: ""
                            inputboxselecteditems_text += name
                            inputboxselecteditems_has_symbol += true
                            inputboxselecteditems_audio += audioPath
                            inputboxselecteditems_pron += pron
                        }
                        1 -> {  // speak only
                            speakItem(menu_id, item_index, name)
                        }
                        2 -> {  // both
                            val menu = MenuFinder(menu_id)
                            val audioPath = menu.custom_audio_paths.getOrNull(item_index ?: -1) ?: ""
                            val pron = menu.pronunciation_overrides.getOrNull(item_index ?: -1) ?: ""
                            inputboxselecteditems_text += name
                            inputboxselecteditems_has_symbol += true
                            inputboxselecteditems_audio += audioPath
                            inputboxselecteditems_pron += pron
                            speakItem(menu_id, item_index, name)
                        }
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
fun Folder(Name: String, image_url: String, LinkedMenu: Int, Vertical_Stretch: Dp, x_offset: Dp = 0.dp, y_offset: Dp = 0.dp, modifier: Modifier = Modifier, menu_id: Int? = null, item_index: Int? = null, bgColor: Color) {
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
    Box()
    {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image_url)
                .build(),
            "Picture of $name",
            modifier = Modifier
                .offset(x_offset, y_offset)
                .height(box_size + Vertical_Stretch + (box_padding * 3))
                .width(box_size)
                .clip(RoundedCornerShape(40.dp))
                .background(bgColor, RoundedCornerShape(40.dp))
                .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
                .padding(box_padding)
                .scale(1f)
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
            .width(box_size)
            .clip(RoundedCornerShape(40.dp))
            .background(Color.White)
            .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
            .padding(box_padding)
    )
}

@Serializable
data class FitzgeraldCategory(val name: String, val colorHex: String)

val fitzgeraldKey = mutableStateListOf<FitzgeraldCategory>(
    FitzgeraldCategory("Pronoun",   Color(0xFFFFEB3B).toHexString()),  // yellow
    FitzgeraldCategory("Noun",    Color(0xFFFF9800).toHexString()),  // orange
    FitzgeraldCategory("Verb",    Color(0xFF4CAF50).toHexString()),  // green
    FitzgeraldCategory("Adjective",  Color(0xFF2196F3).toHexString()),  // blue
    FitzgeraldCategory("Social",   Color(0xFFE91E63).toHexString()),  // pink
    FitzgeraldCategory("Question",  Color(0xFF9C27B0).toHexString()),  // purple
    FitzgeraldCategory("Adverb",   Color(0xFF795548).toHexString()),  // brown
    FitzgeraldCategory("Determiner", Color(0xFFFFFFFF).toHexString()),  // white
    FitzgeraldCategory("Other",    Color(0xFFBDBDBD).toHexString())  // gray
)

fun Color.toHexString(): String {
    val r = (red * 255).toInt(); val g = (green * 255).toInt(); val b = (blue * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}

fun String.toComposeColor(): Color =
    if (isBlank()) Color.White
    else try { Color(android.graphics.Color.parseColor(this)) } catch (e: Exception) { Color.White }

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
    val custom_image_paths: List<String> = emptyList(), // custom image paths for items
    val custom_audio_paths: List<String> = emptyList(),   // recorded/imported audio file path
    val custom_audio_names: List<String> = emptyList(), // recorded/imported audio file name
    val pronunciation_overrides: List<String> = emptyList(), // phonetic respelling text
    val colors: List<String> = emptyList()  // hex like "#FFEB3B", "" = default white
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
      val item_names = remember { mutableStateListOf<String>() }
      val item_urls = remember { mutableStateListOf<String>() }
      if (item_text_padding < 5.dp) item_text_padding = 5.dp

      LaunchedEffect(menutemplate.id, menutemplate.item_list, menutemplate.image_urls,
             menutemplate.custom_image_paths, switchmenuparser.value) {
            item_names.clear()
            item_urls.clear()

            val cachedUrls = menutemplate.image_urls
            val allResolved = cachedUrls.size == menutemplate.item_list.size &&
                cachedUrls.none { it.isBlank() }

            if (allResolved) {
              item_names.addAll(menutemplate.item_list)
              menutemplate.item_list.indices.forEach { idx ->
                item_urls.add(menutemplate.displayUrl(idx))
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
                item_urls.add(if (!custom.isNullOrBlank()) custom else resolved.getOrNull(idx) ?: "")
              }
            }
          }
      current_menu_id = menutemplate.id

      key(switchmenuparser.value, menutemplate.id) {
            val dots_reserve = 22.dp
            val available_height = (menu_height - dots_reserve).coerceAtLeast(box_size + box_padding * 3)
            val item_width = box_size + (box_padding * 2)
            val item_height_natural = box_size + (box_padding * 3)

            val items_per_row = (menu_width.value / item_width.value).toInt().coerceAtLeast(1)
            val rows_per_page = (available_height.value / item_height_natural.value).toInt().coerceAtLeast(1)
            val item_height_total = available_height / rows_per_page     // divides evenly
            val vertical_stretch = item_height_total - item_height_natural

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

          LaunchedEffect(tutorial_scroll_to_index.intValue, items_per_page, page_count) {
              val idx = tutorial_scroll_to_index.intValue
              if (idx >= 0) {
                  val target_page = (idx/items_per_page).coerceIn(0, page_count-1)
                  if (pagerState.currentPage != target_page)
                  {
                      pagerState.animateScrollToPage(target_page)
                  }
              }
          }

            Column(modifier = Modifier.fillMaxSize()) {
              HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
              ) { page ->
                val startIndex = page * items_per_page
                val endIndex = minOf(startIndex + items_per_page, total_items)
                val empty_slots = items_per_page - (endIndex - startIndex)

                FlowRow(
                  modifier = Modifier.fillMaxSize(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalArrangement = Arrangement.Top
                ) {
                  for (i in startIndex until endIndex) {
                    if (i >= item_names.size || i >= item_urls.size) {
                      MenuPlaceholder(vertical_stretch); continue
                    }
                    val itemKey = "${menutemplate.id}-$i"
                    Box(modifier = Modifier.onGloballyPositioned { coords ->
                      item_positions[itemKey] = coords.positionInRoot()
                        item_sizes[itemKey] = coords.size
                    }) {
                      val itemColor = resolveItemColor(menutemplate.colors.getOrNull(i) ?: "")
                      if (menutemplate.item_type[i]) {
                        Symbol(item_names[i], item_urls[i], vertical_stretch,
                          menutemplate.tts[i]!!, menu_id = menutemplate.id,
                          item_index = i, bgColor = itemColor)
                      } else {
                        Folder(item_names[i], item_urls[i], menutemplate.pointers[i]!!,
                          vertical_stretch, menu_id = menutemplate.id,
                          item_index = i, bgColor = itemColor)
                      }
                    }
                  }
                  repeat(empty_slots) { MenuPlaceholder(vertical_stretch) }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth().height(dots_reserve),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (page_count > 1) {
                  repeat(page_count) { i ->
                    val isActive = i == pagerState.currentPage
                    Box(
                      modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isActive) 10.dp else 7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isActive) Color.Black else Color.Gray.copy(alpha = 0.4f))
                    )
                  }
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
                .drawWithContent {
                    drawContent()

                    val foldSize = size.height * 0.25f
                    val path = Path().apply {
                        moveTo(size.width - foldSize, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, foldSize)
                        close()
                    }
                    drawPath(path, Color.Black)
                }
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

fun resolveItemColor(stored: String): Color {
    if (stored.isBlank()) return Color.White
    if (stored.startsWith("#")) return stored.toComposeColor()   // custom hex
    val cat = fitzgeraldKey.find { it.name == stored }       // symbolic name
    return if (cat != null) effectiveCategoryColor(cat) else Color.White
}

fun effectiveCategoryColor(category: FitzgeraldCategory): Color {
    val override = fitzgerald_overrides[category.name]
    return if (!override.isNullOrBlank()) override.toComposeColor() else category.colorHex.toComposeColor()
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 560.dp),   // cap dialog height
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())   // scroll if needed
            ) {
                Text("Pick a color", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(40.dp)
                    .background(currentColor).border(2.dp, Color.Black))
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)       // fixed height instead of aspectRatio
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            sat = (offset.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            sat = (change.position.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        }
                    }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pure = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pure)))
                        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        val cx = sat * size.width; val cy = (1f - value) * size.height
                        drawCircle(Color.White, 10f, Offset(cx, cy), style = Stroke(2f))
                        drawCircle(Color.Black, 8f, Offset(cx, cy), style = Stroke(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Hue: ${hue.toInt()}°", fontSize = 12.sp)
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(currentColor) }) { Text("OK") }
                }
            }
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
            .clip(RoundedCornerShape(40.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(card_url).build(),
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
                show_autocomplete.value = false
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
fun SettingsScreen(contextmain: Context, onClose: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("UI", "Voice", "Backup", "Misc", "About")

    var done_clicked by remember { mutableStateOf(false) }
    var unsaved by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(done_clicked) {
        if (done_clicked) unsaved = hasStateChanged(context)
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
                    3 -> MiscSettings(contextmain)
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
        ExpandableSection("Static Words Row") { StaticSymbolRowSettings() }
        ExpandableSection("Menu Row") { MenuRowSettings() }
        ExpandableSection("Color Key") { ColorKeySettings() }
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
fun ColorKeySettings() {
    var editingCategory by remember { mutableStateOf<FitzgeraldCategory?>(null) }
    var show_add_dialog by remember { mutableStateOf(false)}

    Column {
        Text(
            "Customize the colors used for each word category. " +
                    "Changes apply to all items using that category.",
            fontSize = 13.sp, color = Color.DarkGray
        )
        Spacer(Modifier.height(8.dp))

        fitzgeraldKey.forEach { cat ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .background(effectiveCategoryColor(cat))
                        .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(12.dp))
                Text(cat.name, modifier = Modifier.weight(1f), fontSize = 16.sp)
                if (fitzgerald_overrides.containsKey(cat.name)) {
                    Button(
                        onClick = { fitzgerald_overrides.remove(cat.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                        modifier = Modifier.padding(end = 4.dp)
                    ) { Text("Reset") }
                }
                Button(onClick = { fitzgeraldKey -= cat
                    fitzgerald_overrides.remove(cat.name)
                }) { Text("Delete") }
                Spacer(Modifier.width(10.dp))
                Button(onClick = { editingCategory = cat }) { Text("Edit") }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                show_add_dialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
        ) { Text("Add item") }
        Button(
            onClick = { fitzgerald_overrides.clear()
                fitzgeraldKey.clear()
                fitzgeraldKey.addAll(listOf(
                    FitzgeraldCategory("Pronoun",   Color(0xFFFFEB3B).toHexString()),  // yellow
                    FitzgeraldCategory("Noun",    Color(0xFFFF9800).toHexString()),  // orange
                    FitzgeraldCategory("Verb",    Color(0xFF4CAF50).toHexString()),  // green
                    FitzgeraldCategory("Adjective",  Color(0xFF2196F3).toHexString()),  // blue
                    FitzgeraldCategory("Social",   Color(0xFFE91E63).toHexString()),  // pink
                    FitzgeraldCategory("Question",  Color(0xFF9C27B0).toHexString()),  // purple
                    FitzgeraldCategory("Adverb",   Color(0xFF795548).toHexString()),  // brown
                    FitzgeraldCategory("Determiner", Color(0xFFFFFFFF).toHexString()),  // white
                    FitzgeraldCategory("Other",    Color(0xFFBDBDBD).toHexString())  // gray
                ))},
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
        ) { Text("Reset all to defaults") }
        if (show_add_dialog) {
            var typedText by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            var displaycolorpicker by remember { mutableStateOf(false) }
            var tempcolor by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            fun submit() {
                fitzgeraldKey += FitzgeraldCategory(typedText, tempcolor)
                displaycolorpicker = false
                show_add_dialog = false
            }
            AlertDialog(
                onDismissRequest = {
                    show_add_dialog = false
                    displaycolorpicker = false
                },
                title = { Text("Add category", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth())
                    {
                        Row(modifier = Modifier.fillMaxWidth())
                        {
                            TextField(
                                value = typedText,
                                onValueChange = { typedText = it },
                                label = { Text("Enter category name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween)
                        {
                            Box(
                                modifier = Modifier.size(32.dp)
                                    .background(tempcolor.toComposeColor())
                                    .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                            )
                            Button(onClick = { displaycolorpicker = true }) { Text("Edit") }
                        }
                    }
                    if (displaycolorpicker) {
                        ColorPickerDialog(
                            initialColor = Color.White,
                            onDismiss = { editingCategory = null },
                            onConfirm = { picked ->
                                tempcolor = picked.toHexString()
                                displaycolorpicker = false
                            }
                        )
                    }
                },
                confirmButton = { Button(onClick = { submit() }) { Text("Add") } },
                dismissButton = { Button(onClick = { show_add_dialog = false }) { Text("Cancel") } }
            )
        }

        editingCategory?.let { cat ->
            ColorPickerDialog(
                initialColor = effectiveCategoryColor(cat),
                onDismiss = { editingCategory = null },
                onConfirm = { picked ->
                    fitzgerald_overrides[cat.name] = picked.toHexString()
                    editingCategory = null
                }
            )
        }
    }
}

@Composable
fun MiscSettings(context: Context) {
    val activity = LocalActivity.current as? ComponentActivity
    var show_error_msg = remember { mutableStateOf(false) }
    Column {
        Button(
            onClick = {
                if (isOnline(context)) {
                    show_error_msg.value = false
                    activity?.lifecycleScope?.launch { resolveAndPrecacheAll(activity) }
                }
                else
                {
                    show_error_msg.value = true
                }
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
        if (show_error_msg.value)
        {
            Text(
                "Error: Not connected to the internet!",
                fontSize = 14.sp,
                color = Color.Red
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
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) {
            statusMessage = "Export cancelled."
            isError = false
            return@rememberLauncherForActivityResult
        }
        try {
            exportToZip(context, uri)
            statusMessage = "Exported successfully."
            isError = false
        } catch (e: Exception) {
            statusMessage = "Export failed: ${e.message}"
            isError = true
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            statusMessage = "Import cancelled."
            isError = false
            return@rememberLauncherForActivityResult
        }
        try {
            importFromZip(context, uri)
            statusMessage = "Imported successfully."
            isError = false
        } catch (e: Exception) {
            statusMessage = "Import failed: ${e.message}"
            isError = true
        }
    }

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

        Button(
            onClick = {
                val timestamp = java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss", java.util.Locale.getDefault()
                ).format(java.util.Date())
                exportLauncher.launch("spegen_backup_$timestamp.spegen")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) { Text("Export to .zip file") }

        Text(
            "Exports your vocabulary, settings, and any custom images into a single file that can be transferred to another device or kept as a backup.",
            fontSize = 13.sp, color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                importLauncher.launch(
                    arrayOf(
                        "application/zip",
                        "application/octet-stream",
                        "application/json",
                        "*/*"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) { Text("Import from file") }

        Text(
            "Accepts .spegen backup files and legacy .json backups. Importing replaces all current vocabulary and settings.",
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

    val dots_reserve = 22f
    val item_total_width = preview_size + box_padding.value * 2
    val item_total_height = preview_size + box_padding.value * 3
    val items_per_row = (menu_width.value / item_total_width ).toInt().coerceAtLeast(1)
    val available_height = (menu_height.value - dots_reserve).coerceAtLeast(item_total_height)
    val rows_per_page = (available_height / item_total_height).toInt().coerceAtLeast(1)
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
    val context = LocalContext.current
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
                } + "  (${String.format("%.1f", tts_speech_rate.value)}×)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Slider(
            value = tts_speech_rate.value,
            onValueChange = {
                tts_speech_rate.value = it
                tts.value?.setSpeechRate(it)
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
                } + "  (${String.format("%.1f", tts_pitch.value)}×)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Slider(
            value = tts_pitch.value,
            onValueChange = {
                tts_pitch.value = it
                tts.value?.setPitch(it)
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
                    "${"%.1f".format(tts_pause_duration.longValue / 1000f)}s",
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
                    tts.value?.speak(words[0], TextToSpeech.QUEUE_FLUSH, null, "word_0")
                    for (i in 1 until words.size) {
                        tts.value?.playSilentUtterance(
                            tts_pause_duration.longValue, TextToSpeech.QUEUE_ADD, "pause_$i"
                        )
                        tts.value?.speak(words[i], TextToSpeech.QUEUE_ADD, null, "word_$i")
                    }
                } else {
                    tts.value?.speak(example_sentence, TextToSpeech.QUEUE_FLUSH, null, "preview")
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
            onClick = {
                try {
                    context.startActivity(
                        Intent("com.android.settings.TTS_SETTINGS").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                } catch (e: Exception) {
                    println("TTS settings not available on this device")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open device TTS settings") }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                tts_speech_rate.value = 1.0f
                tts_pitch.value = 1.0f
                tts_pause_between_words.value = false
                tts_pause_duration.longValue = 500L
                tts.value?.setSpeechRate(1.0f)
                tts.value?.setPitch(1.0f)
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
                        if (tts.value?.isSpeaking == true) {
                            tts.value?.stop()
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
                            inputboxselecteditems_has_symbol.removeAt(
                                inputboxselecteditems_has_symbol.lastIndex
                            )
                            inputboxselecteditems_audio.removeAt(inputboxselecteditems_audio.lastIndex)
                            inputboxselecteditems_pron.removeAt(inputboxselecteditems_pron.lastIndex)
                        }
                    })
            ) {
                Text(text = "Delete", color = Color.Black, modifier = Modifier
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
    Column() {
        Box(
            modifier = Modifier
                .offset(x_offset - button_boxes_width, y_offset + button_boxes_width * 4)
                .width(button_boxes_width * 2)
                .height(screenHeight-(button_boxes_width*4)-menu_static_row_height-static_row_height)
                .background(color = Color.White)
                .border(border = BorderStroke(2.dp, Color.Black))
                .clickable { show_autocomplete.value = true }
        ) {
            Text(
                text = "Autocomplete",
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center).padding(3.dp)
            )
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
fun AutocompleteMenu(modifier: Modifier) {
    val lastWord = inputboxselecteditems_text.lastOrNull() ?: ""
    val predictions = remember(lastWord, inputboxselecteditems_text.size, switchmenuparser.value) {
        ngram_model.value.predict(lastWord, limit = 24)
    }

    Column(
        modifier = modifier
            .width(menu_width)
            .height(menu_height)
            .offset(x = 0.dp, y = input_box_height)
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (lastWord.isBlank()) "Suggestions"
                else "After \"${lastWord.replaceFirstChar { it.titlecase() }}\"",
                fontSize = 16.sp, fontWeight = FontWeight.Medium
            )
            Button(onClick = { show_autocomplete.value = false }) { Text("Close") }
        }

        if (predictions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No suggestions yet.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = box_size + box_padding),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(predictions.size) { i ->
                    val rawWord = predictions[i]
                    val word = rawWord.replaceFirstChar { it.titlecase() }
                    var url by remember(rawWord) { mutableStateOf(findCachedUrl(rawWord)) }

                    LaunchedEffect(rawWord) {
                        if (url.isBlank()) {
                            url = useApiWithToken(accesstoken, rawWord)?.image_url ?: ""
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(box_size + (box_padding * 2))
                            .background(Color.White)
                            .border(4.dp, Color.Black, RoundedCornerShape(40.dp))
                            .clickable {
                                inputboxselecteditems_text += word
                                inputboxselecteditems_has_symbol += url.isNotBlank()
                                inputboxselecteditems_audio += ""
                                inputboxselecteditems_pron += ""
                            }
                    ) {
                        if (url.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(url).build(),
                                contentDescription = word,
                                modifier = Modifier.padding(box_padding).fillMaxSize()
                            )
                            Text(
                                text = word,
                                color = Color.Black,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        else
                        {
                            Text(
                                text = word,
                                color = Color.Black,
                                modifier = Modifier.align(Alignment.Center).padding(4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorToolbar() {
    val context = LocalContext.current
    val toolbarScroll = rememberScrollState()
    var exit_clicked by remember { mutableStateOf(false) }
    var unsaved by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(exit_clicked) {
        if (exit_clicked) unsaved = hasStateChanged(context)
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
            .horizontalScrollbar(toolbarScroll)
            .horizontalScroll(toolbarScroll)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("EDITOR MODE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Button(onClick = { show_add_item_dialog.value = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("+ Item") }
        Button(onClick = { show_new_menu_dialog.value = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("+ Menu") }
        Button(onClick = { show_delete_menu_dialog.value = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("- Menu") }
        Button(onClick = { show_goto_menu_dialog.value = true}, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("Go To Menu") }
        Button(
            onClick = {
                trigger_save.value = true
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)
            )) { Text("Apply Changes") }
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
            MenuList[i] = m.copy(
                item_list  = m.item_list.filterIndexed  { idx, _ -> idx !in dead },
                pointers   = m.pointers.filterIndexed   { idx, _ -> idx !in dead },
                tts        = m.tts.filterIndexed        { idx, _ -> idx !in dead },
                item_type  = m.item_type.filterIndexed  { idx, _ -> idx !in dead },
                image_urls = if (m.image_urls.size == m.item_list.size)
                    m.image_urls.filterIndexed { idx, _ -> idx !in dead } else m.image_urls,
                item_uuids = if (m.item_uuids.size == m.item_list.size)
                    m.item_uuids.filterIndexed { idx, _ -> idx !in dead } else m.item_uuids,
                custom_audio_paths = if (m.custom_audio_paths.size == m.item_list.size)
                    m.custom_audio_paths.filterIndexed { idx, _ -> idx !in dead } else m.custom_audio_paths,
                pronunciation_overrides = if (m.pronunciation_overrides.size == m.item_list.size)
                    m.pronunciation_overrides.filterIndexed { idx, _ -> idx !in dead } else m.pronunciation_overrides
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
    val context = LocalContext.current
    var name by remember { mutableStateOf(menu.item_list[idx]) }
    val originalIsSymbol = menu.item_type[idx]
    var ttsType by remember { mutableIntStateOf(menu.tts[idx] ?: 2) }
    var currentCustomPath by remember {
        mutableStateOf(menu.custom_image_paths.getOrNull(idx) ?: "")
    }
    val itemUuid by remember {
        mutableStateOf(
            menu.item_uuids.getOrNull(idx)?.takeIf { it.isNotBlank() }
                ?: java.util.UUID.randomUUID().toString()
        )
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) currentCustomPath = copyImageToPrivateStorage(context, uri, itemUuid)
    }

    var currentAudioPath by remember { mutableStateOf(menu.custom_audio_paths.getOrNull(idx) ?: "") }
    var pronunciation by remember {
        mutableStateOf(menu.pronunciation_overrides.getOrNull(idx) ?: "")
    }
    var useCustomAudio by remember { mutableStateOf(currentAudioPath.isNotBlank()) }
    var isRecording by remember { mutableStateOf(false) }
    var currentAudioName by remember { mutableStateOf(menu.custom_audio_names.getOrNull(idx) ?: "") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentColor by remember { mutableStateOf(menu.colors.getOrNull(idx) ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) currentAudioPath = copyAudioToStorage(context, uri, itemUuid)
    }
    var hasMicPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    // Commits dialog state into MenuList
    fun commitChanges() {
        val menuIndex = MenuList.indexOfFirst { it.id == menu.id }
        if (menuIndex >= 0) {
            val n = menu.item_list.size
            val customPaths = menu.custom_image_paths.toMutableList()
            while (customPaths.size < n) customPaths.add("")     // pad to full length
            customPaths[idx] = currentCustomPath
            val uuids = menu.item_uuids.toMutableList()
            while (uuids.size < n) uuids.add(java.util.UUID.randomUUID().toString())
            val custom_audio_paths = menu.custom_audio_paths.toMutableList()
            while (custom_audio_paths.size < n) custom_audio_paths.add("")
            custom_audio_paths[idx] = if (useCustomAudio) currentAudioPath else ""
            val audioNames = menu.custom_audio_names.toMutableList()
            while (audioNames.size < n) audioNames.add("")
            audioNames[idx] = if (useCustomAudio) currentAudioName.trim() else ""
            val pronunciation_overrides = menu.pronunciation_overrides.toMutableList()
            while (pronunciation_overrides.size < n) pronunciation_overrides.add("")
            val colors = menu.colors.toMutableList()
            while (colors.size < n) colors.add("")
            colors[idx] = currentColor
            pronunciation_overrides[idx] = if (!useCustomAudio) pronunciation.trim() else ""
            uuids[idx] = itemUuid

            MenuList[menuIndex] = menu.copy(
                item_list = menu.item_list.toMutableList().also { it[idx] = name.trim() },
                tts = if (originalIsSymbol)
                    menu.tts.toMutableList().also { it[idx] = ttsType } else menu.tts,
                custom_image_paths = customPaths,
                item_uuids = uuids,
                custom_audio_paths = custom_audio_paths,
                custom_audio_names = audioNames,
                pronunciation_overrides = pronunciation_overrides,
                colors = colors
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
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = {
            commitChanges()
            show_edit_item_dialog.value = false
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxHeight(0.90f).fillMaxWidth(0.95f),
        title = { Text("Edit ${if (originalIsSymbol) "symbol" else "folder"}") },
        text = {
            Row(modifier = Modifier.verticalScrollbar(scrollState).verticalScroll(scrollState)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 16.dp),
                ) {
                    Text("Item Preview", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    image_preview(name, context, previewUrl, true)
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
                                    .clip(RoundedCornerShape(20.dp))
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
                                    model = ImageRequest.Builder(context)
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
                                        .clip(RoundedCornerShape(20.dp))
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
                    Button(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                        Text("Choose custom image")
                    }
                    if (currentCustomPath.isNotBlank()) {
                        Button(
                            onClick = { currentCustomPath = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF757575))
                        ) { Text("Reset to default") }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Audio", fontSize = 14.sp, fontWeight = FontWeight.Medium)

                    Row {
                        Row(modifier = Modifier.clickable { useCustomAudio = false }.padding(8.dp)) {
                            Text(if (!useCustomAudio) "● Use item name" else "○ Use item name", fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.clickable { useCustomAudio = true }.padding(8.dp)) {
                            Text(if (useCustomAudio) "● Use custom audio" else "○ Use custom audio", fontSize = 13.sp)
                        }
                    }

                    if (useCustomAudio) {
                        if (currentAudioPath.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .border(2.dp, Color.Black, RoundedCornerShape(50))
                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    currentAudioName.ifBlank { "Audio clip" },
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { showRenameDialog = true },
                                    contentAlignment = Alignment.Center
                                ) { Text("✎", fontSize = 14.sp) }
                            }
                        }
                        if (showRenameDialog) {
                            var tempName by remember { mutableStateOf(currentAudioName) }
                            AlertDialog(
                                onDismissRequest = { showRenameDialog = false },
                                title = { Text("Rename Audio Clip") },
                                text = {
                                    TextField(
                                        value = tempName,
                                        onValueChange = { tempName = it },
                                        singleLine = true,
                                        label = { Text("Clip name") }
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        currentAudioName = tempName.trim()
                                        showRenameDialog = false
                                    }) { Text("Save") }
                                },
                                dismissButton = {
                                    Button(onClick = { showRenameDialog = false }) { Text("Cancel") }
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (!hasMicPermission) {
                                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                } else if (isRecording) {
                                    currentAudioPath = stopRecording()
                                    currentAudioName = "Recording"
                                    isRecording = false
                                } else {
                                    startRecording(context, itemUuid)
                                    isRecording = true
                                }
                            }) { Text(if (isRecording) "Stop" else "Record") }

                            Button(onClick = {
                                audioPicker.launch(arrayOf("audio/*"))
                            }) { Text("Import from device") }
                        }
                        if (currentAudioPath.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { playAudioFile(currentAudioPath) }) { Text("▶ Preview") }
                                Button(
                                    onClick = { currentAudioPath = "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
                                ) { Text("Clear audio") }
                            }
                        }
                    } else {
                        // pronunciation override box
                        Text(
                            "Optional: respell the name so it's spoken correctly (e.g. \"MIS-chiv-us\").",
                            fontSize = 12.sp, color = Color.Gray
                        )
                        TextField(
                            value = pronunciation,
                            onValueChange = { pronunciation = it },
                            singleLine = true,
                            label = { Text("Pronunciation") }
                        )
                    }

                    // Color
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Color", fontSize = 14.sp, fontWeight = FontWeight.Medium)

                    // Current selection label + preview
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp)
                            .background(resolveItemColor(currentColor))
                            .border(2.dp, Color.Black, RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                currentColor.startsWith("#") -> "Custom"
                                else -> currentColor
                            },
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tappable swatches: Default + each Fitzgerald category
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        fitzgeraldKey.forEach { cat ->
                            val isSelected = currentColor == cat.name
                            Box(modifier = Modifier.size(36.dp)
                                .background(effectiveCategoryColor(cat))
                                .border(
                                    if (isSelected) 4.dp else 2.dp,
                                    if (isSelected) Color(0xFF1976D2) else Color.Black,
                                    RoundedCornerShape(4.dp))
                                .clickable { currentColor = cat.name }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = { showColorPicker = true }) {
                        Text(if (currentColor.startsWith("#")) "Edit custom color" else "Pick custom color")
                    }

                    if (showColorPicker) {
                        ColorPickerDialog(
                            initialColor = resolveItemColor(currentColor),
                            onDismiss = { showColorPicker = false },
                            onConfirm = { picked ->
                                currentColor = picked.toHexString()
                                showColorPicker = false
                            }
                        )
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
fun image_preview(name: String, context: Context, previewUrl: String, has_text: Boolean)
{
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color.White)
            .border(4.dp, Color.Black, RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(previewUrl).build(),
            contentDescription = "Preview of $name",
            modifier = Modifier.padding(8.dp).fillMaxSize().clip(RoundedCornerShape(20.dp))
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
                        val updated = m.copy(
                            item_list = m.item_list + name.trim(),
                            pointers = m.pointers + (if (isSymbol) null else folderTarget),
                            tts = m.tts + (if (isSymbol) ttsType else null),
                            item_type = m.item_type + isSymbol,
                            item_uuids = m.item_uuids + java.util.UUID.randomUUID().toString()
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
                    AutocompleteMenu(Modifier)
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