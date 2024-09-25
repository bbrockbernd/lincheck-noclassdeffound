plugins {
    kotlin("jvm") version "2.0.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlinx:lincheck:2.34")
    testImplementation(kotlin("test-junit"))
}

sourceSets.main {
    java.srcDir("src/main")
}

sourceSets.test {
    java.srcDir("src/test")
}
kotlin {
    jvmToolchain(17)
}