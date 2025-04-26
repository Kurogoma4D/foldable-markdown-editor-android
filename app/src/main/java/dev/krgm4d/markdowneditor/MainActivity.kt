package dev.krgm4d.markdowneditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.krgm4d.markdowneditor.ui.theme.MarkdownEditorTheme
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaSession
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.util.concurrent.Executor

class MainViewModel : ViewModel() {
    var markdownText by mutableStateOf("# Hello Markdown")
        private set

    var isEditorFirst by mutableStateOf(true)
        private set

    val parsedHtml: String
        get() {
            val flavour = CommonMarkFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownText)
            return HtmlGenerator(markdownText, parsedTree, flavour).generateHtml()
        }

    fun onMarkdownTextChange(newText: String) {
        markdownText = newText
    }

    fun toggleColumnOrder() {
        isEditorFirst = !isEditorFirst
    }

    fun saveMarkdownToFile(uri: Uri?, contentResolver: android.content.ContentResolver) {
        uri?.let {
            try {
                contentResolver.openFileDescriptor(it, "w")?.use { parcelFileDescriptor ->
                    FileOutputStream(parcelFileDescriptor.fileDescriptor).use { fileOutputStream ->
                        fileOutputStream.write(markdownText.toByteArray())
                    }
                }
            } catch (e: Exception) {
                // Handle exceptions, e.g., show a toast message
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalWindowApi::class)
class MainActivity : ComponentActivity(), WindowAreaSessionCallback {
    private lateinit var windowAreaController: WindowAreaController
    private lateinit var displayExecutor: Executor
    private var windowAreaInfo: WindowAreaInfo? = null
    private var capabilityStatus: WindowAreaCapability.Status =
        WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED
    private val rearDisplayOperation = WindowAreaCapability.Operation.OPERATION_TRANSFER_ACTIVITY_TO_AREA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayExecutor = ContextCompat.getMainExecutor(this)
        windowAreaController = WindowAreaController.getOrCreate()

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowAreaController.windowAreaInfos
                    .map { info -> info.firstOrNull { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING } }
                    .onEach { info -> windowAreaInfo = info }
                    .map { it?.getCapability(rearDisplayOperation)?.status ?: WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED }
                    .distinctUntilChanged()
                    .collect {
                        capabilityStatus = it
                    }
            }
        }

        setContent {
            MarkdownEditorTheme {
                MarkdownEditorApp(
                    onTapSwitchDisplay = {
                        when(capabilityStatus) {
                            WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE -> {
                                windowAreaInfo?.token?.let { token ->
                                    windowAreaController.transferActivityToWindowArea(
                                        token = token,
                                        activity = this,
                                        executor = displayExecutor,
                                        windowAreaSessionCallback = this
                                    )
                                }
                            }
                            WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE -> {
                                val windowAreaSession = windowAreaInfo?.getActiveSession(rearDisplayOperation)
                                windowAreaSession?.close()
                            }
                            else -> {

                            }
                        }
                    }
                )
            }
        }
    }

    override fun onSessionEnded(t: Throwable?) {
        Log.d("MAIN", "onSessionEnded")
    }

    override fun onSessionStarted(session: WindowAreaSession) {
        Log.d("MAIN", "onSessionStarted")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditorApp(viewModel: MainViewModel = viewModel(), onTapSwitchDisplay: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val scope = rememberCoroutineScope()

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown") // Use CreateDocument
    ) { uri: Uri? ->
        scope.launch {
            viewModel.saveMarkdownToFile(uri, contentResolver)
            // Optionally show a confirmation message (e.g., Toast)
        }
    }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isOpened = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Markdown Editor") },
                actions = {
                    if (isOpened) {
                        IconButton(onClick = onTapSwitchDisplay) {
                            Icon(
                                painter = painterResource(R.drawable.rear_camera),
                                contentDescription = "Switch to rear display"
                            )
                        }
                        IconButton(onClick = { viewModel.toggleColumnOrder() }) {
                            Icon(
                                painter = painterResource(R.drawable.swap_horiz),
                                contentDescription = "Swap columns"
                            )
                        }
                    }
                    IconButton(onClick = {
                        saveFileLauncher.launch("untitled.md")
                    }) {
                        Icon(Icons.Default.Done, contentDescription = "Save Markdown")
                    }
                }
            )
        },
    ) { paddingValues ->
        if (isOpened) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (viewModel.isEditorFirst) {
                    Column(modifier = Modifier.weight(1f)) {
                        EditorScreen(viewModel.markdownText, viewModel::onMarkdownTextChange)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        PreviewScreen(viewModel.parsedHtml)
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        PreviewScreen(viewModel.parsedHtml)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        EditorScreen(viewModel.markdownText, viewModel::onMarkdownTextChange)
                    }
                }
            }
        } else {
            HorizontalPager(
                beyondViewportPageCount = 2,
                contentPadding = PaddingValues(16.dp),
                state = pagerState,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(paddingValues) // Apply padding from Scaffold
            ) { page ->
                when (page) {
                    0 -> EditorScreen(viewModel.markdownText, viewModel::onMarkdownTextChange)
                    1 -> PreviewScreen(viewModel.parsedHtml)
                }
            }
        }
    }
}

@Composable
fun EditorScreen(text: String, onTextChange: (String) -> Unit) {
    TextField(
        value = text,
        onValueChange = onTextChange,
        singleLine = false,
        modifier = Modifier.fillMaxSize(),
        keyboardOptions = KeyboardOptions.Default,
    )
}

@Composable
fun PreviewScreen(html: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.size(16.dp))
        Text(
            AnnotatedString.fromHtml(html.trimIndent()),
        )
        Spacer(Modifier.size(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MarkdownEditorTheme {
        MarkdownEditorApp(onTapSwitchDisplay = {})
    }
}