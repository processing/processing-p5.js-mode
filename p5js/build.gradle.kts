import org.jetbrains.compose.internal.de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.konan.properties.suffix
import kotlin.text.replace

plugins {
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version "1.9.0"

    // TODO Unclear whether the whole Compose dependency is necessary
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
}

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jogamp.org/deployment/maven") }
}

dependencies {
    compileOnly(project(":app"))
    compileOnly(project(":app:utils"))
    implementation(project(":core"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0")

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)
    implementation(libs.material3)
}

tasks.register<Download>("includeP5jsExamples"){
    val examples = layout.buildDirectory.file("tmp/p5-examples.zip")
    src("https://github.com/processing/p5.js-website/archive/refs/heads/2.0.zip")
    dest(examples)
    overwrite(false)
    doLast{
        copy{
            from(zipTree(examples)){ // remove top level directory
                include("*/src/content/examples/en/**/*")
                exclude("**/description.mdx")
                eachFile{
                    relativePath = RelativePath(true, *relativePath.segments.drop(5).toTypedArray())
                }
                eachFile{
                    if(name != "code.js"){ return@eachFile }

                    val parentName = this.file.parentFile.name
                    name = "$parentName.js"
                }
                eachFile {
                    // if the file is .js and not in a directory named of itself, move it to such a directory
                    if(!name.endsWith(".js")){ return@eachFile}
                    val parentName = this.file.parentFile.name
                    if(parentName == name.removeSuffix(".js")){ return@eachFile }
                    relativePath = RelativePath(true, *relativePath.segments.dropLast(1).toTypedArray(), name.removeSuffix(".js"), name)
                }
                // if a sketch folder starts with a digit, remove that digit and the following dash
                eachFile {
                    val parent = this.file.parentFile
                    val parentName = parent.name
                    val regex = Regex("^\\d+_")
                    if(regex.containsMatchIn(parentName)){
                        val newParentName = parentName.replace(regex, "")
                        relativePath = RelativePath(true, *relativePath.segments.dropLast(2).toTypedArray(), newParentName, newParentName.suffix("js"))
                    }
                }
            }
            into(layout.buildDirectory.dir("mode/examples/Basics"))
        }
    }
}

tasks.register<Copy>("createMode") {
    dependsOn("jar", "includeP5jsExamples")
    into(layout.buildDirectory.dir("mode"))
    // TODO Why is there a duplicate in the first place?
    duplicatesStrategy = DuplicatesStrategy.WARN

    from(layout.projectDirectory.dir("library")) {
        include ("**")
    }

    from(layout.projectDirectory) {
        include("js/**")
    }

    from(configurations.runtimeClasspath) {
        into("mode")
    }

    from(tasks.jar) {
        into("mode")
    }
}


tasks.register<Zip>("createZip") {
    dependsOn("createMode")
    from(tasks.named("createMode"))
    into(project.name)

    destinationDirectory.set(layout.buildDirectory)
}

tasks.register<Zip>("createPdex") {
    dependsOn("createMode")
    from(tasks.named("createMode"))
    into(project.name)

    archiveExtension.set("pdex")
    destinationDirectory.set(layout.buildDirectory)
}

tasks.register<Copy>("includeMode") {
    dependsOn("createMode")
    from(tasks.named("createMode"))
    into(project(":app").layout.buildDirectory.dir("resources-bundled/common/modes/p5js"))
}

project(":app").tasks.named("includeProcessingResources").configure {
    dependsOn(tasks.named("includeMode"))
}