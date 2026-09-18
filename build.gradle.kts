plugins {
    kotlin("jvm") version "2.4.0"
    id("net.typho.typho_publish") version "1.0.1"
}

group = "net.typho"
version = "1.2.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-tree:9.10.1")
    implementation("org.ow2.asm:asm-util:9.10.1")
    implementation("org.ow2.asm:asm-commons:9.10.1")
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.2.0")
    implementation("org.jetbrains:annotations:26.0.2")
}

kotlin {
    jvmToolchain(8)
}