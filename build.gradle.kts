plugins {
    `java-library`
    id("org.graalvm.buildtools.native") version "1.0.0"
    id("com.gradleup.shadow") version "9.4.1"
    id("io.freefair.lombok") version "9.2.0"
    `maven-publish`
}

group = "com.zenith"
version = "1.21.4"

val javaReleaseVersion = 21
val javaVersion = JavaLanguageVersion.of(25)
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

val mcplVersion = "1.21.4.49"
dependencies {
    api("com.github.rfresh2:JDA:6.4.31") {
        exclude(group = "club.minnced")
        exclude(group = "net.java.dev.jna")
        exclude(group = "com.google.crypto.tink")
    }
    api("com.github.rfresh2:MCProtocolLib:$mcplVersion") {
        exclude(group = "io.netty")
    }
    api(platform("io.netty:netty-bom:4.2.12.Final"))
    api("io.netty:netty-buffer")
    api("io.netty:netty-codec-haproxy")
    api("io.netty:netty-codec-dns")
    api("io.netty:netty-codec-http2")
    api("io.netty:netty-codec-http")
    api("io.netty:netty-codec-socks")
    api("io.netty:netty-handler-proxy")
    api("io.netty:netty-handler")
    api("io.netty:netty-resolver-dns")
    api("io.netty:netty-transport-classes-epoll")
    api("io.netty:netty-transport-native-epoll") { artifact { classifier = "linux-x86_64" } }
    api("io.netty:netty-transport-native-epoll") { artifact { classifier = "linux-aarch_64" } }
    api("io.netty:netty-transport-native-unix-common") { artifact { classifier = "linux-x86_64"} }
    api("io.netty:netty-transport-native-unix-common") { artifact { classifier = "linux-aarch_64"} }
    api("io.netty:netty-resolver-dns-native-macos") { artifact { classifier = "osx-aarch_64" } }
    api("org.cloudburstmc.math:api:2.0")
    api("org.cloudburstmc.math:immutable:2.0")
    api("org.redisson:redisson:4.3.1") {
        exclude(group = "io.netty")
    }
    api("com.github.rfresh2:SimpleEventBus:1.6")
    val fastutilVersion = "8.5.16"
    api("com.github.rfresh2.fastutil.maps:object-object-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:int-object-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:object-int-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:long-object-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:int-int-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:int-double-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:reference-object-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.maps:long-double-maps:$fastutilVersion")
    api("com.github.rfresh2.fastutil.queues:int-queues:$fastutilVersion")
    api("com.viaversion:viaversion-common:5.9.0-20260417.203614-32")
    api("com.viaversion:viabackwards-common:5.9.0-20260417.203715-15")
    api("com.viaversion:viarewind-common:4.1.0-20260409.155713-7")
    api("org.jline:jline:4.0.12")
    api("ar.com.hjg:pngj:2.1.0")
    api("com.zaxxer:HikariCP:7.0.2")
    api("org.postgresql:postgresql:42.7.10")
    api("org.jdbi:jdbi3-postgres:3.52.1")
    api("com.google.guava:guava:33.5.0-jre")
    api("ch.qos.logback:logback-classic:1.5.32")
    api("org.slf4j:slf4j-api:2.0.17")
    api("org.slf4j:jul-to-slf4j:2.0.17")
    api("com.mojang:brigadier:1.3.10")
    api("net.kyori:adventure-text-logger-slf4j")
    api("dev.omega24:upnp4j:1.0")
    api(platform("tools.jackson:jackson-bom:3.1.2"))
    api("tools.jackson.core:jackson-databind")
    api("tools.jackson.dataformat:jackson-dataformat-smile")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers:2.0.4")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("org.graalvm.sdk:nativeimage:25.0.2")
}

