/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 4/3/21, 1:20 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

buildscript {
    repositories {
        mavenLocal()
        maven { url = uri("https://dl.bintray.com/korlibs/korlibs") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        mavenCentral()
        google()
    }
}

plugins {
    kotlin("multiplatform") version "1.4.32"
    id("org.jetbrains.dokka") version "1.4.30"
    application
}

group = "xyz.angm.gamelin"
version = "1.3.1"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src/commonMain")
        }
        val commonTest by getting {
            kotlin.srcDir("src/commonTest")
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val gdxVersion = "1.9.14"
        val ktxVersion = "1.9.14-b1"
        val jvmMain by getting {
            kotlin.srcDir("src/jvmMain")
            dependencies {
                implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
                implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
                implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
                implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
                implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")

 				implementation("com.badlogicgames.gdx:gdx-jnigen:1.9.10")
                implementation("uk.co.electronstudio.sdl2gdx:sdl2gdx:1.0.4")

                implementation("io.github.libktx:ktx-assets:$ktxVersion")
                implementation("io.github.libktx:ktx-actors:$ktxVersion")
                implementation("io.github.libktx:ktx-collections:$ktxVersion")
                implementation("io.github.libktx:ktx-style:$ktxVersion")
                implementation("io.github.libktx:ktx-scene2d:$ktxVersion")
                implementation("io.github.libktx:ktx-vis-style:$ktxVersion")
                implementation("io.github.libktx:ktx-vis:$ktxVersion")
                implementation("com.kotcrab.vis:vis-ui:1.4.11")

                implementation("com.esotericsoftware:kryo:5.0.4")
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("src/jvmTest")
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:4.4.3")
                implementation("io.mockk:mockk:1.10.6")
                implementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
                implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
            }
        }

        val jsMain by getting {
            kotlin.srcDir("src/jsMain")
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
                implementation("com.soywiz.korlibs.korau:korau:2.0.10")
            }
        }
        val jsTest by getting {
            kotlin.srcDir("src/jsTest")
        }
    }
}

// These tasks were previosuly in Groovy, thank you to Unciv for their Kotlin translations!
// https://github.com/yairm210/Unciv/blob/bcab751f7cc643f11b845ba9f038e190a58780c9/desktop/build.gradle.kts
val mainClassName = "xyz.angm.gamelin.LauncherKt"
val assetsDir = file("assets")
tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName("classes"))

    main = mainClassName
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = assetsDir
    isIgnoreExitValue = true
}

tasks.register<JavaExec>("debug") {
    dependsOn(tasks.getByName("classes"))
    main = mainClassName
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = assetsDir
    isIgnoreExitValue = true
    debug = true
}

tasks.register<Jar>("dist") {
    dependsOn(tasks.getByName("classes"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(files(sourceSets.main.get().output.resourcesDir))
    from(files(sourceSets.main.get().output.classesDirs))
    // see Laurent1967's comment on https://github.com/libgdx/libgdx/issues/5491
    from({ configurations.compileClasspath.get().resolve().map { if (it.isDirectory) it else zipTree(it) } })
    from(files(assetsDir))
    archiveFileName.set("gamelin-$version.jar")

    manifest {
        attributes(mapOf("Main-Class" to mainClassName))
    }
}

tasks.register<Copy>("distJs") {
    dependsOn(tasks.getByName("jsBrowserWebpack"))
    from("build/distributions/gamelin.js")
    from("build/distributions/gamelin.js.map")
    into("web/")
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
