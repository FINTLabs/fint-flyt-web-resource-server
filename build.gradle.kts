import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("java")
    id("com.github.ben-manes.versions") version "0.52.0"
    id("maven-publish")
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
}

group = "no.fintlabs"
version = findProperty("version") ?: "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.fintlabs.no/releases")
    }
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")

    implementation("no.fintlabs:fint-flyt-cache:1.2.3")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("no.fintlabs:fint-kafka:4.0.1")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

java {
    withSourcesJar()
}

ktlint {
    version.set("1.7.1")
    ignoreFailures.set(false)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
    }
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}

apply(from = "https://raw.githubusercontent.com/FINTLabs/fint-buildscripts/master/reposilite.ga.gradle")
