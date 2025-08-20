package com.xsr.technique


import com.embabel.agent.config.annotation.EnableAgentShell
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication


@SpringBootApplication
@EnableAgentShell
@EnableConfigurationProperties(GeminiProperties::class)
class GeminiApplication


fun main(args: Array<String>) {
    runApplication<GeminiApplication>(*args)
}