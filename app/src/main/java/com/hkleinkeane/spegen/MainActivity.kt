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

package com.hkleinkeane.spegen

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import java.util.Locale
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.core.content.edit
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.hkleinkeane.spegen.menutemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.collections.List
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import kotlin.Int
import kotlin.collections.MutableList
import androidx.compose.material3.Slider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.Checkbox

val DEMO_MODE = false

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
    1, "Home", 1,
    listOf("I", "want", "more", "help", "yes", "no", "stop", "please", "People", "Actions", "Food", "Feelings"),
    listOf(null, null, null, null, null, null, null, null, 2, 3, 4, 5),
    listOf(2, 2, 2, 2, 2, 2, 2, 2, null, null, null, null),
    listOf(true, true, true, true, true, true, true, true, false, false, false, false)
)

val people = menutemplate(
    2, "People", 1,
    listOf("you", "me", "mom", "dad", "sister", "brother", "friend", "teacher"),
    listOf(null, null, null, null, null, null, null, null),
    listOf(2, 2, 2, 2, 2, 2, 2, 2),
    listOf(true, true, true, true, true, true, true, true)
)

val actions = menutemplate(
    3, "Actions", 1,
    listOf("eat", "drink", "play", "go", "sleep", "read", "watch", "listen"),
    listOf(null, null, null, null, null, null, null, null),
    listOf(2, 2, 2, 2, 2, 2, 2, 2),
    listOf(true, true, true, true, true, true, true, true)
)

val food = menutemplate(
    4, "Food", 1,
    listOf("water", "milk", "apple", "banana", "sandwich", "pizza", "cookie", "snack"),
    listOf(null, null, null, null, null, null, null, null),
    listOf(2, 2, 2, 2, 2, 2, 2, 2),
    listOf(true, true, true, true, true, true, true, true)
)

