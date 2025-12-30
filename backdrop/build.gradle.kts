plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("com.vanniktech.maven.publish")
}

kotlin {
    androidTarget()


    iosArm64()
    iosSimulatorArm64()
    iosX64()


    jvm("desktop")


    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xexpect-actual-classes"
        )
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
            }
        }

        // Shared source set for Skia-based platforms (iOS + Desktop)
        val skiaMain by creating {
            dependsOn(commonMain)
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.compose.foundation)
                implementation(libs.androidx.compose.ui)
                implementation(libs.androidx.compose.ui.graphics)
            }
        }


        val iosX64Main by getting {
            dependsOn(skiaMain)
        }
        val iosArm64Main by getting {
            dependsOn(skiaMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(skiaMain)
        }

        val desktopMain by getting {
            dependsOn(skiaMain)
        }

        val wasmJsMain by getting {
            dependsOn(skiaMain)
        }
    }
}

android {
    namespace = "com.kashif_e.backdrop"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }
}

mavenPublishing {
    publishToMavenCentral()
    // signAllPublications() // Disabled for local development - enable for release

    coordinates("io.github.kyant0", "backdrop", "0.0.8-alpha11")

    pom {
        name.set("Backdrop")
        description.set("Compose Multiplatform blur and Liquid Glass effects")
        inceptionYear.set("2025")
        url.set("https://github.com/Kyant0/AndroidLiquidGlass")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("Kyant0")
                name.set("Kyant0")
                url.set("https://github.com/Kyant0")
            }
        }
        scm {
            url.set("https://github.com/Kyant0/AndroidLiquidGlass")
            connection.set("scm:git:git://github.com/Kyant0/AndroidLiquidGlass.git")
            developerConnection.set("scm:git:ssh://git@github.com/Kyant0/AndroidLiquidGlass.git")
        }
    }
}
