package processing.p5js

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import processing.app.Base
import processing.app.Formatter
import processing.app.Mode
import processing.app.Platform
import processing.app.syntax.JEditTextArea
import processing.app.syntax.PdeTextArea
import processing.app.syntax.PdeTextAreaDefaults
import processing.app.ui.Editor
import processing.app.ui.EditorFooter
import processing.app.ui.EditorState
import processing.app.ui.EditorToolbar
import processing.app.ui.theme.ProcessingTheme
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import javax.swing.JMenu


class p5jsEditor(base: Base, path: String?, state: EditorState?, mode: Mode?): Editor(base, path, state, mode) {

    val scope = CoroutineScope(Dispatchers.Default)
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
                    <script src="./node_modules/p5/lib/p5.min.js"></script>
                    <script src="./node_modules/p5.sound/dist/p5.sound.min.js"></script>
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
                    <script src="$name.js"></script>
                  </body>
                </html>
            """.trimIndent()
            File("$folder/index.html").writeText(indexHtml)

            // TODO: Install `pnpm` automatically, stand-alone, and use as Node manager
            runNpmActions(folder, TYPE.npm, listOf("install", "-g", "pnpm"))
            // --dangerously-allow-all-builds allows electron in particular to install properly
            runNpmActions(folder, TYPE.pnpm, listOf("install", "--dangerously-allow-all-builds"))
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
        return super.buildFileMenu(arrayOf())
    }

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
        processes.forEach { it.destroy() }
    }


    override fun deactivateRun() {
        processes.forEach { it.destroy() }
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
                                    runNpmActions(sketch.folder, TYPE.pnpm, listOf("add", packageToInstall, "--dangerously-allow-all-builds"))
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

    enum class TYPE{
        npm, pnpm, npx
    }

    val processes = mutableListOf<Process>()
    fun runNpmActions(directory: File, type: TYPE, actions: List<String>, onFinished: () -> Unit = {}) {


        // Wait for previous processes to finish
        processes.forEach { it.waitFor() }

        val processBuilder = ProcessBuilder()
        // Set the command based on the operating system
        val command = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("cmd", "/c", type.name , *actions.toTypedArray())
        } else {
            listOf(type.name, *actions.toTypedArray())
        }

        processBuilder.command(command)
        processBuilder.directory(directory)

        try {
            val process = processBuilder.start()
            processes.add(process)

            // Handle output stream
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println(line)
            }


            // Wait for the process to complete
            val exitCode = process.waitFor()
            processes.remove(process)
            onFinished()
            if (exitCode != 0) {
                throw RuntimeException("npm install failed with exit code $exitCode")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to run npm install", e)
        }
    }
}