val feelings = menutemplate(
    5, "Feelings", 1,
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

var linked_menu = mutableStateOf(1)

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

var menu_terms_ids = mutableStateListOf(0, 2, 3, 4, 5)

var trigger_save = mutableStateOf(false)

var trigger_load = mutableStateOf(false)

var trigger_state_change_check = mutableStateOf(false)

var state_has_changed = mutableStateOf(false)

val show_tutorial = mutableStateOf(false)

var tts_speech_rate = mutableStateOf(1.0f)
var tts_pitch = mutableStateOf(1.0f)

var tts_pause_between_words = mutableStateOf(false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking {
            loadAllPreferences(this@MainActivity)
        }
        setContent {
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
            if (show_tutorial.value)
            {
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
            }
            if (trigger_save.value)
            {
                runBlocking {
                    saveAllPreferences(this@MainActivity)
                }
                trigger_save.value = false
            }
            if (trigger_load.value)
            {
                runBlocking {
                    loadAllPreferences(this@MainActivity)
                }
                trigger_load.value = false
            }
            if (trigger_state_change_check.value)
            {
                runBlocking {
                    state_has_changed.value = hasStateChanged(this@MainActivity)
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

    // Clear all wordfinder state
    wordfinder_display.intValue = 0
    wordfinder_display_buttonguide.intValue = 0
    wordfinder_path_ids.clear()
    wordfinder_path_names.clear()
    createclonefolder.value = false
    createclonesymbol.value = false
    wordfinder_highlight_index.intValue = -1

    // Reset menus to defaults
    MenuList.clear()
    MenuList.addAll(listOf(home, people, actions, food, feelings))

    // Reset static row
    static_terms.clear()
    static_terms.addAll(listOf("Yes", "No", "Thank you", "I need help", "Excuse me", "I use a talker to communicate"))

    // Reset menu row
    menu_terms_ids.clear()
    menu_terms_ids.addAll(listOf(0, 2, 3, 4, 5))

    // Close any open overlays
    editor_mode.value = false
    show_settings.value = false
    show_edit_item_dialog.value = false
    show_add_item_dialog.value = false
    show_new_menu_dialog.value = false

    // Back to home menu
    linked_menu.value = 1
    switchmenuparser.value++

    // Reset tutorial
    show_tutorial.value = true
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
    val tts_pause_between_words: Boolean = false
)


suspend fun saveAllPreferences(context: Context) {
    val state = PersistedState(
        box_size_dp = box_size.value,
        box_padding_dp = box_padding.value,
        input_box_height_dp = input_box_height.value,
        item_text_padding_dp = item_text_padding.value,
        has_seen_tutorial = show_tutorial.value,
        tts_data_found = tts_data_found.value,
        menu_list = MenuList,
        static_terms = static_terms,
        static_row_height = static_row_height.value,
        menu_static_row_height = menu_static_row_height.value,
        button_boxes_width = button_boxes_width.value,
        menu_row_ids = menu_terms_ids.toList(),
        tts_speech_rate = tts_speech_rate.value,
        tts_pitch = tts_pitch.value,
        tts_pause_between_words = tts_pause_between_words.value
    )
    context.spegen_datastore.edit { prefs ->
        prefs[APP_STATE_KEY] = Json.encodeToString(state)
    }
}

suspend fun loadAllPreferences(context: Context) {
    val prefs = context.spegen_datastore.data.first()
    val json = prefs[APP_STATE_KEY] ?: return
    val state = try {
        Json.decodeFromString<PersistedState>(json)
    } catch (e: Exception) {
        println("Failed to load preferences: ${e.message}")
        return
    }

    box_size = state.box_size_dp.dp
    box_padding = state.box_padding_dp.dp
    input_box_height = state.input_box_height_dp.dp
    item_text_padding = state.item_text_padding_dp.dp
    tts_data_found.value = state.tts_data_found

    static_terms.clear()
    static_terms.addAll(state.static_terms)
    MenuList.clear()
    MenuList.addAll(state.menu_list)

    static_row_height = state.static_row_height.dp
    menu_static_row_height = state.menu_static_row_height.dp
    button_boxes_width = state.button_boxes_width.dp

    menu_terms_ids.clear()
    menu_terms_ids.addAll(state.menu_row_ids)

    show_tutorial.value = state.has_seen_tutorial

    tts_speech_rate.value = state.tts_speech_rate
    tts_pitch.value = state.tts_pitch

    tts_pause_between_words.value = state.tts_pause_between_words
}

suspend fun hasStateChanged(context: Context): Boolean {
    val prefs = context.spegen_datastore.data.first()
    val savedJson = prefs[APP_STATE_KEY] ?: return true

    val savedState = try {
        Json.decodeFromString<PersistedState>(savedJson)
    } catch (e: Exception) {
        return true
    }

    val currentState = PersistedState(
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
        tts_pitch = tts_pitch.value
    )

    return currentState != savedState
}


@Composable
fun GetScreenDimensions() {
    // Function that gets the dimensions of the screen for later use in UI scaling
    var configuration = LocalConfiguration.current
    screenWidth = configuration.screenWidthDp.dp
    screenHeight = configuration.screenHeightDp.dp
    configuration = LocalConfiguration.current
    isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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
    tts = rememberTextToSpeech()
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
                        if (tts.value?.isSpeaking == true) {
                            tts.value?.stop()
                        } else tts.value?.speak(
                            text, TextToSpeech.QUEUE_FLUSH, null, ""
                        )
                    })
            ) {
                Text(text = static_terms[i], color = text_color, modifier = Modifier.align(text_alignment))
            }
        }
}

