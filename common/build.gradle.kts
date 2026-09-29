plugins {
    id("org.gradle.java-library")
    id("maven-publish")
}

dependencies {
    api("org.jetbrains:annotations:26.1.0")
}

tasks.compileJava {
    java.sourceCompatibility = JavaVersion.VERSION_1_8
    java.targetCompatibility = JavaVersion.VERSION_1_8
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
