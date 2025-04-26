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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

class MainViewModel : ViewModel() {
    var markdownText by mutableStateOf("# Hello Markdown")
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarkdownEditorTheme {
                MarkdownEditorApp()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditorApp(viewModel: MainViewModel = viewModel()) {
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
                        IconButton(onClick = {
                            // Launch the file saver intent
                            saveFileLauncher.launch("untitled.md") // Suggest a default filename
                        }) {
                            Icon(Icons.Filled.Done, contentDescription = "Save Markdown")
                        }
                }
            )
        },
    ) { paddingValues ->
        if (isOpened) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply padding from Scaffold
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EditorScreen(viewModel.markdownText, viewModel::onMarkdownTextChange)
                }
                Column(modifier = Modifier.weight(1f)) {
                    PreviewScreen(viewModel.parsedHtml)
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
        MarkdownEditorApp()
    }
}