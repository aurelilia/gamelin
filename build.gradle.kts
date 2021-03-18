/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 10:55 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.4.31"
    application
}

group = "me.ellie"
version = "1.0-SNAPSHOT"

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
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
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

                implementation("io.github.libktx:ktx-assets:$ktxVersion")
                implementation("io.github.libktx:ktx-actors:$ktxVersion")
                implementation("io.github.libktx:ktx-collections:$ktxVersion")
                implementation("io.github.libktx:ktx-style:$ktxVersion")
                implementation("io.github.libktx:ktx-scene2d:$ktxVersion")
                implementation("io.github.libktx:ktx-vis-style:$ktxVersion")
                implementation("io.github.libktx:ktx-vis:$ktxVersion")
                implementation("com.kotcrab.vis:vis-ui:1.4.11")
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("src/jvmTest")
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")

                implementation("io.kotest:kotest-runner-junit5:4.4.3")
                implementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
                implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
            }
        }

        val jsMain by getting {
            kotlin.srcDir("src/jsMain")
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
            }
        }
        val jsTest by getting {
            kotlin.srcDir("src/jsTest")
        }
    }
}


tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
    outputFileName = "output.js"
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
    // This is for the .dll and .so files to make the Discord RPC work on all desktops
    archiveFileName.set("gamelin-jvm.jar")

    manifest {
        attributes(mapOf("Main-Class" to mainClassName))
    }
}