lombok {
    version = "1.18.44"
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
        forkEvery = 1 // needed bc zenith uses global static state
        maxParallelForks = Runtime.getRuntime().availableProcessors()
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
        val args = listOf(
			"-Xmx300m", "-XX:+UseG1GC", "-XX:+UseCompactObjectHeaders",
			"--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"
		)
        jvmArgs = args
        standardInput = System.`in`
        environment("ZENITH_DEV", "true")
        outputs.upToDateWhen { false }
    }
    val javaPathTask = register<JavaPathTask>("javaPath") {
        javaLauncher = javaLauncherProvider
    }
    val generateCommandDocsTask = register("generateCommandDocs", JavaExec::class.java) {
        group = "build"
        description = "Generate command documentation for the wiki"
        javaLauncher = javaLauncherProvider
        workingDir = layout.projectDirectory.dir("run").asFile
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("com.zenith.util.CommandDocsGenerator")
        val outputFile = project.layout.buildDirectory.file("Commands.md")
        args = listOf(outputFile.get().asFile.absolutePath)
        environment("ZENITH_DEV", "true")
        jvmArgs = listOf("-Xmx300m", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
        outputs.file(outputFile)
    }
    val pluginLoadTestTask = register("pluginLoadTest", PluginLoadTestTask::class.java) {
        group = "verification"
        description = "Tests that plugins are able to load"
        javaLauncher = javaLauncherProvider
        workingDir = layout.projectDirectory.dir("run").asFile
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("com.zenith.Proxy")
    }
    val updateWikiTask = register<UpdateWikiTask>("updateWiki") {
        inputs.files(generateCommandDocsTask.get().outputs.files)
        wikiDirectory = layout.projectDirectory.dir("docs/wiki").asFile
        wikiFiles = files(project.layout.buildDirectory.file("Commands.md"))
    }
    processResources {
        dependsOn(releaseTagTask, mcVersionTask, commitHashTask, javaPathTask)
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
    build {
        dependsOn(shadowJar, updateWikiTask)
    }
    nativeCompile {
        notCompatibleWithConfigurationCache("not compatible with configuration cache")
        classpathJar = shadowJar.flatMap { it.archiveFile }
        dependsOn(build)
    }
    generateResourcesConfigFile {
        notCompatibleWithConfigurationCache("not compatible with configuration cache")
        dependsOn(shadowJar)
    }
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
            // additional config in: `src/main/resources/META-INF/native-image/com.zenith/zenithproxy/native-image.properties
            buildArgs.addAll(
                "-H:DeadlockWatchdogInterval=30",
                "-H:+CompactingOldGen",
                "-H:+TrackPrimitiveValues",
                "-H:+TreatAllTypeReachableConditionsAsTypeReached",
                "-H:+UsePredicates",
                "--future-defaults=all",
                "-R:MaxHeapSize=200m",
                "-march=x86-64-v3",
                "--gc=serial",
                "-J-XX:MaxRAMPercentage=90",
//                "--enable-monitoring=nmt,jfr",
//                "-H:+PrintClassInitialization"
            )
            val pgoPath = providers.environmentVariable("GRAALVM_PGO_PATH").orNull
			val pgoInstrument = providers.environmentVariable("GRAALVM_PGO_INSTRUMENT").orNull
			val trace = providers.environmentVariable("GRAALVM_NATIVE_IMAGE_TRACE").orNull
            val buildReport = providers.environmentVariable("GRAALVM_BUILD_REPORT").orNull
            if (pgoPath != null) {
                println("Using PGO profile: $pgoPath")
                buildArgs.add("--pgo=$pgoPath")
                buildArgs.add("-H:+PGOPrintProfileQuality")
            } else {
                if (pgoInstrument != null) {
                    println("Instrumenting PGO")
                    buildArgs.add("--pgo-instrument")
                    buildArgs.add("-R:ProfilesDumpFile=profile.iprof")
                } else if (trace != null) {
					println("Enabling tracing agent")
					buildArgs.add("-H:Preserve=all")
				}
            }
            if (buildReport != null) {
                buildArgs.add("--emit build-report")
            }
            configurationFileDirectories.from(file("src/main/resources/META-INF/native-image"))
        }
        named("test") {
            javaLauncher = javaLauncherProvider
            quickBuild = true
            verbose = true
            debug = true
            // additional config in: `src/main/resources/META-INF/native-image/com.zenith/zenithproxy/native-image.properties
            buildArgs.addAll(
                "-H:DeadlockWatchdogInterval=30",
                "-H:+CompactingOldGen",
                "-H:+TrackPrimitiveValues",
                "-H:+TreatAllTypeReachableConditionsAsTypeReached",
                "-H:+UsePredicates",
                "--future-defaults=all",
                "-R:MaxHeapSize=200m",
                "-march=x86-64-v3",
                "--gc=serial",
                "-J-XX:MaxRAMPercentage=90",
            )
            configurationFileDirectories.from(file("src/main/resources/META-INF/native-image"))
        }
    }
    metadataRepository { enabled = true }
}

shadow {
    addShadowVariantIntoJavaComponent = false
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
            from(components["java"])
        }
        create<MavenPublication>("release") {
            groupId = "com.zenith"
            artifactId = "ZenithProxy"
            version = providers.environmentVariable("ZENITH_RELEASE_TAG").orElse("0.0.0+${project.version}").get()
            from(components["java"])
        }
    }
}
