import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.tasks.SourcesJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.protobuf)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.googleProtobuf.get()}"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }

            task.builtins {
                create("kotlin")
            }
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateProto")
}

kotlin.sourceSets.all {
    // Prevent build warnings for grpc's generated opt-in code
    languageSettings.optIn("kotlin.RequiresOptIn")
}

sourceSets {
    main {
        java {
            srcDirs(
                "build/generated/source/proto/main/grpc",
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/kotlin"
            )
        }
    }
}

dependencies {
    protobuf(project(":maestro-proto"))
    implementation(project(":maestro-utils"))
    implementation(project(":maestro-ios-driver"))
    implementation(project(":maestro-appium"))

    api(libs.graaljs)
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.stub)
    api(libs.grpc.netty)
    api(libs.grpc.protobuf)
    api(libs.grpc.okhttp)
    api(libs.google.protobuf.kotlin)
    api(libs.kotlin.result)
    api(libs.dadb)
    api(libs.square.okio)
    api(libs.image.comparison)
    api(libs.mozilla.rhino)
    api(libs.square.okhttp)
    api(libs.jarchivelib)
    api(libs.jackson.core.databind)
    api(libs.jackson.module.kotlin)
    api(libs.jackson.dataformat.yaml)
    api(libs.jackson.dataformat.xml)
    api(libs.apk.parser)

    implementation(project(":maestro-ios"))
    implementation(project(":maestro-web"))
    implementation(libs.google.findbugs)
    implementation(libs.axml)
    implementation(libs.selenium)
    implementation(libs.selenium.devtools)
    implementation(libs.jcodec)
    api(libs.slf4j)
    api(libs.logback) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.google.truth)
    testImplementation(libs.square.mock.server)
    testImplementation(libs.junit.jupiter.params)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.named("compileKotlin", KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjdk-release=1.8")
    }
}

tasks.withType(SourcesJar::class).configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
