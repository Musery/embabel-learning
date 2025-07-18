package com.xsr.technique

import com.embabel.agent.config.annotation.AgentPlatform
import com.embabel.agent.config.annotation.EnableAgentShell
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
//@EnableAgentShell // 启动shell交互
@AgentPlatform("ollama") // 启用ollama
class OllamaApplication

fun main(args: Array<String>) {
    runApplication<OllamaApplication>(*args)
}