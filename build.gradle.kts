import org.apache.tools.ant.taskdefs.condition.Os
import java.io.ByteArrayOutputStream
import java.nio.file.Files

plugins {
    java
    id("org.graalvm.buildtools.native") version "0.10.2"
    id("com.gradleup.shadow") version "8.3.0"
    `maven-publish`
}

group = "com.zenith"
version = "1.21.0"

val javaVersion22 = JavaLanguageVersion.of(22)
val javaVersion21 = JavaLanguageVersion.of(21)
val javaLauncherProvider21 = javaToolchains.launcherFor { languageVersion = javaVersion21 }
val javaLauncherProvider22 = javaToolchains.launcherFor { languageVersion = javaVersion22 }
java { toolchain { languageVersion = javaVersion21 } }

repositories {
    maven("https://maven.2b2t.vc/releases") {
        content { includeGroupByRegex("com.github.rfresh2.*") }
    }
    maven("https://libraries.minecraft.net") {
        name = "minecraft"
        content { includeGroup("com.mojang") }
    }
    maven("https://repo.opencollab.dev/maven-releases/") {
        name = "opencollab-release"
        content { includeGroupByRegex("org.cloudburstmc.*") }
    }
    maven("https://papermc.io/repo/repository/maven-public/") {
        name = "paper"
        content { includeGroup("com.velocitypowered") }
    }
    maven("https://repo.minebench.de/") {
        name = "minebench"
        content { includeGroup("de.themoep") }
    }
    maven("https://repo.viaversion.com") {
        name = "ViaVersion"
        content {
            includeGroup("com.viaversion")
            includeGroup("net.raphimc")
        }
    }
    maven("https://maven.lenni0451.net/releases") {
        name = "Lenni0451"
        content {
            includeGroup("net.raphimc")
            includeGroup("net.lenni0451")
        }
    }
    mavenCentral()
    mavenLocal()
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

dependencies {
    shade("com.zaxxer:HikariCP:5.1.0")
    shade("org.postgresql:postgresql:42.7.4")
    val jdbiVersion = "3.45.4"
    shade("org.jdbi:jdbi3-core:$jdbiVersion")
    shade("org.jdbi:jdbi3-postgres:$jdbiVersion")
    shade("com.google.guava:guava:33.3.0-jre")
    shade("ch.qos.logback:logback-classic:1.5.8")
    shade("org.slf4j:slf4j-api:2.0.16")
    shade("org.slf4j:jul-to-slf4j:2.0.16")
    shade("com.mojang:brigadier:1.2.9")
    shade("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    shade("com.github.rfresh2:SimpleEventBus:1.2")
    shade("com.github.rfresh2.discord4j:discord4j-core:3.4.3.8") {
        exclude(group = "io.netty")
    }
    shade("com.github.rfresh2:MCProtocolLib:1.21.0.9") {
        exclude(group = "io.netty.incubator")
        exclude(group = "io.netty")
    }
    shade("net.raphimc:MinecraftAuth:4.1.0")
    val nettyVersion = "4.1.113.Final"
    shade("io.netty:netty-codec-haproxy:$nettyVersion")
    shade("io.netty:netty-codec-dns:$nettyVersion")
    shade("io.netty:netty-codec-http2:$nettyVersion")
    shade("io.netty:netty-codec-http:$nettyVersion")
    shade("io.netty:netty-codec-socks:$nettyVersion")
    shade("io.netty:netty-handler-proxy:$nettyVersion")
    shade("io.netty:netty-handler:$nettyVersion")
    shade("io.netty:netty-resolver-dns:$nettyVersion")
    shade("io.netty:netty-transport-classes-epoll:$nettyVersion")
    shade("io.netty:netty-transport-native-epoll:$nettyVersion:linux-x86_64")
    shade("io.netty:netty-transport-native-unix-common:$nettyVersion:linux-x86_64")
    shade("io.netty:netty-resolver-dns-native-macos:$nettyVersion:osx-aarch_64")
    shade("de.themoep:minedown-adventure:1.7.3-SNAPSHOT")
    shade("org.cloudburstmc.math:api:2.0")
    shade("org.cloudburstmc.math:immutable:2.0")
    shade("org.redisson:redisson:3.35.0") {
        exclude(group = "io.netty")
    }
    val fastutilVersion = "8.5.14"
    shade("com.github.rfresh2.fastutil.maps:object-object-maps:$fastutilVersion")
    shade("com.github.rfresh2.fastutil.maps:int-object-maps:$fastutilVersion")
    shade("com.github.rfresh2.fastutil.maps:object-int-maps:$fastutilVersion")
    shade("com.github.rfresh2.fastutil.maps:long-object-maps:$fastutilVersion")
    shade("com.github.rfresh2.fastutil.maps:int-int-maps:$fastutilVersion")
    shade("com.github.rfresh2.fastutil.maps:reference-object-maps:$fastutilVersion")
    shade("com.github.rfresh2.fastutil.queues:int-queues:$fastutilVersion")
    shade("net.raphimc:ViaLoader:3.0.2")
    shade("com.viaversion:viaversion:5.0.3")
    shade("com.viaversion:viabackwards:5.0.3")
    shade("org.jline:jline:3.26.3")
    shade("org.jline:jline-terminal-jni:3.26.3")
    shade("ar.com.hjg:pngj:2.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    val lombokVersion = "1.18.34"
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
        options.isDeprecation = true
    }
    test {
        useJUnitPlatform()
    }
    val commitHashTask = register("writeCommitHash") {
        group = "build"
        description = "Write commit hash / version to file"
        doLast {
            val byteOut = ByteArrayOutputStream()
            exec {
                commandLine = "git rev-parse --short=8 HEAD".split(" ")
                standardOutput = byteOut
            }
            String(byteOut.toByteArray()).trim().let {
                if (it.length > 5) {
                    file(layout.buildDirectory.asFile.get().absolutePath + "/resources/main/zenith_commit.txt").apply {
                        parentFile.mkdirs()
                        println("Writing commit hash: $it")
                        writeText(it)
                    }
                } else {
                    println("Unable to determine commit hash")
                }
            }
        }
        outputs.upToDateWhen { false }
    }
    val releaseTagTask = register("releaseTag") {
        group = "build"
        description = "Write release tag to file"
        doLast {
            System.getenv("RELEASE_TAG")?.let {
                file(layout.buildDirectory.asFile.get().absolutePath + "/resources/main/zenith_release.txt").apply {
                    parentFile.mkdirs()
                    println("Writing release tag: $it")
                    writeText(it)
                }
            } ?: run {
                println("Dev build detected, skipping release tag generation")
            }
        }
        outputs.upToDateWhen { false }
    }
    val runGroup = "run"
    register("run", JavaExec::class.java) {
        group = runGroup
        description = "Execute proxy"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("com.zenith.Proxy")
        jvmArgs = listOf("-Xmx300m", "-XX:+UseG1GC")
        outputs.upToDateWhen { false }
    }
    val javaPathTask = register("javaPath", Task::class.java) {
        group = runGroup
        doLast {
            val execPath = javaLauncherProvider21.get().executablePath
            // create a file symlinked to the java executable for use in scripts
            layout.buildDirectory.asFile.get().mkdirs()
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                val f: File = file (layout.buildDirectory.asFile.get().toString() + "/java_toolchain.bat")
                if (f.exists()) {
                    f.delete()
                }
                f.writeText("@" + execPath.asFile.toString() + " %*")
            } else if (Os.isFamily(Os.FAMILY_UNIX)) {
                val f: File = file (layout.buildDirectory.asFile.get().toString() + "/java_toolchain")
                if (f.exists()) {
                    f.delete()
                }
                Files.createSymbolicLink(f.toPath(), execPath.asFile.toPath())
            }
        }
    }
    processResources{ finalizedBy(commitHashTask, releaseTagTask) }
    jar { enabled = false }
    shadowJar {
        from(collectReachabilityMetadata)
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
        archiveVersion.set("")

        configurations = listOf(shade)

        exclude(listOf(
            "module-info.class", "META-INF/licenses/**", "META-INF/maven/**", "META-INF/proguard/**",
            "META-INF/gradle/**", "META-INF/native-image/io.netty/**/native-image.properties" ,
            "about.html", "bungee.yml", "plugin.yml", "velocity-plugin.json", "fabric.mod.json", "OSGI-INF/**"
        ))

        minimize {
            exclude(dependency("org.slf4j:slf4j-api:.*"))
            exclude(dependency("ch.qos.logback:.*:.*"))
            exclude(dependency("org.jline:.*:.*"))
            exclude(dependency("com.github.rfresh2.discord4j:discord4j-core:.*"))
            exclude(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
            exclude(dependency("org.postgresql:postgresql:.*"))
            exclude(dependency("io.netty:netty-codec-http:.*"))
            exclude(dependency("io.netty:netty-codec-http2:.*"))
            exclude(dependency("io.netty:netty-resolver-dns:.*"))
            exclude(dependency("org.cloudburstmc.math:api:.*"))
            exclude(dependency("org.cloudburstmc.math:immutable:.*"))
            exclude(dependency("io.jsonwebtoken:jjwt-api:.*"))
            exclude(dependency("io.jsonwebtoken:jjwt-gson:.*"))
            exclude(dependency("io.jsonwebtoken:jjwt-impl:.*"))
            exclude(dependency("com.viaversion:viaversion:.*"))
            exclude(dependency("com.viaversion:viabackwards:.*"))
            exclude(dependency("net.raphimc:ViaLoader:.*"))
            exclude(dependency("net.kyori:adventure-api:.*"))
            exclude(dependency("net.kyori:adventure-text-serializer-gson:.*"))
        }
        manifest {
            attributes(mapOf(
                "Implementation-Title" to "ZenithProxy",
                "Implementation-Version" to project.version,
                "Main-Class" to "com.zenith.Proxy",
                "Multi-Release" to "true"
            ))
        }
    }
    val jarBuildTask = register("jarBuild") {
        group = "build"
        dependsOn(shadowJar, build, javaPathTask)
    }
    nativeCompile {
        classpathJar = shadowJar.flatMap { it.archiveFile }
        dependsOn(jarBuildTask)
    }
    generateResourcesConfigFile { dependsOn(shadowJar) }
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher = javaLauncherProvider22
            imageName = "ZenithProxy"
            mainClass = "com.zenith.Proxy"
            quickBuild = false
            verbose = true
            buildArgs.addAll(
                "-Duser.country=US",
                "-Duser.language=en",
                "--enable-url-protocols=https,http",
                "-H:+ReportExceptionStackTraces",
                "-H:DeadlockWatchdogInterval=30",
                "-H:IncludeLocales=en",
                "-R:MaxHeapSize=200m",
                "-march=x86-64-v3",
                "--gc=serial",
                "-J-XX:MaxRAMPercentage=90",
                "--initialize-at-build-time=org.redisson.misc.BiHashMap",
                "--initialize-at-build-time=org.redisson.liveobject.core.RedissonObjectBuilder\$CodecMethodRef"
            )
            val pgoPath = System.getenv("GRAALVM_PGO_PATH")
            if (pgoPath != null) {
                println("Using PGO profile: $pgoPath")
                buildArgs.add("--pgo=$pgoPath")
            } else {
                val pgoInstrument = System.getenv("GRAALVM_PGO_INSTRUMENT")
                if (pgoInstrument != null) {
                    println("Instrumenting PGO")
                    buildArgs.add("--pgo-instrument")
                }
            }
            configurationFileDirectories.from(file("src/main/resources/META-INF/native-image"))
        }
    }
    metadataRepository { enabled = true }
}

/** Publishing stuff for dataGenerator subproject **/
// avoiding publishing the shaded jar which would otherwise cause dependency duplication issues
// jar task needs to be enabled above (temporarily) for this publishing to work
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.zenith"
            artifactId = "ZenithProxy"
            version = project.version.toString()
            val publishArtifact = components["java"] as AdhocComponentWithVariants
            publishArtifact.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
            from(publishArtifact)
        }
    }
}
