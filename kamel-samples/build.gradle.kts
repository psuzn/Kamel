import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    multiplatform
    compose
    `android-application`
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    packagingOptions {
        resources {
            excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
        }
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res", "src/commonMain/resources")
        }
    }

    configurations {
        create("androidTestApi")
        create("androidTestDebugApi")
        create("androidTestReleaseApi")
        create("testApi")
        create("testDebugApi")
        create("testReleaseApi")
//        named("implementation") {
//            exclude(group = "androidx.compose.animation")
//            exclude(group = "androidx.compose.foundation")
//            exclude(group = "androidx.compose.material")
//            exclude(group = "androidx.compose.runtime")
//            exclude(group = "androidx.compose.ui")
//        }
    }
}

kotlin {

    explicitApi = ExplicitApiMode.Warning

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("11"))
    }

    android()
    jvm("desktop")

    for (target in Targets.macosTargets) {
        targets.add(
            (presets.getByName(target)
                .createTarget(target) as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget).apply {
                binaries.executable {
                    freeCompilerArgs += listOf(
                        "-linker-option", "-framework", "-linker-option", "Metal"
                    )
                }
            }
        )
    }


    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(project(":kamel-image"))
                implementation(project(":kamel-tests"))
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.animation)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(Dependencies.Android.ActivityCompose)
                implementation(Dependencies.Android.Material)
                implementation(Dependencies.Ktor.Android)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(Dependencies.Ktor.CIO)
            }
        }

        val macosMain by creating {
            dependsOn(commonMain)
        }

        Targets.macosTargets.forEach { target ->
            getByName("${target}Main") {
                dependsOn(macosMain)
            }
        }

        all {
            languageSettings.apply {
                optIn("kotlin.Experimental")
            }
        }

    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

compose {
    desktop {
        application {
            mainClass = "io.kamel.samples.DesktopSampleKt"
        }
    }
}


compose.desktop.nativeApplication {
    targets(kotlin.targets.getByName("macosArm64"))
    distributions {
        targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
        packageName = "Native-Sample"
        packageVersion = "1.0.0"
    }
}