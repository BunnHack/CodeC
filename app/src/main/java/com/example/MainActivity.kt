package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalContext
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.db.AppDatabase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.home.HomeScreen
import com.example.ui.theme.*
import com.termux.view.TerminalView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

enum class PreviewViewportMode {
    DESKTOP,
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE
}

@Composable
fun WebPreviewDialog(fileContents: Map<String, String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val htmlFileName = remember(fileContents) {
        fileContents.keys.find { it.endsWith(".html", ignoreCase = true) }
    }
    val isWebProject = htmlFileName != null
    var viewportMode by remember { mutableStateOf(PreviewViewportMode.DESKTOP) }
    var menuExpanded by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val combinedHtml = remember(fileContents, refreshTrigger, htmlFileName) {
        if (htmlFileName == null) "" else {
            val htmlContent = fileContents[htmlFileName] ?: ""
            val cssName = fileContents.keys.find { it.endsWith(".css", ignoreCase = true) } ?: "styles.css"
            val jsName = fileContents.keys.find { it.endsWith(".js", ignoreCase = true) } ?: "app.js"
            val cssContent = fileContents[cssName] ?: ""
            val jsContent = fileContents[jsName] ?: ""
            
            htmlContent
                .replace("<link rel=\"stylesheet\" href=\"$cssName\">", "<style>\n$cssContent\n</style>")
                .replace("<link rel=\"stylesheet\" href=\"styles.css\">", "<style>\n$cssContent\n</style>")
                .replace("<script src=\"$jsName\"></script>", "<script>\n$jsContent\n</script>")
                .replace("<script src=\"app.js\"></script>", "<script>\n$jsContent\n</script>")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = EditorBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // High-fidelity toolbar at the top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SidebarBackground)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Preview",
                            tint = TextNormal
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = if (isWebProject) "Web Preview" else "Preview Unavailable",
                            fontWeight = FontWeight.Bold,
                            color = TextNormal,
                            fontSize = 15.sp
                        )
                        val subtitle = when (viewportMode) {
                            PreviewViewportMode.DESKTOP -> "Desktop Mode • Fullscreen"
                            PreviewViewportMode.MOBILE_PORTRAIT -> "Mobile View • 360 x 740"
                            PreviewViewportMode.MOBILE_LANDSCAPE -> "Mobile Landscape • 740 x 360"
                        }
                        Text(
                            text = subtitle,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Live preview badge
                    if (isWebProject) {
                        Surface(
                            color = Color(0xFF00E676).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E676)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00E676), RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE",
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Refresh Button
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Preview",
                            tint = TextNormal
                        )
                    }

                    // Three Dots Menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = TextNormal
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(SidebarBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Desktop View (Fullscreen)", color = TextNormal) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Computer,
                                        contentDescription = null,
                                        tint = if (viewportMode == PreviewViewportMode.DESKTOP) AccentColor else TextMuted
                                    )
                                },
                                trailingIcon = {
                                    if (viewportMode == PreviewViewportMode.DESKTOP) {
                                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = AccentColor)
                                    }
                                },
                                onClick = {
                                    viewportMode = PreviewViewportMode.DESKTOP
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mobile Portrait (Phone)", color = TextNormal) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (viewportMode == PreviewViewportMode.MOBILE_PORTRAIT) AccentColor else TextMuted
                                    )
                                },
                                trailingIcon = {
                                    if (viewportMode == PreviewViewportMode.MOBILE_PORTRAIT) {
                                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = AccentColor)
                                    }
                                },
                                onClick = {
                                    viewportMode = PreviewViewportMode.MOBILE_PORTRAIT
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mobile Landscape (Tablet)", color = TextNormal) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Tablet,
                                        contentDescription = null,
                                        tint = if (viewportMode == PreviewViewportMode.MOBILE_LANDSCAPE) AccentColor else TextMuted
                                    )
                                },
                                trailingIcon = {
                                    if (viewportMode == PreviewViewportMode.MOBILE_LANDSCAPE) {
                                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = AccentColor)
                                    }
                                },
                                onClick = {
                                    viewportMode = PreviewViewportMode.MOBILE_LANDSCAPE
                                    menuExpanded = false
                                }
                            )
                            HorizontalDivider(color = BorderColor)
                            DropdownMenuItem(
                                text = { Text("Reload Page", color = TextNormal) },
                                leadingIcon = { Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = TextMuted) },
                                onClick = {
                                    refreshTrigger++
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open in Browser", color = TextNormal) },
                                leadingIcon = { Icon(imageVector = Icons.Filled.OpenInNew, contentDescription = null, tint = TextMuted) },
                                onClick = {
                                    menuExpanded = false
                                    try {
                                        val builder = android.os.StrictMode.VmPolicy.Builder()
                                        android.os.StrictMode.setVmPolicy(builder.build())
                                        
                                        val file = java.io.File(context.externalCacheDir ?: context.cacheDir, "web_preview.html")
                                        file.writeText(combinedHtml)
                                        
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(android.net.Uri.fromFile(file), "text/html")
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Could not open in browser: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }
                }
                
                HorizontalDivider(color = BorderColor)
                
                if (isWebProject) {
                    var webViewRef by remember { mutableStateOf<WebView?>(null) }

                    // We need a styled wrapper box centered on screen if we are in mobile mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF0F1012)), // dark outer presentation area
                        contentAlignment = Alignment.Center
                    ) {
                        val webViewModifier = when (viewportMode) {
                            PreviewViewportMode.DESKTOP -> Modifier.fillMaxSize()
                            PreviewViewportMode.MOBILE_PORTRAIT -> Modifier
                                .width(360.dp)
                                .fillMaxHeight()
                                .padding(vertical = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(4.dp, BorderColor, RoundedCornerShape(24.dp))
                            PreviewViewportMode.MOBILE_LANDSCAPE -> Modifier
                                .fillMaxWidth(0.9f)
                                .aspectRatio(16f / 9.5f)
                                .clip(RoundedCornerShape(24.dp))
                                .border(4.dp, BorderColor, RoundedCornerShape(24.dp))
                        }

                        AndroidView(
                            modifier = webViewModifier,
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webChromeClient = WebChromeClient()
                                    webViewClient = WebViewClient()
                                    webViewRef = this
                                    loadDataWithBaseURL(null, combinedHtml, "text/html", "UTF-8", null)
                                    tag = combinedHtml
                                }
                            },
                            update = { webView ->
                                if (webView.tag != combinedHtml) {
                                    webView.loadDataWithBaseURL(null, combinedHtml, "text/html", "UTF-8", null)
                                    webView.tag = combinedHtml
                                }
                            }
                        )
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            webViewRef?.apply {
                                stopLoading()
                                clearHistory()
                                loadUrl("about:blank")
                                removeAllViews()
                                destroy()
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Only Web Projects (with index.html) can be previewed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

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
        
        // Pre-create cache directories to silence Chromium's lack-of-directory logs
        try {
            val jsCacheDir = cacheDir.resolve("WebView/Default/HTTP Cache/Code Cache/js")
            val wasmCacheDir = cacheDir.resolve("WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!jsCacheDir.exists()) jsCacheDir.mkdirs()
            if (!wasmCacheDir.exists()) wasmCacheDir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onNavigateToEditor = { projectId, template ->
                                navController.navigate("editor/$projectId/$template")
                            }
                        )
                    }
                    composable("editor/{projectId}/{template}") { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                        val template = backStackEntry.arguments?.getString("template") ?: "Web Template (HTML, CSS, JS)"
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = ActivityBarBackground,
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            CodeEditorApp(
                                projectId = projectId,
                                template = template,
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorApp(projectId: String, template: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var showWebPreview by remember { mutableStateOf(false) }
    val initialFiles = remember(template) {
        if (template.contains("Web", ignoreCase = true)) {
            listOf(
                CodeFile("index.html", "<!DOCTYPE html>\n<html>\n<head>\n    <title>My Web App</title>\n    <link rel=\"stylesheet\" href=\"styles.css\">\n</head>\n<body>\n    <h1>Hello web development!</h1>\n    <p>Welcome to your site.</p>\n    <script src=\"app.js\"></script>\n</body>\n</html>"),
                CodeFile("styles.css", "body {\n    background-color: #121212;\n    color: #ffffff;\n    font-family: Arial, sans-serif;\n    display: flex;\n    flex-direction: column;\n    align-items: center;\n    justify-content: center;\n    height: 100vh;\n    margin: 0;\n}\nh1 {\n    color: #00E676;\n}"),
                CodeFile("app.js", "console.log('App successfully launched!');\nalert('Welcome!');\n"),
                CodeFile("README.md", "# Web Project\n\nThis is a simple web dev project template.\n")
            )
        } else {
            listOf(
                CodeFile("main.kt", "fun main() {\n    println(\"Hello, Blank Project!\")\n}\n"),
                CodeFile("README.md", "# Blank Project\n\nStart fresh! Use the buttons in the explorer sidebar to add files and folders.\n")
            )
        }
    }

    var filesList by remember(template) { mutableStateOf(initialFiles) }
    var selectedFile by remember(template) { mutableStateOf(initialFiles.firstOrNull { !it.isFolder } ?: initialFiles[0]) }
    var fileContents by remember(template) {
        mutableStateOf(initialFiles.associate { it.name to it.content }.toMutableMap())
    }

    val context = LocalContext.current
    var projectName by remember { mutableStateOf("Project") }
    LaunchedEffect(projectId) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.projectDao().getAllProjects().collect { projects ->
                projects.find { it.id == projectId }?.let {
                    projectName = it.name
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val onDeleteFile: (CodeFile) -> Unit = { file ->
        filesList = filesList.filter { it != file }
        val updatedContents = fileContents.toMutableMap()
        updatedContents.remove(file.name)
        fileContents = updatedContents
        if (selectedFile == file) {
            selectedFile = filesList.firstOrNull { !it.isFolder } ?: CodeFile("README.md", "", false)
        }
    }

    val onRenameFile: (CodeFile, String) -> Unit = { file, newName ->
        if (newName.isNotBlank() && file.name != newName) {
            val oldName = file.name
            filesList = filesList.map {
                if (it == file) it.copy(name = newName) else it
            }
            val updatedContents = fileContents.toMutableMap()
            val previousContent = updatedContents.remove(oldName) ?: file.content
            updatedContents[newName] = previousContent
            fileContents = updatedContents
            if (selectedFile.name == oldName) {
                selectedFile = selectedFile.copy(name = newName, content = previousContent)
            }
        }
    }

    val onDownloadFile: (CodeFile) -> Unit = { file ->
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val content = fileContents[file.name] ?: file.content
            val clip = ClipData.newPlainText("Downloaded Code", content)
            clipboard.setPrimaryClip(clip)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, content)
            }
            context.startActivity(Intent.createChooser(intent, "Download / Share ${file.name}"))
            
            Toast.makeText(context, "${file.name} copied to clipboard & share sheet opened!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val onCopyPath: (CodeFile) -> Unit = { file ->
        val path = "/storage/emulated/0/Documents/CodeEditor/$projectName/${file.name}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("File Path", path)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied Path: $path", Toast.LENGTH_SHORT).show()
    }

    var clipboardFile by remember { mutableStateOf<CodeFile?>(null) }
    var isCutOperation by remember { mutableStateOf(false) }

    val onCopyFile: (CodeFile) -> Unit = { file ->
        clipboardFile = file
        isCutOperation = false
        Toast.makeText(context, "Copied '${file.name}' to clipboard", Toast.LENGTH_SHORT).show()
    }

    val onCutFile: (CodeFile) -> Unit = { file ->
        clipboardFile = file
        isCutOperation = true
        Toast.makeText(context, "Cut '${file.name}' to clipboard", Toast.LENGTH_SHORT).show()
    }

    val onPasteFile: (CodeFile?) -> Unit = { target ->
        val clip = clipboardFile
        if (clip != null) {
            val isFolder = clip.isFolder
            if (isCutOperation) {
                var newName = clip.name
                val otherFiles = filesList.filter { it != clip }
                if (otherFiles.any { it.name == newName }) {
                    val base = newName.substringBeforeLast(".")
                    val ext = if (newName.contains(".") && !isFolder) "." + newName.substringAfterLast(".") else ""
                    var counter = 1
                    while (otherFiles.any { it.name == "${base}_paste$counter$ext" }) {
                        counter++
                    }
                    newName = "${base}_paste$counter$ext"
                }

                filesList = filesList.map {
                    if (it == clip) it.copy(name = newName) else it
                }
                
                if (!isFolder) {
                    val updatedContents = fileContents.toMutableMap()
                    val content = updatedContents.remove(clip.name) ?: clip.content
                    updatedContents[newName] = content
                    fileContents = updatedContents
                    if (selectedFile == clip) {
                        selectedFile = selectedFile.copy(name = newName, content = content)
                    }
                }
                Toast.makeText(context, "Moved '${clip.name}' to '$newName'", Toast.LENGTH_SHORT).show()
                clipboardFile = null
                isCutOperation = false
            } else {
                val base = clip.name.substringBeforeLast(".")
                val ext = if (clip.name.contains(".") && !isFolder) "." + clip.name.substringAfterLast(".") else ""
                var newName = "${base}_copy$ext"
                var counter = 1
                while (filesList.any { it.name == newName }) {
                    newName = "${base}_copy$counter$ext"
                    counter++
                }

                val duplicatedFile = CodeFile(newName, clip.content, isFolder)
                filesList = filesList + duplicatedFile
                if (!isFolder) {
                    val updated = fileContents.toMutableMap()
                    updated[newName] = fileContents[clip.name] ?: clip.content
                    fileContents = updated
                }
                Toast.makeText(context, "Copied and created '$newName'", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp
        
        if (isCompact) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = SidebarBackground, // Sidebar background
                        modifier = Modifier.width(280.dp),
                        windowInsets = WindowInsets(0)
                    ) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            ActivityBar(onBack = onBack)
                            Sidebar(
                                files = filesList,
                                selectedFile = selectedFile,
                                onFileSelected = {
                                    selectedFile = it
                                    scope.launch { drawerState.close() }
                                },
                                onCreateFile = { name ->
                                    if (name.isNotBlank()) {
                                        val newFile = CodeFile(name, "")
                                        filesList = filesList + newFile
                                        val updatedContents = fileContents.toMutableMap()
                                        updatedContents[name] = ""
                                        fileContents = updatedContents
                                        selectedFile = newFile
                                    }
                                },
                                onCreateFolder = { name ->
                                    if (name.isNotBlank()) {
                                        val newFolder = CodeFile(name, "", isFolder = true)
                                        filesList = filesList + newFolder
                                    }
                                },
                                onDeleteFile = onDeleteFile,
                                onRenameFile = onRenameFile,
                                onDownloadFile = onDownloadFile,
                                onCopyPath = onCopyPath,
                                onCopyFile = onCopyFile,
                                onCutFile = onCutFile,
                                onPasteFile = onPasteFile,
                                clipboardFile = clipboardFile
                            )
                        }
                    }
                }
            ) {
                // Main Editor Area
                Column(modifier = Modifier.fillMaxSize().background(EditorBackground)) {
                    // Mobile Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SidebarBackground)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, "Menu", tint = TextNormal)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = TextKeyword,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedFile.name, color = TextNormal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showWebPreview = true }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Run", tint = TextLineNumber)
                        }
                    }

                    Breadcrumbs(projectName = projectName, fileName = selectedFile.name)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    Column(modifier = Modifier.weight(1f)) {
                        // Editor Input
                        CodeEditorPane(
                            code = fileContents[selectedFile.name] ?: "",
                            onCodeChange = { newCode ->
                                val updated = fileContents.toMutableMap()
                                updated[selectedFile.name] = newCode
                                fileContents = updated
                            },
                            modifier = Modifier.weight(0.6f)
                        )

                        HorizontalDivider(color = BorderColor, thickness = 1.dp)

                        // Terminal Pane
                        TerminalPane(modifier = Modifier.weight(0.4f))
                    }

                    // Status Bar
                    StatusBar()
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Activity Bar (Far Left)
                ActivityBar(onBack = onBack)

                // Explorer Sidebar
                Sidebar(
                    files = filesList,
                    selectedFile = selectedFile,
                    onFileSelected = { selectedFile = it },
                    onCreateFile = { name ->
                        if (name.isNotBlank()) {
                            val newFile = CodeFile(name, "")
                            filesList = filesList + newFile
                            val updatedContents = fileContents.toMutableMap()
                            updatedContents[name] = ""
                            fileContents = updatedContents
                            selectedFile = newFile
                        }
                    },
                    onCreateFolder = { name ->
                        if (name.isNotBlank()) {
                            val newFolder = CodeFile(name, "", isFolder = true)
                            filesList = filesList + newFolder
                        }
                    },
                    onDeleteFile = onDeleteFile,
                    onRenameFile = onRenameFile,
                    onDownloadFile = onDownloadFile,
                    onCopyPath = onCopyPath,
                    onCopyFile = onCopyFile,
                    onCutFile = onCutFile,
                    onPasteFile = onPasteFile,
                    clipboardFile = clipboardFile
                )
                VerticalDivider(color = BorderColor, thickness = 1.dp)

                // Main Editor Area
                Column(modifier = Modifier
                    .weight(1f)
                    .background(EditorBackground)) {
                    // Tabs Row
                    EditorTabs(selectedFile = selectedFile, onPlay = { showWebPreview = true })
                    Breadcrumbs(projectName = projectName, fileName = selectedFile.name)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

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
        
        if (showWebPreview) {
            WebPreviewDialog(
                fileContents = fileContents,
                onDismiss = { showWebPreview = false }
            )
        }
    }
}

@Composable
fun ActivityBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(ActivityBarBackground)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextNormal, modifier = Modifier.size(28.dp).clickable(onClick = onBack))
        Icon(Icons.Filled.Description, contentDescription = "Explorer", tint = TextNormal, modifier = Modifier.size(28.dp))
        Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextLineNumber, modifier = Modifier.size(28.dp))
        Icon(Icons.Filled.Code, contentDescription = "Source Control", tint = TextLineNumber, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextLineNumber, modifier = Modifier.size(28.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Sidebar(
    files: List<CodeFile>,
    selectedFile: CodeFile,
    onFileSelected: (CodeFile) -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFile: (CodeFile) -> Unit,
    onRenameFile: (CodeFile, String) -> Unit,
    onDownloadFile: (CodeFile) -> Unit,
    onCopyPath: (CodeFile) -> Unit,
    onCopyFile: (CodeFile) -> Unit,
    onCutFile: (CodeFile) -> Unit,
    onPasteFile: (CodeFile?) -> Unit,
    clipboardFile: CodeFile?
) {
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    var selectedFileForMenu by remember { mutableStateOf<CodeFile?>(null) }
    var renamingFileId by remember { mutableStateOf<CodeFile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false; inputName = "" },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            onCreateFile(inputName.trim())
                            showCreateFileDialog = false
                            inputName = ""
                        }
                    },
                    enabled = inputName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false; inputName = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false; inputName = "" },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            onCreateFolder(inputName.trim())
                            showCreateFolderDialog = false
                            inputName = ""
                        }
                    },
                    enabled = inputName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false; inputName = "" }) {
                    Text("Cancel")
                }
            }
        )
    }



    if (showDeleteDialog && selectedFileForMenu != null) {
        val file = selectedFileForMenu!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (file.isFolder) "Delete Folder" else "Delete File") },
            text = {
                Text("Are you sure you want to delete '${file.name}'? This action is permanent.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(file)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(SidebarBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "EXPLORER",
                color = TextLineNumber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New File",
                    tint = TextLineNumber,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { showCreateFileDialog = true }
                )
                Icon(
                    imageVector = Icons.Filled.CreateNewFolder,
                    contentDescription = "New Folder",
                    tint = TextLineNumber,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { showCreateFolderDialog = true }
                )
            }
        }

        LazyColumn {
            items(files) { file ->
                val isSelected = file == selectedFile
                var showContextMenu by remember { mutableStateOf(false) }
                
                val isRenamingThis = renamingFileId == file
                
                val itemModifier = if (isRenamingThis) {
                    Modifier
                        .fillMaxWidth()
                        .background(if (isSelected && !file.isFolder) TabActiveBackground else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onLongClick = {
                                showContextMenu = true
                            },
                            onClick = {
                                if (!file.isFolder) onFileSelected(file)
                            }
                        )
                        .background(if (isSelected && !file.isFolder) TabActiveBackground else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                }

                Box {
                    Row(
                        modifier = itemModifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isFolder) Icons.Filled.KeyboardArrowDown else Icons.Filled.Description,
                            contentDescription = null,
                            tint = if (file.isFolder) TextNormal else TextKeyword,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (isRenamingThis) {
                            var inlineNameText by remember { mutableStateOf(file.name) }
                            BasicTextField(
                                value = inlineNameText,
                                onValueChange = { inlineNameText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (inlineNameText.isNotBlank()) {
                                            onRenameFile(file, inlineNameText.trim())
                                        }
                                        renamingFileId = null
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Transparent)
                                    .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Confirm",
                                tint = Color(0xFF00E676),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        if (inlineNameText.isNotBlank()) {
                                            onRenameFile(file, inlineNameText.trim())
                                        }
                                        renamingFileId = null
                                    }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancel",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        renamingFileId = null
                                    }
                            )
                        } else {
                            Text(
                                text = file.name,
                                color = if (isSelected && !file.isFolder) Color.White else TextNormal,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        modifier = Modifier.background(SidebarBackground)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = TextNormal) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = TextMuted) },
                            onClick = {
                                showContextMenu = false
                                renamingFileId = file
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy", color = TextNormal) },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = TextMuted) },
                            onClick = {
                                showContextMenu = false
                                onCopyFile(file)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cut", color = TextNormal) },
                            leadingIcon = { Icon(Icons.Filled.ContentCut, contentDescription = null, tint = TextMuted) },
                            onClick = {
                                showContextMenu = false
                                onCutFile(file)
                            }
                        )
                        if (clipboardFile != null) {
                            DropdownMenuItem(
                                text = { Text("Paste", color = TextNormal) },
                                leadingIcon = { Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = TextMuted) },
                                onClick = {
                                    showContextMenu = false
                                    onPasteFile(file)
                                }
                            )
                        }
                        if (!file.isFolder) {
                            DropdownMenuItem(
                                text = { Text("Download", color = TextNormal) },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, tint = TextMuted) },
                                onClick = {
                                    showContextMenu = false
                                    onDownloadFile(file)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Copy Path", color = TextNormal) },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = TextMuted) },
                            onClick = {
                                showContextMenu = false
                                onCopyPath(file)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFFF5252)) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                            onClick = {
                                showContextMenu = false
                                selectedFileForMenu = file
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Breadcrumbs(projectName: String, fileName: String, modifier: Modifier = Modifier) {
    val items = remember(projectName, fileName) {
        val list = mutableListOf<String>()
        list.add(projectName)
        if (fileName.contains("/")) {
            list.addAll(fileName.split("/").filter { it.isNotEmpty() })
        } else {
            list.add(fileName)
        }
        list
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(EditorBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.size - 1
            val isFirst = index == 0
            
            val icon = when {
                isFirst -> Icons.Filled.FolderOpen
                !isLast -> Icons.Filled.Folder
                item.endsWith(".html") || item.endsWith(".css") || item.endsWith(".js") || item.endsWith(".kt") -> Icons.Filled.Code
                else -> Icons.Filled.Description
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isLast) AccentColor else TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = item,
                color = if (isLast) TextNormal else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (isLast) FontWeight.Medium else FontWeight.Normal
            )
            
            if (!isLast) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextLineNumber,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun EditorTabs(selectedFile: CodeFile, onPlay: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SidebarBackground),
            verticalAlignment = Alignment.CenterVertically
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
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Run", tint = TextLineNumber)
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}

@Composable
fun CodeEditorPane(code: String, onCodeChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable()
            )
        }
    }
}

