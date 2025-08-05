package processing.p5js

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PackageJson(
    val name: String,
    val version: String,
    val main: String = "main.js",
    val dependencies: MutableMap<String, String> = mutableMapOf(),
    val devDependencies: MutableMap<String, String> = mutableMapOf(),
    var sketch: String = "sketch.js"
)

fun loadPackageJson(path: String): PackageJson {
    if(!File(path).exists()) {
        return PackageJson("p5js", "1.0.0")
    }
    val jsonString = File(path).readText()
    return Json.decodeFromString<PackageJson>(jsonString)
}

@OptIn(ExperimentalSerializationApi::class)
fun savePackageJson(path: String, packageJson: PackageJson) {
    val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  " // Use 2 spaces for indentation (npm standard)
        encodeDefaults = true       // Include default values in output
        explicitNulls = false       // Don't include null values in output
        ignoreUnknownKeys = true    // Don't fail on unknown keys during serialization
    }
    val jsonString = json.encodeToString(PackageJson.serializer(), packageJson)
    File(path).writeText(jsonString)
}