@Composable
fun InputBox(modifier: Modifier) {
    val tts = rememberTextToSpeech()

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
                    if (tts.value?.isSpeaking == true) {
                        tts.value?.stop()
                    } else if (tts_pause_between_words.value && inputboxselecteditems_text.isNotEmpty()) {
                        // First word flushes the queue, subsequent words queue up with pauses between
                        tts.value?.speak(
                            inputboxselecteditems_text[0],
                            TextToSpeech.QUEUE_FLUSH, null, "word_0"
                        )
                        for (i in 1 until inputboxselecteditems_text.size) {
                            tts.value?.playSilentUtterance(500L, TextToSpeech.QUEUE_ADD, "pause_$i")
                            tts.value?.speak(
                                inputboxselecteditems_text[i],
                                TextToSpeech.QUEUE_ADD, null, "word_$i"
                            )
                        }
                    } else {
                        val speech = inputboxselecteditems_text.joinToString(" ")
                        tts.value?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "")
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
    var url by remember {mutableStateOf("")}
    LaunchedEffect(inputboxselecteditems_text) {
        val res = useApiWithToken(accesstoken, name)
        url = res?.image_url ?: ""
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
                .height(height_dp.dp)
                .width(width_dp.dp)
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
    Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f), contentAlignment = Alignment.CenterStart) {
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
    tts = rememberTextToSpeech()
    Box(modifier = modifier)  {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
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
                    if (tts_type == 0) {
                        if (tts.value?.isSpeaking == true) {
                            tts.value?.stop()
                        }
                        if (tts_pause_between_words.value) {
                            val splitwords = name.split(" ")
                            tts.value?.speak(
                                splitwords[0],
                                TextToSpeech.QUEUE_FLUSH, null, "word_0"
                            )
                            for (i in 1 until splitwords.size) {
                                tts.value?.playSilentUtterance(
                                    500L,
                                    TextToSpeech.QUEUE_ADD,
                                    "pause_$i"
                                )
                                tts.value?.speak(
                                    splitwords[i],
                                    TextToSpeech.QUEUE_ADD, null, "word_$i"
                                )
                            }
                        }
                        else tts.value?.speak(
                            (name), TextToSpeech.QUEUE_FLUSH, null, ""
                        )
                    }
                    if (tts_type == 1) {
                        inputboxselecteditems_text += name
                        inputboxselecteditems_has_symbol += true
                    }
                    if (tts_type == 2) {
                        if (tts.value?.isSpeaking == true) {
                            tts.value?.stop()
                        }
                        if (tts_pause_between_words.value) {
                            val splitwords = name.split(" ")
                            tts.value?.speak(
                                splitwords[0],
                                TextToSpeech.QUEUE_FLUSH, null, "word_0"
                            )
                            for (i in 1 until splitwords.size) {
                                tts.value?.playSilentUtterance(
                                    500L,
                                    TextToSpeech.QUEUE_ADD,
                                    "pause_$i"
                                )
                                tts.value?.speak(
                                    splitwords[i],
                                    TextToSpeech.QUEUE_ADD, null, "word_$i"
                                )
                            }
                        }
                        else tts.value?.speak(
                            (name), TextToSpeech.QUEUE_FLUSH, null, ""
                        )
                        inputboxselecteditems_text += name
                        inputboxselecteditems_has_symbol += true
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
            model = ImageRequest.Builder(LocalContext.current)
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
                                linked_menu.value = LinkedMenu
                                switchmenuparser.value += 1
                                wordfinder_manager()
                            }
                        }
                    } else {
                        linked_menu.value = LinkedMenu
                        switchmenuparser.value += 1
                    }
                    if (wordfinder_path_ids.size >= 1 && !wordfinder_target_is_symbol) {
                        wordfinder_highlight_index.intValue = -1
                        wordfinder_path_ids.removeAt(0)
                        wordfinder_path_ids.clear()
                        wordfinder_path_names.clear()
                        wordfinder_display_buttonguide.intValue = 0
                        createclonefolder.value = false
                        createclonesymbol.value = false
                        linked_menu.value = LinkedMenu
                        switchmenuparser.value += 1
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
    }
}

