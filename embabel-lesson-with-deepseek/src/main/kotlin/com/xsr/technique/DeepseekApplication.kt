package com.xsr.technique

import com.embabel.agent.config.annotation.EnableAgentShell
import com.xsr.technique.config.DeepSeekProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication


@SpringBootApplication
@EnableAgentShell // 启动shell交互
@EnableConfigurationProperties(DeepSeekProperties::class)
class DeepseekApplication

fun main(args: Array<String>) {
    runApplication<DeepseekApplication>(*args)
}