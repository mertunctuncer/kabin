plugins {
    this.kotlin("jvm") version "2.2.21"
}

group = "dev.peopo"
version = "1.0-SNAPSHOT"

repositories {
    this.mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.ow2.asm:asm:9.9.1")

    testImplementation(kotlin("test"))
    testImplementation("javax.inject:javax.inject:1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}