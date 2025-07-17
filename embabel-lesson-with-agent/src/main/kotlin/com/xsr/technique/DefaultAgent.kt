package com.xsr.technique

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.create
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.prompt.persona.Persona
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria
import org.slf4j.LoggerFactory


data class Solution(val result: Int)

@Agent(description = "基础数学计算智能体")
class DefaultAgent {


    companion object {
        private val logger = LoggerFactory.getLogger(DefaultAgent::class.java)
    }

    val teacher = Persona.create(
        name = "泰勒 斯坦",
        persona = "高中数学老师",
        voice = "严肃",
        objective = "为数学问题提供解答思路"
    )


    @AchievesGoal("答案")
    @Action
    fun chatWithLlm(userInput: UserInput): Solution = using(
        LlmOptions(criteria = ModelSelectionCriteria.Auto)
            .withTemperature(.3)
    ).withPromptContributor(teacher)
        .create(
            """
            根据数学问题描述, 提供数学思维解答
            
            # 数学问题描述
            ${userInput.content}
        """.trimIndent()
        )

}