@Composable
fun TerminalPane(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as android.app.Activity
    val coordinator = remember { com.example.terminal.TerminalCoordinator(context) }
    var terminalViewRef by remember { mutableStateOf<TerminalView?>(null) }

    DisposableEffect(Unit) {
        val sessionClient = com.example.terminal.AppTerminalSessionClient(
            onRefresh = { 
                terminalViewRef?.post { 
                    terminalViewRef?.onScreenUpdated() 
                } 
            },
            onFinished = { }
        )
        coordinator.start(sessionClient)
        terminalViewRef?.let { coordinator.attach(it, requestFocus = false) }
        
        onDispose { coordinator.stop() }
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
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            factory = { ctx ->
                TerminalView(ctx, null).apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    keepScreenOn = true
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setTextSize((14 * ctx.resources.displayMetrics.scaledDensity).toInt())

                    val client = com.example.terminal.AppTerminalViewClient(activity, this)
                    setTerminalViewClient(client)

                    coordinator.attach(this)
                    terminalViewRef = this
                }
            },
            update = { view ->
                // Called on recompose. Safely attach session if not already attached
                coordinator.attach(view, requestFocus = false)
            }
        )
    }
}

@Composable
fun StatusBar() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isSaving by remember { mutableStateOf(false) }

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
            
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier
                    .clickable {
                        if (isSaving) return@clickable
                        isSaving = true
                        try {
                            val process = Runtime.getRuntime().exec("logcat -d")
                            val bufferedReader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                            val log = java.lang.StringBuilder()
                            var line: String? = bufferedReader.readLine()
                            while (line != null) {
                                log.append(line).append("\n")
                                line = bufferedReader.readLine()
                            }
                            Runtime.getRuntime().exec("logcat -c")
                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                            val debugFile = java.io.File(downloadsDir, "CodeEditor_DebugLog_${System.currentTimeMillis()}.txt")
                            debugFile.writeText(log.toString())
                            android.widget.Toast.makeText(context, "Debug log saved to Downloads. Log cleared.", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(context, "Failed to save log", android.widget.Toast.LENGTH_SHORT).show()
                        } finally {
                            isSaving = false
                        }
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Build, contentDescription = "Debug Log", tint = StatusBarText, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Debug Log", color = StatusBarText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
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

