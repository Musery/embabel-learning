import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

subprojects {
    group = "com.xsr.technique"
    version = "0.1.0"

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "embabel-releases"
            url = uri("https://repo.embabel.com/artifactory/libs-release")
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            name = "embabel-snapshots"
            url = uri("https://repo.embabel.com/artifactory/libs-snapshot")
            mavenContent {
                snapshotsOnly()
            }
        }
        maven {
            name = "Spring Milestones"
            url = uri("https://repo.spring.io/milestone")
        }
    }

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("io.spring.dependency-management")
    }

    dependencyManagement {
        dependencies {
            dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
            dependency("com.embabel.agent:embabel-agent-starter:${extra["embabel-agent.version"] as String}")
        }
    }

    /**
     * Java compilerArgs
     */
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    /**
     * kotlin compilerArgs
     */
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            javaParameters = true
        }
    }
}