@Serializable
data class menutemplate(
    val id: Int, // ID of the current menu
    val title: String, // Title of the current menu
    val parentId: Int?, // ID of the parent menu
    val item_list: List<String>, // List of the names of all items, both folders and symbols
    val pointers: List<Int?>, // Pointers to be used in MenuFinder to find the corresponding menu for a folder to link to. Null if item is a symbol since it has no pointer.
    val tts: List<Int?>, // 0 is for appending to the input box without instantly playing, 1 is for instantly playing in tts engine without appending to input box, 2 is for both appending to text box and playing in tts engine instantly. If a value is null item is a folder that doesn't have tts.
    val item_type: List<Boolean>, // False is for folder, true is for symbol
)

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
    LaunchedEffect(Unit) {
        getAccessToken()
    }
    LaunchedEffect(menutemplate) {
        getAccessToken()

        item_names.clear()
        item_urls.clear()

        menutemplate.item_list.forEach { query ->
            val res = useApiWithToken(accesstoken, query)
            item_names.add(query)
            item_urls.add(res?.image_url ?: "")
        }
    }
    current_menu_id = menutemplate.id
    if (switchmenuparser.value > 0) {
        key(switchmenuparser.value) {
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
                if (idx >= 0)
                {
                    val target_page = (idx / items_per_page).coerceIn(0, page_count - 1)
                    if (pagerState.currentPage != target_page)
                    {
                        pagerState.animateScrollToPage(target_page)
                    }
                }
            }

            HorizontalPager(state = pagerState, modifier = modifier.fillMaxWidth().fillMaxHeight()) { page ->
                val startIndex = page * items_per_page
                val endIndex = minOf(startIndex + items_per_page, total_items)
                val empty_slots = items_per_page - (endIndex - startIndex)

                FlowRow(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in startIndex until endIndex) {
                        if (i >= item_names.size || i >= item_urls.size) {
                            Box(
                                modifier = Modifier
                                    .height(box_size + vertical_stretch + (box_padding * 3))
                                    .background(Color.White)
                                    .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
                                    .padding(box_padding)
                                    .scale(1f)
                                    .width(box_size)
                            )
                            continue
                        }
                        val itemKey = "${menutemplate.id}-$i"
                        Box(modifier = Modifier.onGloballyPositioned { coords ->
                            item_positions[itemKey] = coords.positionInRoot()
                        }) {
                            if (menutemplate.item_type[i]) {
                                Symbol(item_names[i], item_urls[i], vertical_stretch, menutemplate.tts[i]!!, menu_id = menutemplate.id, item_index = i)
                            } else {
                                Folder(item_names[i], item_urls[i], menutemplate.pointers[i]!!, vertical_stretch, menu_id = menutemplate.id, item_index = i)
                            }
                        }
                    }
                    for (i in 0 until empty_slots) {
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
                }
            }
        }
    }
    else {
        key(menukeylist[MenuList.indexOf(menutemplate)]) {
            val item_width = box_size + (box_padding * 2)
            val item_height_total = box_size + vertical_stretch + (box_padding * 3)
            val items_per_row = (menu_width.value / item_width.value).toInt().coerceAtLeast(1)
            val rows_per_page = (menu_height.value / item_height_total.value).toInt().coerceAtLeast(1)
            val items_per_page = (items_per_row * rows_per_page).coerceAtLeast(1)
            val total_items = menutemplate.item_list.size
            val page_count = ((total_items + items_per_page - 1) / items_per_page).coerceAtLeast(1)

            val pagerState = rememberPagerState(pageCount = { page_count })
            HorizontalPager(state = pagerState, modifier = modifier.fillMaxWidth().fillMaxHeight()) { page ->
                val startIndex = page * items_per_page
                val endIndex = minOf(startIndex + items_per_page, total_items)
                val empty_slots = items_per_page - (endIndex - startIndex)

                FlowRow(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                for (i in startIndex until endIndex) {
                    if (i >= item_names.size || i >= item_urls.size) {
                        Box(
                            modifier = Modifier
                                .height(box_size + vertical_stretch + (box_padding * 3))
                                .background(Color.White)
                                .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
                                .padding(box_padding)
                                .scale(1f)
                                .width(box_size)
                        )
                        continue
                    }
                    val itemKey = "${menutemplate.id}-$i"
                    Box(modifier = Modifier.onGloballyPositioned { coords ->
                        item_positions[itemKey] = coords.positionInRoot()
                    }) {
                        if (menutemplate.item_type[i]) {
                            Symbol(item_names[i], item_urls[i], vertical_stretch, menutemplate.tts[i]!!, menu_id = menutemplate.id, item_index = i)
                        } else {
                            Folder(item_names[i], item_urls[i], menutemplate.pointers[i]!!, vertical_stretch, menu_id = menutemplate.id, item_index = i)
                        }
                    }
                }
                    for (i in 0 until empty_slots) {
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
                    linked_menu.value = menu_terms_ids[i]
                    switchmenuparser.value += 1
                })
        ) {
            Text(
                text = MenuFinder(menu_terms_ids[i]).title,
                color = text_color,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}


// Could make an image override function that lets the user use their own images in place of the default ones
fun ImageOverride() {
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
                    onClick = { wordfinder_display.intValue = 0 }
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
                                if (MenuList[i].item_list[a].lowercase().replace(" ", "") == searchQuery.lowercase().replace(" ", "")) {
                                    WordFinder_Card(searchQuery, i, MenuList[i].item_type[a], a, flowrow_height_space, box_width)
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
        if (current.id in visited) break  // stops infinite loop
        visited.add(current.id)
        pathParts.add(0, current.title)
        wordfinder_path_names.add(0, current.title)
        val parentId = current.parentId
        current = if (parentId != null) MenuList.find { it.id == parentId } else null
    }

    return pathParts.joinToString(" > ")
}
fun setWordfinderPath(menuIndex: Int) {
    val pathParts = mutableListOf<String>()
    val visited = mutableSetOf<Int>()
    var current: menutemplate? = MenuList[menuIndex]

    wordfinder_path_ids.clear()

    while (current != null) {
        if (current.id in visited) break  // stops infinite loop
        visited.add(current.id)
        wordfinder_path_ids.add(0, current.id)
        val parentId = current.parentId
        current = if (parentId != null) MenuList.find { it.id == parentId } else null
    }
}

@Composable
fun WordFinder_Card(Name: String, MenuList_element: Int, is_symbol: Boolean, item_position: Int, total_avaliable_height: Dp, total_avaiable_width: Dp) {
    var min_height = 20.dp
    var cards_per_row = 4
    var card_height = 0.dp
    var card_name by remember { mutableStateOf("") }
    var card_url by remember { mutableStateOf("") }
    var box_size = (total_avaliable_height/cards_per_row)
    var box_padding = 20.dp
    val item_path = getMenuPath(MenuList_element)
    if ((total_avaliable_height.value/cards_per_row).dp > min_height) {
        card_height = (total_avaliable_height.value/cards_per_row).dp
    }
    else {
        card_height = min_height
    }
    LaunchedEffect(Unit) {
        val res = useApiWithToken(accesstoken, MenuList[MenuList_element].item_list[item_position])
        card_name = res?.name ?: MenuList[MenuList_element].item_list[item_position]
        card_url = res?.image_url ?: ""
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 4.dp, color = Color.Black, shape = RoundedCornerShape(40.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(box_padding),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(card_url)
                    .build(),
                contentDescription = "Picture of $card_name",
                modifier = Modifier
                    .size(box_size)
                    .scale(1f)
            )
            val capitalized_name = Name.replaceFirstChar { char ->
                char.titlecase()
            }
            Column()
            {
                Text(
                    text = card_name,
                    fontSize = (box_size.value / 3).sp
                )
                Text(
                    text = item_path,
                    fontSize = (box_size.value / 6).sp,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            var showButtonGuide by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    showButtonGuide = true
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(text = "Find", textAlign = TextAlign.Center)
            }
            if (showButtonGuide) {
                wordfinder_target_is_symbol = is_symbol
                setWordfinderPath(MenuList_element)
                val targetName = MenuList[MenuList_element].item_list[item_position]
                if (wordfinder_path_names.lastOrNull() != targetName) {
                    wordfinder_path_names.add(targetName)
                }
                linked_menu.value = wordfinder_path_ids.firstOrNull() ?: 1
                wordfinder_display_buttonguide.intValue += 1
                wordfinder_display.intValue = 0
                switchmenuparser.value++
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
    val tabs = listOf("UI", "Voice", "Backup", "About")

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
            Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
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
                    3 -> AboutContent()
                }
            }

            // DONE BUTTON
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onClose) { Text("Done") }
            }
        }
    }
}

