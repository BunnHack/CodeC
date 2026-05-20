package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.termux.view.TerminalView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

data class CodeFile(val name: String, val content: String, val isFolder: Boolean = false)

val INITIAL_FILES = listOf(
    CodeFile("src", "", isFolder = true),
    CodeFile("main.kt", "fun main() {\n    println(\"Hello, World!\")\n}\n\n// This is an awesome code editor\n// Built with Jetpack Compose"),
    CodeFile("App.tsx", "import React from 'react';\n\nexport default function App() {\n  return (\n    <div className=\"app\">\n      <h1>Hello from Code Editor!</h1>\n    </div>\n  );\n}\n"),
    CodeFile("styles.css", "body {\n    background-color: #1e1e1e;\n    color: #d4d4d4;\n    font-family: 'Courier New', Courier, monospace;\n}\n"),
    CodeFile("README.md", "# Code Editor\n\nA beautiful multi-pane code editor inspired by VS Code and Zed.\n")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val crashFile = java.io.File(downloadsDir, "CodeEditor_CrashLog_${System.currentTimeMillis()}.txt")
                crashFile.writeText("Error: ${throwable.message}\n\nStackTrace:\n${throwable.stackTraceToString()}")
            } catch (e: Exception) {
                e.printStackTrace() // Ignore
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ActivityBarBackground,
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    CodeEditorApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CodeEditorApp(modifier: Modifier = Modifier) {
    var selectedFile by remember { mutableStateOf(INITIAL_FILES[1]) }
    var fileContents by remember { mutableStateOf(INITIAL_FILES.associate { it.name to it.content }.toMutableMap()) }

    Row(modifier = modifier.fillMaxSize()) {
        // Activity Bar (Far Left)
        ActivityBar()

        // Explorer Sidebar
        Sidebar(
            files = INITIAL_FILES,
            selectedFile = selectedFile,
            onFileSelected = { selectedFile = it }
        )
        VerticalDivider(color = BorderColor, thickness = 1.dp)

        // Main Editor Area
        Column(modifier = Modifier
            .weight(1f)
            .background(EditorBackground)) {
            // Tabs Row
            EditorTabs(selectedFile = selectedFile)

            Column(modifier = Modifier.weight(1f)) {
                // Editor Input
                CodeEditorPane(
                    code = fileContents[selectedFile.name] ?: "",
                    onCodeChange = { newCode ->
                        val updated = fileContents.toMutableMap()
                        updated[selectedFile.name] = newCode
                        fileContents = updated
                    },
                    modifier = Modifier.weight(0.7f)
                )

                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                // Terminal Pane
                TerminalPane(modifier = Modifier.weight(0.3f))
            }

            // Status Bar
            StatusBar()
        }
    }
}

@Composable
fun ActivityBar() {
    Column(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(ActivityBarBackground)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(Icons.Filled.Description, contentDescription = "Explorer", tint = TextNormal, modifier = Modifier.size(28.dp))
        Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextLineNumber, modifier = Modifier.size(28.dp))
        Icon(Icons.Filled.Code, contentDescription = "Source Control", tint = TextLineNumber, modifier = Modifier.size(28.dp))
        Icon(Icons.Filled.PlayArrow, contentDescription = "Run", tint = TextLineNumber, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextLineNumber, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun Sidebar(files: List<CodeFile>, selectedFile: CodeFile, onFileSelected: (CodeFile) -> Unit) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(SidebarBackground)
    ) {
        Text(
            text = "EXPLORER",
            color = TextLineNumber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn {
            items(files) { file ->
                val isSelected = file == selectedFile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!file.isFolder) onFileSelected(file) }
                        .background(if (isSelected && !file.isFolder) TabActiveBackground else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (file.isFolder) Icons.Filled.KeyboardArrowDown else Icons.Filled.Description,
                        contentDescription = null,
                        tint = if (file.isFolder) TextNormal else TextKeyword,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = file.name,
                        color = if (isSelected && !file.isFolder) Color.White else TextNormal,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EditorTabs(selectedFile: CodeFile) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SidebarBackground)
        ) {
            Column(
                modifier = Modifier
                    .background(TabActiveBackground)
                    .width(IntrinsicSize.Max)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(AccentColor))
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = TextKeyword,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedFile.name,
                        color = AccentColor,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Tab",
                        tint = TextLineNumber,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}

@Composable
fun CodeEditorPane(code: String, onCodeChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
    ) {
        val scrollState = rememberScrollState()
        
        Row(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .horizontalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 16.dp)
        ) {
            // Line Numbers
            Column(
                modifier = Modifier
                    .padding(end = 16.dp, start = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                val lineCount = code.count { it == '\n' } + 1
                for (i in 1..lineCount) {
                    Text(
                        text = i.toString(),
                        color = TextLineNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        modifier = Modifier.height(20.dp)
                    )
                }
            }

            // Actual Text Field
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                textStyle = TextStyle(
                    color = TextNormal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(TextNormal),
                visualTransformation = SyntaxHighlightTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TerminalPane(modifier: Modifier = Modifier) {
    var terminalSession by remember { mutableStateOf<TerminalSession?>(null) }
    
    // Fake client to satisfy the API
    val client = remember {
        object : TerminalSessionClient {
            override fun onTextChanged(session: TerminalSession) {}
            override fun onTitleChanged(session: TerminalSession) {}
            override fun onSessionFinished(session: TerminalSession) {}
            override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}
            override fun onPasteTextFromClipboard(session: TerminalSession) {}
            override fun onBell(session: TerminalSession) {}
            override fun onColorsChanged(session: TerminalSession) {}
            override fun onTerminalCursorStateChange(state: Boolean) {}
            override fun getTerminalCursorStyle(): Int = 0
            override fun logError(tag: String?, message: String?) {}
            override fun logWarn(tag: String?, message: String?) {}
            override fun logInfo(tag: String?, message: String?) {}
            override fun logDebug(tag: String?, message: String?) {}
            override fun logVerbose(tag: String?, message: String?) {}
            override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
            override fun logStackTrace(tag: String?, e: Exception?) {}
        }
    }

    LaunchedEffect(Unit) {
        val cwd = "/"
        val shell = "/system/bin/sh"
        val env = arrayOf("TERM=xterm-256color")
        terminalSession = TerminalSession(shell, cwd, arrayOf(shell), env, 1000, client)
        // Note: initializing pseudo terminal with initializeEmulator() triggers JNI
        // terminalSession?.updateSize(80, 24)
    }

    Column(modifier = modifier.fillMaxWidth().background(EditorBackground)) {
        // Terminal Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("PROBLEMS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                Text("OUTPUT", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                Text("DEBUG CONSOLE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                Column {
                    Text("TERMINAL", color = TextNormal, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                    Box(modifier = Modifier.padding(top = 4.dp).height(2.dp).width(55.dp).background(AccentColor))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "New Terminal", tint = TextMuted, modifier = Modifier.size(16.dp))
                Icon(Icons.Filled.Delete, contentDescription = "Kill Terminal", tint = TextMuted, modifier = Modifier.size(16.dp))
                Icon(Icons.Filled.Close, contentDescription = "Close Panel", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
        
        // Terminal Content
        if (terminalSession != null) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                factory = { context ->
                    TerminalView(context, null).apply {
                        attachSession(terminalSession)
                    }
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) // Loading
        }
    }
}

@Composable
fun StatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StatusBarBackground)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Share, contentDescription = "Branch", tint = StatusBarText, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("main*", color = StatusBarText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("UTF-8", color = StatusBarText, fontSize = 11.sp, letterSpacing = 1.sp)
            Text("KOTLIN", color = StatusBarText, fontSize = 11.sp, letterSpacing = 1.sp)
            Text("LAYOUT: COMPOSE", color = StatusBarText, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

// Simple Syntax Highlighter for visual depth
class SyntaxHighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        
        // Very basic regex-based syntax highlight mapping
        val keywordPattern = "\\b(fun|val|var|class|import|return|export|default|if|else|for)\\b".toRegex()
        val funcPattern = "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()".toRegex()
        val stringPattern = "(\"[^\"]*\")|('[^']*')".toRegex()
        val commentPattern = "(//.*)".toRegex()
        
        keywordPattern.findAll(text.text).forEach { match ->
            builder.addStyle(SpanStyle(color = TextKeyword, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }
        funcPattern.findAll(text.text).forEach { match ->
            builder.addStyle(SpanStyle(color = TextFunction), match.range.first, match.range.last + 1)
        }
        stringPattern.findAll(text.text).forEach { match ->
            builder.addStyle(SpanStyle(color = TextString), match.range.first, match.range.last + 1)
        }
        commentPattern.findAll(text.text).forEach { match ->
            builder.addStyle(SpanStyle(color = TextComment), match.range.first, match.range.last + 1)
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

