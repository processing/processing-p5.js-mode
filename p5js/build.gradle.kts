plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "org.processing"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jogamp.org/deployment/maven") }
}

dependencies {
//    compileOnly(files("/Applications/Processing.app/Contents/Java/pde.jar/**/"))
    implementation(project(":core"))
    implementation(project(":app"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    testImplementation(kotlin("test"))
}

project(":app").tasks.named("run").configure {
    dependsOn(tasks.named("installLibrary"))
}
tasks.create<Copy>("copyJars") {
    group = "processing"
    dependsOn(tasks.jar)
    from(layout.buildDirectory.dir("libs")){
        include("**/*.jar")
    }
    from(configurations.compileClasspath)
    into(layout.buildDirectory.dir("library/mode"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
tasks.create<Copy>("createLibrary") {
    group = "processing"
    from("library")
    into(layout.buildDirectory.dir("library"))
}
tasks.create<Copy>("installLibrary") {
    group = "processing"
    dependsOn(tasks.named("createLibrary"))
    dependsOn(tasks.named("copyJars"))
    from(layout.buildDirectory.dir("library"))
    into("${System.getProperty("user.home")}/sketchbook/modes/p5js")
}
//tasks.register<JavaExec>("runProcessing") {
//    dependsOn(tasks.named("installLibrary"))
//    group = "processing"
//    classpath = files(fileTree("/Applications/Processing.app/Contents/Java/"){
//        include("*.jar")
//    })
//    mainClass.set("processing.app.Base")  // Your main class
//
//    // Optional: Add arguments if needed
//    args = listOf("")
//
//    // Optional: Add JVM arguments if needed
//    jvmArgs = listOf("-Xmx2g")
//}
tasks.jar {
    archiveVersion.set("")
    archiveBaseName.set("p5js")
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}