@Composable
fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
fun BackupSettingsContent() {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) {
            statusMessage = "Export cancelled."
            return@rememberLauncherForActivityResult
        }
        try {
            val state = PersistedState(
                box_size_dp = box_size.value,
                box_padding_dp = box_padding.value,
                input_box_height_dp = input_box_height.value,
                item_text_padding_dp = item_text_padding.value,
                has_seen_tutorial = true,
                tts_data_found = tts_data_found.value,
                menu_list = MenuList.toList(),
                static_terms = static_terms.toList(),
                static_row_height = static_row_height.value,
                menu_static_row_height = menu_static_row_height.value,
                button_boxes_width = button_boxes_width.value,
                menu_row_ids = menu_terms_ids.toList()
            )
            val json = Json.encodeToString(state)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray())
            }
            statusMessage = "Exported successfully."
        } catch (e: Exception) {
            statusMessage = "Export failed: ${e.message}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            statusMessage = "Import cancelled."
            return@rememberLauncherForActivityResult
        }
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: throw Exception("Could not read file.")
            val state = Json.decodeFromString<PersistedState>(json)

            box_size = state.box_size_dp.dp
            box_padding = state.box_padding_dp.dp
            input_box_height = state.input_box_height_dp.dp
            item_text_padding = state.item_text_padding_dp.dp
            tts_data_found.value = state.tts_data_found
            static_terms.clear()
            static_terms.addAll(state.static_terms)
            MenuList.clear()
            MenuList.addAll(state.menu_list)
            static_row_height = state.static_row_height.dp
            menu_static_row_height = state.menu_static_row_height.dp
            button_boxes_width = state.button_boxes_width.dp

            statusMessage = "Imported successfully."
        } catch (e: Exception) {
            statusMessage = "Import failed: ${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Save your SpeGen data to a file, or restore from a previous backup.",
            fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss",
                    java.util.Locale.getDefault()).format(java.util.Date())
                exportLauncher.launch("spegen_backup_$timestamp.json")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) { Text("Export to .json file") }
        Text("Backup/Restore for this option works with the following applications: ",
            fontSize = 14.sp, color = Color.DarkGray)
        Text("SpeGen")
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) { Text("Import from file") }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = statusMessage, fontSize = 14.sp,
                color = if (statusMessage.contains("fail", ignoreCase = true)
                    || statusMessage.contains("cancel", ignoreCase = true))
                    Color.Red else Color(0xFF2E7D32))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Note: Importing replaces all current data. Export first if you want to keep a copy.",
            fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun StaticSymbolRowSettings() {
    Column {
        Text("Words always visible at the bottom of the screen.", fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        static_terms.forEachIndexed { index, term ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = term, modifier = Modifier.weight(1f), fontSize = 16.sp)
                Button(
                    onClick = { static_terms.removeAt(index) },
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Remove") }
            }
        }
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
    }
}

