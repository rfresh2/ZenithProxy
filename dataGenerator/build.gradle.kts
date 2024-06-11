plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
}

group = "com.zenith"
version = "1.0.0"

repositories {
    mavenLocal() {
        content { includeGroup("com.zenith") }
    }
    mavenCentral()
    maven("https://jitpack.io") {
        name = "jitpack.io"
        content { includeGroupByRegex("com.github.rfresh2.*") }
    }
    maven("https://maven.parchmentmc.org")
}

loom {
    accessWidenerPath = file("src/main/resources/zenithproxy.accesswidener")
    runs {
        getByName("server") {
            ideConfigGenerated(true)
            server()
            property("data.dir", project.layout.buildDirectory.file("data").get().asFile.absolutePath)
        }
    }
}
val lombokVersion = "1.18.32"

dependencies {
    minecraft("com.mojang:minecraft:1.21-rc1")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.20.6:2024.06.02@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:0.15.11")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.100.0+1.21")
    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.github.rfresh2:MCProtocolLib:d82fe51c52") {
        exclude("*")
    }
    implementation("com.zenith:ZenithProxy:1.20.6") {
        exclude("*")
    }
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
}
