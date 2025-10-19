package processing.p5js

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import processing.app.*
import processing.app.syntax.JEditTextArea
import processing.app.syntax.PdeTextArea
import processing.app.syntax.PdeTextAreaDefaults
import processing.app.ui.*
import processing.app.ui.theme.ProcessingTheme
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import javax.swing.JMenu
import javax.swing.JMenuItem
import kotlin.jvm.optionals.getOrNull


class p5jsEditor(base: Base, path: String?, state: EditorState?, mode: Mode?): Editor(base, path, state, mode) {

    val scope = CoroutineScope(Dispatchers.Default)
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    var sketchProcess: Process? = null

    init {
        scope.launch {
            val folder = sketch.folder
            val name = sketch.name

            // TODO: `getContentFile` is deprecated; move to JAR resource system if time allows
            var javascriptFolder = mode?.getContentFile("js")
            // TODO: Better error handling in case Electron scaffolding is not found
            javascriptFolder?.listFiles()?.forEach { it.copyTo(File(folder, it.name), true) }

            // TODO: Find a better way to load actual sketch file
            val indexHtml = """
                <!DOCTYPE html>
                <html lang="en">
                  <head>
                    <meta charset="utf-8" />
                    <style>
                    html, body {
                      margin: 0;
                      padding: 0;
                    }
                    canvas {
                      display: block;
                    }
                    </style>
                  </head>

                  <body>
                    <script src="renderer.js"></script>
                    <script src="./node_modules/p5/lib/p5.js"></script>
                    <script src="./node_modules/p5/lib/addons/p5.sound.js"></script>
                    <script src="$name.js"></script>
                  </body>
                </html>
            """.trimIndent()
            File("$folder/index.html").writeText(indexHtml)

            // TODO: refactor into functions
            // Check whether `pnpm` is already installed; horrible code—my apologies!
            statusNotice("Looking for pnpm…")
            try {
                runCommand("pnpm -v")
            }
            catch (e: Exception) {
                statusNotice("pnpm not found. Installing pnpm…")
                if (isWindows) {
                    runCommand("powershell -command \"Invoke-WebRequest https://get.pnpm.io/install.ps1 -UseBasicParsing | Invoke-Expression\"")
                }
                else {
                    runCommand("chmod u+x ${mode?.folder}/install.sh")
                    runCommand("${mode?.folder}/install.sh")
                }

                statusNotice("Installing Node via pnpm…")
                runCommand("pnpm env use --global lts") {
                    statusNotice("Installing Node dependencies…")
                }
            }

            // --dangerously-allow-all-builds allows electron in particular to install properly
            runCommand("pnpm install --dangerously-allow-all-builds") {
                statusNotice("All done! Enjoy p5.js mode.")
            }
        }
    }

    override fun createTextArea(): JEditTextArea {
        return PdeTextArea(PdeTextAreaDefaults(), p5jsInputHandler(this), this)
    }

    override fun createToolbar(): EditorToolbar {
        return p5jsEditorToolbar(this)
    }

    override fun createFormatter(): Formatter {
        return p5jsFormatter()
    }

    override fun buildFileMenu(): JMenu {
        val exportApp: JMenuItem = Toolkit.newJMenuItemShift(Language.text("menu.file.export_application"), 'E'.code)
        exportApp.addActionListener(ActionListener { e: ActionEvent? ->
            if (sketch.isUntitled || sketch.isReadOnly) {
                Messages.showMessage("Save First", "Please first save the sketch.");
            } else {
                // TODO: I’m sure this is not the best way to ensure that this runs async, so the ActionListener can return
                // but works for now
                scope.launch {
                    handleExport()
                }
            }
        })
        return super.buildFileMenu(arrayOf(exportApp))
    }

    private fun handleExport() {
        statusNotice(Language.text("export.notice.exporting"))

        val electronBuilderBin = File(sketch.folder, "node_modules/.bin/electron-builder")
        if (!electronBuilderBin.exists()) {
            runCommand("pnpm install --dangerously-allow-all-builds --force")
        }

        runCommand("pnpm app:pack") {
            Platform.openFolder(sketch.folder)
            statusNotice(Language.text("export.notice.exporting.done"))
        }
    }

//    override fun handleSaveAs(): Boolean {
//        val saved = super.handleSaveAs()
//        statusNotice("Rebuilding Node dependencies…")
//        TODO: Saving is async and might not be finished once the function returns
//        runCommand("pnpm install --force", onFinished = {
//            statusNotice("Rebuilding Node dependencies… Done.")
//        })
//        return saved
//    }