@Composable
fun MenuRowSettings() {
    Column {
        Text("Choose which menus appear in the bottom menu row.", fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        MenuList.forEach { menu ->
            if (menu.id != 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = menu.title, modifier = Modifier.weight(1f), fontSize = 16.sp)
                    Button(onClick = {
                        if (menu.id in menu_terms_ids) {
                            menu_terms_ids.remove(menu.id)
                        }
                        else {
                            menu_terms_ids += menu.id
                        }
                    }) { Text("Toggle visibility") }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (menu.id in menu_terms_ids) {
                        Text(text = "(visible)", color = Color.Gray, fontSize = 14.sp)
                    }
                    else {
                        Text(text = "(not visible)", color = Color.Gray, fontSize = 14.sp)
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
            onValueChange = { preview_size = it },
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

        if (preview_size != box_size.value) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Current: ${box_size.value.toInt()}dp  →  Preview: ${preview_size.toInt()}dp",
                fontSize = 12.sp,
                color = Color(0xFFFF8F00)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    box_size = preview_size.dp
                    switchmenuparser.value++
                          },
                modifier = Modifier.weight(1f),
                enabled = preview_size != box_size.value
            ) { Text("Apply") }

            Button(
                onClick = { preview_size = 100f },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
            ) { Text("Reset") }
        }
    }
}

