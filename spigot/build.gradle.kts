buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
}

plugins {
    id("maven-publish")
    // Updated to com.gradleup.shadow which supports Java 25+ (ASM updated)
    id("com.gradleup.shadow") version "9.4.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val nbtApiVersion = project.property("nbtApiVersion") as String
val townyVersion = project.property("townyVersion") as String
val papiVersion = project.property("papiVersion") as String
val worldGuardVersion = project.property("worldGuardVersion") as String

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "Sonatype Snapshots"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.org/repository/maven-public/") {
        name = "CodeMC"
        content {
            includeGroup("de.tr7zw")
        }
    }
    maven("https://repo.tcoded.com/releases") {
        name = "TCoded"
    }

}

// --- dialogs source set: compiles against Paper 1.21.7+ for the dialog API ---
sourceSets {
    val dialogs = create("dialogs") {
        java {
            srcDir("src/dialogs/java")
        }
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations.named("dialogsCompileOnly") {
    extendsFrom(configurations.compileOnly.get())
}

dependencies {
    implementation(project(":common"))

    // Compile against Paper 1.21.1, the oldest version the plugin actively supports.
    // 1.20.x users receive bug fixes only (legacy support, no new features).
    // Primary targets: current Paper/Purpur 26.x line and 1.21.1+.
    // APIs introduced after 1.21.1 are accessed via VersionCompat checks and
    // reflection at runtime, never directly imported.
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.apache.commons:commons-lang3:3.13.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.110.0")

    // bStats: 3.2.1
    api("org.bstats:bstats-bukkit:3.2.1")

    // Dependencies
    implementation("de.tr7zw:item-nbt-api:$nbtApiVersion")

    // FoliaLib: cross-platform scheduler (Spigot / Paper / Purpur / Pufferfish / Folia)
    implementation("com.tcoded:FoliaLib:0.5.1")

    implementation("org.enginehub:squirrelid:0.3.2")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("com.mysql:mysql-connector-j:9.7.0")

    // Integrations (soft-depend, provided at runtime by the server)
    compileOnly("com.github.TownyAdvanced:Towny:$townyVersion")
    compileOnly("me.clip:placeholderapi:$papiVersion")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:$worldGuardVersion")
    compileOnly("com.github.angeschossen:LandsAPI:6.28.11")
    compileOnly("com.cjburkey.claimchunk:claimchunk:0.0.25-FIX3")
    compileOnly("com.github.Zrips:Residence:6.0.0.1") { isTransitive = false }
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.2") { isTransitive = false }

    add("dialogsCompileOnly", "io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
}

val targetJavaVersion = project.property("targetJavaVersion") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

val pluginVersion: String = project.version.toString()

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:-deprecation")
    options.compilerArgs.add("-Xlint:-removal")
}

tasks.processResources {
    inputs.property("version", pluginVersion)

    filesMatching(listOf("plugin.yml")) {
        expand("version" to pluginVersion)
    }
}

tasks.javadoc {
    options {
            source = "21"
        encoding = "UTF-8"
        memberLevel = JavadocMemberLevel.PACKAGE
        (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    this.isFailOnError = false
}

tasks.shadowJar {
    relocate("de.tr7zw.changeme.nbtapi", "de.sean.blockprot.bukkit.shaded.nbtapi")
    relocate("org.bstats", "de.sean.blockprot.bukkit.metrics")
    relocate("org.enginehub.squirrelid", "de.sean.blockprot.bukkit.squirrelid")
    relocate("com.zaxxer.hikari", "de.sean.blockprot.bukkit.shaded.hikari")
    relocate("com.tcoded.folialib", "de.sean.blockprot.bukkit.shaded.folialib")
    // minimize()

    dependencies {
        this.include(project(":common"))
        this.include(dependency("org.jetbrains:annotations"))
        this.include(dependency("de.tr7zw:item-nbt-api"))
        this.include(dependency("org.bstats:bstats-base"))
        this.include(dependency("org.bstats:bstats-bukkit"))
        this.include(dependency("com.tcoded:FoliaLib"))
        this.include(dependency("org.enginehub:squirrelid"))
        this.include(dependency("com.zaxxer:HikariCP"))
        this.include(dependency("com.mysql:mysql-connector-j"))
        this.include(dependency("org.slf4j:slf4j-api"))
        this.include(dependency("com.github.ben-manes.caffeine:caffeine"))
    }

    // Output: BlockProtReloaded-1.3.0.jar  /  BlockProtReloaded-1.3.0-SNAPSHOT.jar
    val branch = ext["gitBranchName"] as String
    val isMaster = branch == "master" || branch == "HEAD" || branch == "main"
    val jarVersion = project.version as String
    val jarSuffix  = if (isMaster) "" else "-$branch"
    archiveFileName.set("BlockProtReloaded-${jarVersion}${jarSuffix}.jar")
    from(sourceSets["dialogs"].output)
}

tasks.build {
    dependsOn(tasks["javadocJar"])
    dependsOn(tasks.shadowJar)
    dependsOn(tasks.named("compileDialogsJava"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.runServer {
    downloadPlugins {
        url("https://download.luckperms.net/1561/bukkit/loader/LuckPerms-Bukkit-5.4.146.jar")
    }
    minecraftVersion("26.2")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
