package com.xsr.technique

import com.xsr.technique.config.DeepSeekProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication


@SpringBootApplication
@EnableConfigurationProperties(DeepSeekProperties::class)
class DeepseekApplication

fun main(args: Array<String>) {
    runApplication<DeepseekApplication>(*args)

}