@Composable
fun VoiceSettingsContent() {
    val context = LocalContext.current
    var rate_preview  by remember { mutableFloatStateOf(tts_speech_rate.value) }
    var pitch_preview by remember { mutableFloatStateOf(tts_pitch.value) }
    val example_sentence = "The quick brown fox jumps over the lazy dog."

    fun applyToEngine() {
        tts.value?.setSpeechRate(rate_preview)
        tts.value?.setPitch(pitch_preview)
        tts_speech_rate.value = rate_preview
        tts_pitch.value = pitch_preview
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // Speech Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Speech rate", fontSize = 16.sp)
            Text(
                when {
                    rate_preview < 0.6f -> "Very slow"
                    rate_preview < 0.9f -> "Slow"
                    rate_preview < 1.1f -> "Normal"
                    rate_preview < 1.5f -> "Fast"
                    else -> "Very fast"
                } + "  (${String.format("%.1f", rate_preview)}×)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Slider(
            value = rate_preview,
            onValueChange = { rate_preview = it },
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
                    pitch_preview < 0.7f -> "Low"
                    pitch_preview < 0.9f -> "Slightly low"
                    pitch_preview < 1.1f -> "Normal"
                    pitch_preview < 1.4f -> "Slightly high"
                    else -> "High"
                } + "  (${String.format("%.1f", pitch_preview)}×)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Slider(
            value = pitch_preview,
            onValueChange = { pitch_preview = it },
            valueRange = 0.5f..2.0f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        // Apply & Test
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { applyToEngine() },
                modifier = Modifier.weight(1f),
                enabled = rate_preview != tts_speech_rate.value
                        || pitch_preview != tts_pitch.value
            ) { Text("Apply") }

            Button(
                onClick = {
                    applyToEngine()
                    if (tts_pause_between_words.value)
                    {
                        val splitsentence = example_sentence.split(" ")
                        tts.value?.speak(
                            splitsentence[0],
                            TextToSpeech.QUEUE_FLUSH, null, "word_0"
                        )
                        for (i in 1 until splitsentence.size) {
                            tts.value?.playSilentUtterance(500L, TextToSpeech.QUEUE_ADD, "pause_$i")
                            tts.value?.speak(
                                splitsentence[i],
                                TextToSpeech.QUEUE_ADD, null, "word_$i"
                            )
                        }
                    }
                    else
                    {
                        tts.value?.speak(
                            example_sentence,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "preview"
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) { Text("▶ Test voice") }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System TTS settings
        Text(
            "Voice engine & language",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Install voice packs, switch engines (if available), or change language in your device's TTS settings.",
            fontSize = 13.sp,
            color = Color.Gray
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

        // --- Reset ---
        Button(
            onClick = {
                rate_preview  = 1.0f
                pitch_preview = 1.0f
                applyToEngine()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
        ) { Text("Reset to defaults") }
    }
}

@Composable
fun AboutContent() {
    Column {
        Text("SpeGen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("An open-source AAC app developed by Harper Klein Keane. SpeGen's website can be found at the following URL: https://hkleinkeane.github.io/spegen/.", fontSize = 14.sp)
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
                Text(text = "Settings", color = Color.Black, modifier = Modifier.align(Alignment.Center).padding(3.dp))
            }
        }
        //BOTTOM RIGHT
        Column() {
            Box(
                modifier = Modifier
                    .offset(x_offset, y_offset + button_boxes_width*2)
                    .size(button_boxes_width)
                    .background(color = Color.White)
                    .border(border = BorderStroke(2.dp, Color.Black))
                    .clickable(onClick = {
                        if (tts.value?.isSpeaking == true) {
                            tts.value?.stop()
                        }
                    })
            ) {
                Text(text = "Stop", color = Color.Black, modifier = Modifier.align(Alignment.Center).padding(3.dp))
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
                Text(text = "Keyboard", color = Color.Black, modifier = Modifier.align(Alignment.Center).padding(3.dp))
            }}
        //MIDDLE LEFT
        Column() {
            Box(
                modifier = Modifier
                    .offset(x_offset - button_boxes_width, y_offset + button_boxes_width*2)
                    .size(button_boxes_width)
                    .background(color = Color.White)
                    .border(border = BorderStroke(2.dp, Color.Black))
                    .clickable(onClick = {
                        inputboxselecteditems_text.clear()
                        inputboxselecteditems_has_symbol.clear()
                    })
            ) {
                Text(text = "Clear", color = Color.Black, modifier = Modifier.align(Alignment.Center).padding(3.dp))
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
                        }
                    })
            ) {
                Text(text = "Delete", color = Color.Black, modifier = Modifier.align(Alignment.Center).padding(3.dp))
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
            Text(text = "Search", color = Color.Black, modifier = Modifier.align(Alignment.Center).padding(3.dp))
        }
    }
    Column() {
        Box(
            modifier = Modifier
                .offset(x_offset - button_boxes_width, y_offset + button_boxes_width*3)
                .width(button_boxes_width*2)
                .height(button_boxes_width)
                .background(color = Color.White)
                .border(border = BorderStroke(2.dp, Color.Black))
                .clickable(onClick = {
                    if (MenuFinder(current_menu_id).parentId != MenuFinder(current_menu_id).id) {
                        linked_menu.value = MenuFinder(current_menu_id).parentId!!
                        switchmenuparser.value += 1
                    }
                })
        ) {
            Text(text = "Back", color = Color.Black, modifier = Modifier.align(Alignment.Center).padding(3.dp))
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
    var exit_button_clicked by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFFFFE082))
            .border(2.dp, Color(0xFFFF8F00))
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
        Button(
            onClick = {
                    runBlocking {
                        trigger_save.value = true
                    }},
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
        ) { Text("Apply Changes") }
        Spacer(modifier = Modifier.width(20.dp))
        Button(
            onClick = {
                exit_button_clicked = true
                },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
        ) {
            Text("Exit")
        }
    }
    if (exit_button_clicked)
    {
        trigger_state_change_check.value = true
        if (state_has_changed.value) {
            AlertDialog(
                onDismissRequest = {
                    editor_mode.value = false
                    trigger_load.value = true
                    exit_button_clicked = false
                    trigger_state_change_check.value = false
                    state_has_changed.value = false
                },
                title = { Text("Unsaved Changes") },
                text = {
                    Column {
                        Text(
                            "You have unsaved changes, do you want to save them?",
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        trigger_state_change_check.value = false
                        trigger_save.value = true
                        exit_button_clicked = false
                        state_has_changed.value = false
                        editor_mode.value = false
                        trigger_load.value = true
                    }) { Text("Save Changes") }
                },
                dismissButton = {
                    Button(onClick = {
                        trigger_state_change_check.value = false
                        exit_button_clicked = false
                        state_has_changed.value = false
                        editor_mode.value = false
                        trigger_load.value = true
                    }) { Text("Don't Save") }
                }
            )
        }
        else
        {
            trigger_load.value = true
            editor_mode.value = false
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

    var name by remember { mutableStateOf(menu.item_list[idx]) }
    val originalIsSymbol = menu.item_type[idx]
    var ttsType by remember { mutableIntStateOf(menu.tts[idx] ?: 2) }

    AlertDialog(
        onDismissRequest = { show_edit_item_dialog.value = false },
        title = { Text("Edit ${if (originalIsSymbol) "symbol" else "folder"}") },
        text = {
            Column {
                Text("Name", fontSize = 14.sp)
                TextField(value = name, onValueChange = { name = it }, singleLine = true)
                if (originalIsSymbol) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("TTS behavior", fontSize = 14.sp)
                    Row {
                        listOf("Type only" to 0, "Speak only" to 1, "Both" to 2).forEach { (label, value) ->
                            Row(
                                modifier = Modifier.clickable { ttsType = value }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (ttsType == value) "● $label" else "○ $label", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val menuIndex = MenuList.indexOfFirst { it.id == menu.id }
                if (menuIndex >= 0) {
                    val updated = menu.copy(
                        item_list = menu.item_list.toMutableList().also { it[idx] = name.trim() },
                        tts = if (originalIsSymbol)
                            menu.tts.toMutableList().also { it[idx] = ttsType }
                        else menu.tts
                    )
                    MenuList[menuIndex] = updated
                }
                show_edit_item_dialog.value = false
            }) { Text("Save") }
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
                Button(onClick = { show_edit_item_dialog.value = false }) { Text("Cancel") }
            }
        }
    )
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
                    Row(modifier = Modifier.clickable { isSymbol = true }.padding(8.dp)) {
                        Text(if (isSymbol) "● Symbol" else "○ Symbol")
                    }
                    Row(modifier = Modifier.clickable { isSymbol = false }.padding(8.dp)) {
                        Text(if (!isSymbol) "● Folder" else "○ Folder")
                    }
                }
                if (!isSymbol) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Folder target menu", fontSize = 14.sp)
                    Column(modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
                        MenuList.forEach { m ->
                            Row(modifier = Modifier.clickable { folderTarget = m.id }.padding(4.dp)) {
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
                            item_type = m.item_type + isSymbol
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
                        parentId = 1,
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
            .offset(0.dp, 0.dp)
            .width(screenWidth)
            .height(input_box_height)
            .background(dim)
            .zIndex(700f)
            .clickable( onClick = {} )
    )
    // Covers MenuRow and Static_Row_Needs
    Box(
        modifier = Modifier
            .offset(0.dp, input_box_height+menu_height)
            .width(screenWidth)
            .height(menu_static_row_height+static_row_height)
            .background(dim)
            .zIndex(700f)
            .clickable( onClick = {} )
    )
    // Covers buttonboxes
    Box(
        modifier = Modifier
            .offset(screenWidth-(button_boxes_width*2), input_box_height)
            .width(button_boxes_width*2)
            .height(menu_height)
            .background(dim)
            .zIndex(700f)
            .clickable( onClick = {} )
    )
    Box(
        modifier = Modifier
            .offset(0.dp, input_box_height)
            .width(screenWidth-(button_boxes_width*2))
            .height(menu_height)
            .border(3.dp, Color(0xFFFF8F00))
            .zIndex(750f)
    )
}

@Composable
fun Screen() {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        tts = rememberTextToSpeech()
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
            Menu(Modifier)
        }
    }
}