    override fun buildSketchMenu(): JMenu {
        return super.buildSketchMenu(arrayOf())
    }

    override fun handleImportLibrary(name: String?) {
//        TODO("Not yet implemented")
    }

    override fun buildHelpMenu(): JMenu {
        return JMenu()
    }

    override fun handleOpenInternal(path: String?) {
        super.handleOpenInternal(path)
    }

    override fun getCommentPrefix(): String {
        return "// "
    }

    override fun internalCloseRunner() {
        sketchProcess?.destroy()
    }

    override fun deactivateRun() {
        sketchProcess?.destroy()
        toolbar.deactivateRun()
    }

    override fun createFooter(): EditorFooter {
        val footer = super.createFooter()
        val composePanel = ComposePanel()
        composePanel.setContent {
            ProcessingTheme {
                var packageToInstall by remember { mutableStateOf("") }
                var packagesSearched by remember { mutableStateOf(listOf<String>()) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column {
                        Text("Add packages", fontWeight = FontWeight.Bold)
                        // TODO Do not forget localization!
                        Text("Search for and use JavaScript packages from npm here. By selecting a package, pnpm will add it to the dependencies of this sketch. It is then ready for you to import and use.")
                        Row {
                            OutlinedTextField(
                                packageToInstall,
                                singleLine = true,
                                // TODO Hot mess—apologies! (Look into ViewModel, LaunchedEffect, debounce the onValueChange handler!)
                                onValueChange = {
                                    packageToInstall = it
                                    // TODO Need a better debounce
                                    if (packageToInstall.length > 4) {
                                        val npmConn =
                                            URL("https://registry.npmjs.org/-/v1/search?text=$packageToInstall")
                                                .openConnection()
                                        val npmResponseRaw = npmConn.getInputStream().readAllBytes().decodeToString()
                                        val npmResponse: JsonObject = Json.decodeFromString(npmResponseRaw)
                                        val npmPackages = npmResponse["objects"]!!.jsonArray
                                        packagesSearched = npmPackages.map { it.jsonObject["package"]!!.jsonObject["name"]!!.jsonPrimitive.content }
                                        packageToInstall = packagesSearched[0]
                                    }
                                })
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (packageToInstall.isNotBlank()) {
                                    // TODO Better error handling
                                    runCommand("pnpm add $packageToInstall --dangerously-allow-all-builds")
                                    packageToInstall = ""
                                }
                            }) {
                                Text("Add")
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    LazyColumn {
                        itemsIndexed(packagesSearched) { index, pkg ->
                            Text(text = pkg, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.fillMaxWidth().padding(4.dp))
                            Divider()
                        }
                    }
                }
            }
        }
        footer.addPanel(composePanel, "NPM")
        return footer
    }

    private fun filenameToCodeIndex(filename: String) {

    }

    fun runCommand(action: String, directory: File = sketch.folder, onFinished: () -> Unit = {}) {
        try {
            // TODO: Get rid of magic strings. Better way to distinguish “endless” processes and processes we wait for?
            if (action == "pnpm sketch:start") {
                sketchProcess?.destroy()
            }

            val processBuilder = ProcessBuilder()

            // Set the command based on the operating system
            val shell = System.getenv("SHELL")
            val command = if (isWindows) {
                listOf("cmd", "/c", action)
            } else {
                listOf(shell, "-ci", action)
            }

            processBuilder.command(command)
            processBuilder.directory(directory)

            val process = processBuilder.start()

            if (action == "pnpm sketch:start") {
                sketchProcess = process;
            }

            // Handle output stream
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String

            while (reader.readLine().also { line = it } != null) {
                // TODO: so much refactoring!
                // Only check for errors when running the sketch
                if (action.startsWith("npx") && line.startsWith("error")) {
                    // TODO: more robust data exchange, double-check with @Stef
                    // TODO: `statusError` does not do anything with column of a SketchException
                    val ( msgType, msgText, msgFile, msgLine, msgCol ) = line.split("|")
                    statusError(SketchException(msgText, 0, msgLine.toInt()-1, msgCol.toInt()))
                    continue
                }

                println(line)
            }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw RuntimeException("Command failed with non-zero exit code $exitCode.")
            }

            onFinished()
        } catch (e: Exception) {
            statusError("Failed to run `$action`: ${e.message}")
        }
    }
}