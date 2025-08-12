package com.xsr.technique

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.create
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.prompt.persona.Persona
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria


val Collector = Persona(
    name = "Roald Dahl",
    persona = "新闻记者",
    voice = "严谨",
    objective = "收集最新相关新闻信息",
)

data class WebNews(val from: String, val message: String)

@Agent(description = "Search from Web base on user input")
class Unit3AgentWithWeb {

    @AchievesGoal(description = "新闻搜索")
    @Action
    fun search(userInput: UserInput): WebNews = using(
        LlmOptions(criteria = ModelSelectionCriteria.Auto)
            .withTemperature(.9), // Higher temperature for more creative output
    ).withPromptContributor(Collector).withToolGroup(CoreToolGroups.WEB)
        .create(
            """
            搜索关于'${userInput.content}'最新相关新闻内容
        """.trimIndent()
        )

}