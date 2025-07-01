plugins {
    `java-library`
    id("org.graalvm.buildtools.native") version "0.10.6"
    id("com.gradleup.shadow") version "8.3.7"
    `maven-publish`
}

group = "com.zenith"
version = "1.21.4"

val javaReleaseVersion = 21
val javaVersion = JavaLanguageVersion.of(24)
val javaLauncherProvider = javaToolchains.launcherFor { languageVersion = javaVersion }
java {
    toolchain { languageVersion = javaVersion }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    maven("https://maven.2b2t.vc/releases") {
        content { includeGroupByRegex("com.github.rfresh2.*") }
    }
    maven("https://maven.2b2t.vc/remote")
    mavenLocal()
}

val mcplVersion = "1.21.4.25"
dependencies {
    api("com.github.rfresh2:JDA:5.6.14") {
        exclude(group = "club.minnced")
        exclude(group = "net.java.dev.jna")
        exclude(group = "com.google.crypto.tink")
    }
    api("com.github.rfresh2:MCProtocolLib:$mcplVersion") {
        exclude(group = "io.netty")
    }
    val nettyVersion = "4.2.2.Final"
    api("io.netty:netty-buffer:$nettyVersion")
    api("io.netty:netty-codec-haproxy:$nettyVersion")
    api("io.netty:netty-codec-dns:$nettyVersion")
    api("io.netty:netty-codec-http2:$nettyVersion")
    api("io.netty:netty-codec-http:$nettyVersion")
    api("io.netty:netty-codec-socks:$nettyVersion")
    api("io.netty:netty-handler-proxy:$nettyVersion")
    api("io.netty:netty-handler:$nettyVersion")
    api("io.netty:netty-resolver-dns:$nettyVersion")
    api("io.netty:netty-transport-classes-epoll:$nettyVersion")
    api("io.netty:netty-transport-native-epoll:$nettyVersion:linux-x86_64")
    api("io.netty:netty-transport-native-unix-common:$nettyVersion:linux-x86_64")
    api("io.netty:netty-resolver-dns-native-macos:$nettyVersion:osx-aarch_64")
    api("org.cloudburstmc.math:api:2.0")
    api("org.cloudburstmc.math:immutable:2.0")
    api("org.redisson:redisson:3.50.0") {
        exclude(group = "io.netty")
    }
    api("com.github.rfresh2:SimpleEventBus:1.6")
    val fastutilVersion = "8.5.15"
    api("com.github.rfresh2.fastutil.maps:object-object-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:int-object-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:object-int-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:long-object-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:int-int-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:int-double-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:reference-object-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:long-double-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.queues:int-queues:$fastutilVersion")
    api("com.viaversion:vialoader:4.0.4")
    api("com.viaversion:viaversion:5.4.1")
    api("com.viaversion:viabackwards:5.4.1")
    api("org.jline:jline:3.30.4")
    api("org.jline:jline-terminal-jni:3.30.4")
    api("ar.com.hjg:pngj:2.1.0")
    api("com.zaxxer:HikariCP:6.3.0")
    api("org.postgresql:postgresql:42.7.7")
    api("org.jdbi:jdbi3-postgres:3.49.4")
    api("com.google.guava:guava:33.4.6-jre")
    api("ch.qos.logback:logback-classic:1.5.18")
    api("org.slf4j:slf4j-api:2.0.17")
    api("org.slf4j:jul-to-slf4j:2.0.17")
    api("com.mojang:brigadier:1.3.10")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.1")
    api("org.jspecify:jspecify:1.0.0")
    api("net.kyori:adventure-text-logger-slf4j:4.23.0")
    api("dev.omega24:upnp4j:1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    val lombokVersion = "1.18.38"
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
        options.isDeprecation = true
        options.release = javaReleaseVersion
    }
    test {
        useJUnitPlatform()
        workingDir = layout.projectDirectory.dir("run").asFile
    }
    val commitHashTask = register<CommitHashTask>("writeCommitHash") {
        outputFile = project.layout.buildDirectory.file("resources/main/zenith_commit.txt")
    }
    val releaseTagTask = register<WriteMetadataTxtTask>("releaseTag") {
        metadataValue = providers.environmentVariable("RELEASE_TAG").orElse("")
        outputFile = project.layout.buildDirectory.file("resources/main/zenith_release.txt")
    }
    val mcVersionTask = register<WriteMetadataTxtTask>("mcVersion") {
        metadataValue = version.toString()
        outputFile = project.layout.buildDirectory.file("resources/main/zenith_mc_version.txt")
    }
    val runGroup = "run"
    register("run", JavaExec::class.java) {
        group = runGroup
        description = "Execute proxy"
        javaLauncher = javaLauncherProvider
        workingDir = layout.projectDirectory.dir("run").asFile
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("com.zenith.Proxy")
        val args = mutableListOf("-Xmx300m", "-XX:+UseG1GC")
        if (javaLauncher.get().metadata.languageVersion.asInt() == 24)
            args.addAll(listOf(
                "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders",
                "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"
            ))
        jvmArgs = args
        standardInput = System.`in`
        environment("ZENITH_DEV", "true")
        outputs.upToDateWhen { false }
    }
    val javaPathTask = register<JavaPathTask>("javaPath") {
        javaLauncher = javaLauncherProvider
    }
    processResources {
        dependsOn(releaseTagTask, mcVersionTask, commitHashTask)
    }
    val devOutputDir = layout.buildDirectory.get().dir("dev").asFile
    jar {
        enabled = true
        archiveClassifier = ""
        destinationDirectory = devOutputDir
    }
    getByName("sourcesJar", Jar::class) {
        archiveClassifier = "sources"
        destinationDirectory = devOutputDir
    }
    javadoc {
        isFailOnError = false
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            links(
                "https://docs.oracle.com/en/java/javase/${javaReleaseVersion}/docs/api",
                "https://maven.2b2t.vc/javadoc/releases/com/github/rfresh2/MCProtocolLib/$mcplVersion/raw"
            )
        }
    }
    getByName("javadocJar", Jar::class) {
        destinationDirectory = devOutputDir
    }
    shadowJar {
        from(collectReachabilityMetadata)
        archiveBaseName = project.name
        archiveClassifier = ""
        archiveVersion = ""

        exclude(listOf(
            "module-info.class", "META-INF/licenses/**", "META-INF/maven/**", "META-INF/proguard/**",
            "META-INF/gradle/**", "META-INF/native-image/io.netty/**/native-image.properties" ,
            "about.html", "bungee.yml", "plugin.yml", "velocity-plugin.json", "fabric.mod.json", "OSGI-INF/**"
        ))

        manifest {
            attributes(mapOf(
                "Implementation-Title" to "ZenithProxy",
                "Implementation-Version" to project.version,
                "Main-Class" to "com.zenith.Proxy",
                "Multi-Release" to "true",
                "Enable-Native-Access" to "ALL-UNNAMED"
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
            javaLauncher = javaLauncherProvider
            imageName = "ZenithProxy"
            mainClass = "com.zenith.Proxy"
            quickBuild = false
            verbose = true
            sharedLibrary = false
            buildArgs.addAll(
                "-Duser.country=US",
                "-Duser.language=en",
                "--enable-url-protocols=https,http",
                "-H:+ReportExceptionStackTraces",
                "-H:DeadlockWatchdogInterval=30",
                "-H:IncludeLocales=en",
                "-H:+CompactingOldGen",
                "-H:+TrackPrimitiveValues",
                "-H:+UsePredicates",
//                "--emit build-report",
                "-R:MaxHeapSize=200m",
                "-march=x86-64-v3",
                "--gc=serial",
                "-J-XX:MaxRAMPercentage=90",
                "--install-exit-handlers",
//                "--enable-monitoring=nmt,jfr",
                "-J--enable-native-access=ALL-UNNAMED",
                "-J--sun-misc-unsafe-memory-access=allow",
//                "-H:+PrintClassInitialization",
                "--initialize-at-build-time=com.zenith.feature.deathmessages",
                "--initialize-at-build-time=org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType",
                "--initialize-at-build-time=org.cloudburstmc.math.immutable.vector.ImmutableVector3i",
                "--initialize-at-build-time=com.google.common.collect.RegularImmutableList",
                "--initialize-at-build-time=org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType",
                "--initialize-at-build-time=org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType",
                "--initialize-at-build-time=it.unimi.dsi.fastutil",
                "--initialize-at-build-time=com.google.common.collect",
                "--initialize-at-build-time=com.zenith.mc",
                "--initialize-at-run-time=com.zenith.mc.chat_type",
                "--initialize-at-run-time=sun.net.dns.ResolverConfigurationImpl", // fix for windows builds, exception when doing srv lookups with netty
            )
            val pgoPath = providers.environmentVariable("GRAALVM_PGO_PATH").orNull
            if (pgoPath != null) {
                println("Using PGO profile: $pgoPath")
                buildArgs.add("--pgo=$pgoPath")
                buildArgs.add("-H:+PGOPrintProfileQuality")
            } else {
                val pgoInstrument = providers.environmentVariable("GRAALVM_PGO_INSTRUMENT").orNull
                if (pgoInstrument != null) {
                    println("Instrumenting PGO")
                    buildArgs.add("--pgo-instrument")
                    buildArgs.add("-R:ProfilesDumpFile=profile.iprof")
                }
            }
            configurationFileDirectories.from(file("src/main/resources/META-INF/native-image"))
        }
    }
    metadataRepository { enabled = true }
}

publishing {
    repositories {
        maven {
            name = "releases"
            url = uri("https://maven.2b2t.vc/releases")
            credentials {
                username = providers.environmentVariable("MAVEN_USERNAME").orNull
                password = providers.environmentVariable("MAVEN_PASSWORD").orNull
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "snapshots"
            url = uri("https://maven.2b2t.vc/snapshots")
            credentials {
                username = providers.environmentVariable("MAVEN_USERNAME").orNull
                password = providers.environmentVariable("MAVEN_PASSWORD").orNull
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("snapshot") {
            groupId = "com.zenith"
            artifactId = "ZenithProxy"
            version = "${project.version}-SNAPSHOT"
            val javaComponent = components["java"] as AdhocComponentWithVariants
            javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
            from(javaComponent)
        }
        create<MavenPublication>("release") {
            groupId = "com.zenith"
            artifactId = "ZenithProxy"
            version =  providers.environmentVariable("ZENITH_RELEASE_TAG").orElse("0.0.0+${project.version}").get()
            val javaComponent = components["java"] as AdhocComponentWithVariants
            javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
            from(javaComponent)
        }
    }
}
