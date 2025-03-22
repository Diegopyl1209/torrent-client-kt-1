plugins {
    kotlin("jvm") version "2.1.10"
}

group = "me.diegopyl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


val ktor_version = "3.0.2"
dependencies {
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("org.slf4j:slf4j-nop:2.1.0-alpha1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}