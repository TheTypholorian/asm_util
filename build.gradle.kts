plugins {
    kotlin("jvm") version "2.4.21"
    id("net.typho.typho_publish") version "1.0.5"
}

group = "net.typho"
version = "1.4.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.11.1")
    implementation("org.ow2.asm:asm-tree:9.11.1")
    implementation("org.ow2.asm:asm-util:9.11.1")
    implementation("org.ow2.asm:asm-commons:9.11.1")
    implementation(kotlin("metadata-jvm"))
    implementation("org.jetbrains:annotations:26.0.2")
}

kotlin {
    jvmToolchain(8)
}