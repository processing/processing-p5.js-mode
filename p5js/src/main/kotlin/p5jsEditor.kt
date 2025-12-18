package processing.p5js

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.serialization.json.*
import processing.app.*
import processing.app.syntax.JEditTextArea
import processing.app.syntax.PdeTextArea
import processing.app.syntax.PdeTextAreaDefaults
import processing.app.ui.*
import processing.app.ui.theme.PDETheme
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import javax.swing.JMenu
import javax.swing.JMenuItem


class p5jsEditor(base: Base, path: String?, state: EditorState?, mode: Mode?) : Editor(base, path, state, mode) {

    val scope = CoroutineScope(Dispatchers.Default)
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    var processes: MutableList<Process> = mutableListOf()

    init {
        scope.launch {
            // Copy all Electron scaffolding from mode’s `js` folder
            var javascriptFolder = mode?.getContentFile("js")
            javascriptFolder?.resolve("electron")?.copyRecursively(sketch.folder.resolve("electron"), true)

            // Only copy `package.json` and `pnpm-lock.yaml` if not existent
            // Some examples bring their own
            try {
                javascriptFolder?.resolve("package.json")?.copyTo(sketch.folder.resolve("package.json"))
                javascriptFolder?.resolve("pnpm-lock.yaml")?.copyTo(sketch.folder.resolve("pnpm-lock.yaml"))
            } catch (e: FileAlreadyExistsException) {
                Messages.log("File already exists: ${e.message}")
                // TODO: How to differentiate example with own `package.json` and saved sketch?
            }

            createIndexHtml()

            // TODO: refactor into functions, pick up crucial information from stdout
            statusNotice("Looking for pnpm…")
            try {
                runCommand("pnpm -v")
            } catch (e: Exception) {
                statusNotice("pnpm not found. Installing pnpm…")
                if (isWindows) {
                    runCommand("powershell -command \"Invoke-WebRequest https://get.pnpm.io/install.ps1 -UseBasicParsing | Invoke-Expression\"")
                } else {
                    runCommand("chmod u+x ${mode?.folder}/install.sh")
                    runCommand("${mode?.folder}/install.sh")
                }

                statusNotice("Installing Node via pnpm…")
                runCommand("pnpm env use --global lts")
            }

            statusNotice("All done! Enjoy p5.js mode.")
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

        scope.launch {
            runCommand("pnpm app:pack")
            Platform.openFolder(sketch.folder)
            statusNotice(Language.text("export.notice.exporting.done"))
        }
    }

    override fun buildSketchMenu(): JMenu {
        val runItem = Toolkit.newJMenuItem(Language.text("menu.sketch.run"), 'R'.code)
        runItem.addActionListener { e: ActionEvent? -> toolbar.handleRun(0) }

        val presentItem = Toolkit.newJMenuItemShift(Language.text("menu.sketch.present"), 'R'.code)
        presentItem.addActionListener { e: ActionEvent? -> toolbar.handleRun(java.awt.Event.SHIFT_MASK) }

        val stopItem = JMenuItem(Language.text("menu.sketch.stop"))
        stopItem.addActionListener { e: ActionEvent? ->
            toolbar.handleStop()
        }
        return super.buildSketchMenu(arrayOf(runItem, presentItem, stopItem))
    }

    override fun handleImportLibrary(name: String?) {
//        TODO("Not yet implemented")
    }

    override fun buildHelpMenu(): JMenu {
        return JMenu()
    }

    override fun handleOpenInternal(path: String?) {
        try {
            sketch = Sketch(path, this)

            // If sketch is read-only move all files to temporary folder
            // to allow them to run without saving first
            if (sketch.isReadOnly) {
                val newSketchFolder = sketch.makeTempFolder().resolve(sketch.name)
                val mainSketchFile = File(path, "sketch-main.js").name
                sketch.folder.copyRecursively(newSketchFolder)
                sketch = Sketch(newSketchFolder.resolve(mainSketchFile).path, this)
            }
        } catch (e: IOException) {
            throw EditorException("Could not create the sketch.", e)
        }

        header.rebuild()
        updateTitle()
    }

    override fun getCommentPrefix(): String {
        return "// "
    }

    override fun internalCloseRunner() {
    }

    override fun deactivateRun() {
        processes.forEach { it.destroy() }
        updateToolbar()
    }

    override fun createFooter(): EditorFooter {
        val footer = super.createFooter()
        return footer;

        val composePanel = ComposePanel()
        composePanel.setContent {
            PDETheme(darkTheme = false) {
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
                                        packagesSearched =
                                            npmPackages.map { it.jsonObject["package"]!!.jsonObject["name"]!!.jsonPrimitive.content }
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
                            Text(
                                text = pkg,
                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                            )
                            Divider()
                        }
                    }
                }
            }
        }
        footer.addPanel(composePanel, "NPM")
        return footer
    }

    fun createIndexHtml() {
        val htmlCode = createHTMLDocument().html {
            comment("This file is managed by the p5.js mode. Do not change manually!")
            head {
                meta { charset = "utf-8" }
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1"
                }
                title { +sketch.mainName }
                link(href = "style.css", rel = "stylesheet")
            }
            body {
                script(src = "renderer.js") {}
                script(src = "../node_modules/p5/lib/p5.min.js") {}
                script(src = "../node_modules/p5/lib/addons/p5.sound.js") {}
                sketch.code.filter { code -> code.file.extension == "js" }.forEach { code ->
                    script(src = "../${code.file.name}") {}
                }
                script(src = "resizer.js") {}
            }
        }.serialize(true)

        sketch.folder.resolve("electron/index.html").writeText(htmlCode)
    }

    fun updateToolbar() {
        if (processes.any { it.isAlive }) {
            toolbar.activateRun()
        } else {
            toolbar.deactivateRun()
        }
    }

    fun runSketch(present: Boolean) {
        createIndexHtml()
        deactivateRun()
        statusNotice("Starting up sketch…")


        scope.launch {
            try {
                val packageJson = sketch.folder.resolve("package.json")
                val hashFile = sketch.folder.resolve("electron/.package_json_hash")
                val newHash = packageJson.readText().hashCode()
                val oldHash = hashFile.let { if (it.exists()) it.readText().toInt() else null }
                if (newHash != oldHash) {
                    statusNotice("Installing Node dependencies…")
                    runCommand("pnpm install --dangerously-allow-all-builds")
                    hashFile.writeText(newHash.toString())
                }
                statusNotice("Running sketch…")
                val builder = builder("PRESENT=$present pnpm sketch:start", sketch.folder)
                val process = builder.start()
                processes.add(process)
                updateToolbar()

                // Handle output stream
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    // TODO: so much refactoring!
                    // Only check for errors when running the sketch
                    if (line?.startsWith("error") == true) {
                        // TODO: more robust data exchange, double-check with @Stef
                        // TODO: `statusError` does not do anything with column of a SketchException
                        val (msgType, msgText, msgFile, msgLine, msgCol) = line.split("|")
                        statusError(processing.utils.SketchException(msgText, 0, msgLine.toInt() - 1, msgCol.toInt()))
                        continue
                    }

                    println(line)
                }
                process.waitFor()
                if (processes.lastOrNull() == process) {
                    statusNotice("Sketch stopped.")
                }
                processes.remove(process)
                updateToolbar()

            } catch (e: Exception) {
                statusError("Failed to run sketch: ${e.message}")
            }
        }
    }


    fun runCommand(action: String, directory: File = sketch.folder) {
        try {
            val processBuilder = builder(action, directory)
            val process = processBuilder.start()
            // suspend fun to wait for process to finish
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw RuntimeException("Command failed with non-zero exit code $exitCode.")
            }
        } catch (e: Exception) {
            statusError("Failed to run `$action`: ${e.message}")
        }
    }

    private fun builder(action: String, directory: File): ProcessBuilder {
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
        return processBuilder
    }
}