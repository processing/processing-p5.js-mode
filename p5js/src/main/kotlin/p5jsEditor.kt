package processing.p5js

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import processing.app.Base
import processing.app.Formatter
import processing.app.Mode
import processing.app.syntax.JEditTextArea
import processing.app.syntax.PdeInputHandler
import processing.app.syntax.PdeTextArea
import processing.app.syntax.PdeTextAreaDefaults
import processing.app.ui.Editor
import processing.app.ui.EditorState
import processing.app.ui.EditorToolbar
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.swing.JMenu

class p5jsEditor(base: Base, path: String?, state: EditorState?, mode: Mode?): Editor(base, path, state, mode) {

    val scope = CoroutineScope(Dispatchers.Default)
    init {
        scope.launch {
            val folder = sketch.folder
            val name = sketch.name

            val packageJsonName = "package.json"

            val packageJson = loadPackageJson("$folder/$packageJsonName")
            packageJson.devDependencies["electron"] = "^33.2.1"
            packageJson.sketch = "$name.js"
            savePackageJson("$folder/$packageJsonName", packageJson)

            runNpmActions(folder, TYPE.npm, listOf("install"))

            val indexHtml = """
                <!DOCTYPE html>
                <html lang="en">
                    <head>
                        <script src="https://cdnjs.cloudflare.com/ajax/libs/p5.js/1.11.1/p5.js"></script>
                        <script src="https://cdnjs.cloudflare.com/ajax/libs/p5.js/1.11.1/addons/p5.sound.min.js"></script>
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
                        <script src="./renderer.js"></script>
                        <script src="${packageJson.sketch}"></script>
                    </body>
                </html>
            """.trimIndent()

            val mainJS = """
               const path = require('node:path')
               const { app, BrowserWindow, globalShortcut, ipcMain } = require('electron')

                const createWindow = () => {
                    const win = new BrowserWindow({
                        width: 400,
                        height: 400,
                        useContentSize: true,
                        autoHideMenuBar: true,
                        alwaysOnTop: true,
                        webPreferences: {
                            nodeIntegration: true,
                            preload: path.join(__dirname, "preload.js")
                        },
                    })
                
                    win.loadFile('index.html')
                
                    // Register the 'Escape' key shortcut
                    globalShortcut.register('Escape', () => {
                        win.close()
                    })
                
                    // Unregister the shortcut when window is closed
                    win.on('closed', () => {
                        globalShortcut.unregister('Escape')
                    })
                }
                
                app.on('window-all-closed', () => {
                    // Unregister all shortcuts when app is closing
                    globalShortcut.unregisterAll()
                    app.quit()
                })
                
                app.whenReady().then(() => {
                    ipcMain.on("send-message", (event, message) => {
                        console.log(message);
                    });
                    createWindow()
                })
            """.trimIndent()

            val preloadJS = """
                const { contextBridge, ipcRenderer } = require("electron");
                
                contextBridge.exposeInMainWorld("electron", {
                    sendMessage: (message) => ipcRenderer.send("send-message", message)
                });
            """.trimIndent()

            val rendererJS = """
                const sendToMainHandler = {
                    get(target, prop) {
                        const consoleMethod = target[prop];
                        // Only intercept methods, return properties
                        if (typeof consoleMethod !== "function") {
                            return Reflect.get(...arguments);
                        }
                        return new Proxy(consoleMethod, {
                            apply(target, thisArg, args) {
                                // Notify main process via own API through contextBridge
                                window.electron.sendMessage({
                                    level: prop,
                                    msgArgs: args
                                });
                                // Retain original behavior
                                return Reflect.apply(...arguments);
                            }
                        });
                    }
                };
                window.console = new Proxy(console, sendToMainHandler);
            """.trimIndent()

            File("$folder/index.html").writeText(indexHtml)
            File("$folder/main.js").writeText(mainJS)
            File("$folder/preload.js").writeText(preloadJS)
            File("$folder/renderer.js").writeText(rendererJS)
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

    enum class TYPE{
        npm, npx
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