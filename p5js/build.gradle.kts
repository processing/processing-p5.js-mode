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
}

tasks.register<Copy>("createMode") {
    dependsOn("jar")
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