pluginManagement {
    plugins {
        kotlin("jvm") version extra["kotlin.version"] as String
        kotlin("plugin.noarg") version extra["kotlin.version"] as String
        kotlin("plugin.spring") version extra["kotlin.version"] as String

        id("io.spring.dependency-management") version "1.1.0"
        id("org.springframework.boot") version extra["springboot.version"] as String

    }
}
rootProject.name = "embabel-learning"
include("embabel-lesson-with-ollama")
include("embabel-lesson-with-agent")
include("embabel-lesson-with-openai")
include("embabel-lesson-with-deepseek")