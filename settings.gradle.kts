@file:Suppress("UnstableApiUsage")

pluginManagement {
    plugins {
        kotlin("jvm") version "2.3.10"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
}

// F-Droid doesn't support foojay-resolver plugin
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
// }

rootProject.name = "echomusic"
include(
    ":app",
    ":innertube",
    ":paxsenixlyrics",
    ":kugou",
    ":betterlyrics",
    ":lrclib",
    ":simpmusic",
    ":youlyplus",
    ":shazamkit",
    ":artistvideo",
    ":jiosaavn",
    ":canvas",
    ":echomusiccanvas",
    ":applecanvas",
    ":unison"
)
