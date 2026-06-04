import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GitHub
import org.gradle.jvm.toolchain.JavaLanguageVersion

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("org.kohsuke:github-api:1.318")
    }
}

plugins {
    id("org.gradle.java-library")
    id("org.ajoberstar.grgit") version "5.2.1"
}

fun gitBranchName(): String {
    val env = System.getenv()
    if (env["GITHUB_REF"] != null) {
        val branch = env["GITHUB_REF"]!!
        return branch.substring(branch.lastIndexOf("/") + 1)
    }

    return try {
        val branch = grgit.branch.current().name
        branch.substring(branch.lastIndexOf("/") + 1)
    } catch (e: Exception) {
        "master"
    }
}

val env: MutableMap<String, String> = System.getenv()
val blockProtVersion: String by project
val versionSuffix: String by project
val targetJavaVersion: String by project

// Builds the full version from gradle.properties values.
// Set blockProtVersion and versionSuffix manually in gradle.properties.
// Examples: suffix blank -> "1.3.0", suffix "SNAPSHOT" -> "1.3.0-SNAPSHOT"
fun computeFullVersion(): String {
    val rawSuffix = versionSuffix.trim()
    return if (rawSuffix.isBlank()) blockProtVersion else "$blockProtVersion-$rawSuffix"
}

val fullVersion: String = computeFullVersion()

allprojects {
    apply(plugin = "org.gradle.java-library")

    group = "de.sean.blockprot"
    version = fullVersion

    repositories {
        mavenLocal()
        maven("https://jitpack.io") {
            name = "JitPack"
        }
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.compileJava {
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }

    ext["gitBranchName"] = gitBranchName()

    tasks.jar {
        archiveClassifier.set(
            if (ext["gitBranchName"] == "master" || ext["gitBranchName"] == "HEAD" || ext["gitBranchName"] == "main") null
            else (ext["gitBranchName"] as String)
        )
    }
}

tasks.register("github") {
    onlyIf {
        env["GITHUB_TOKEN"] != null
    }

    doLast {
        val github = GitHub.connectUsingOAuth(env["GITHUB_TOKEN"] as String)
        val repository = github.getRepository(env["GITHUB_REPOSITORY"])

        val releaseBuilder = GHReleaseBuilder(repository, version as String)
        releaseBuilder.name(version as String)
        releaseBuilder.body(env["CHANGELOG"] ?: "No changelog.")
        releaseBuilder.commitish(gitBranchName())

        val files = mutableListOf<File?>()
        subprojects.filter { it.name != "common" }.forEach {
            val dir = it.layout.buildDirectory.dir("libs").get().asFile
            files.add(dir.listFiles()?.firstOrNull { file ->
                file.name.startsWith("BlockProt-Reloaded-") && file.name.endsWith(".jar")
                    && !file.name.endsWith("-javadoc.jar")
                    && !file.name.endsWith("-sources.jar")
            })
        }

        val ghRelease = releaseBuilder.create()
        files.forEach {
            ghRelease.uploadAsset(it, "application/java-archive")
        }
        ghRelease.update().name("BlockProt Reloaded $version").update()
    }
}
