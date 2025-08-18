package com.xsr.technique

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.prompt.persona.Persona
import org.slf4j.LoggerFactory


data class Solution(val result: String, val thinking: String)

/**
 * 最基础Agent 单Action
 */
@Agent(description = "数学计算")
class DefaultAgent {


    companion object {
        private val logger = LoggerFactory.getLogger(DefaultAgent::class.java)
    }

    /**
     * 角色锚定, 保持问题解答一致性
     */
    val teacher = Persona.create(
        name = "华罗庚先生",
        persona = "小学数学老师",
        voice = "耐心",
        objective = "进行数学问题解答, 给出详细的执行步骤"
    )


    @AchievesGoal(description = "数学问题解答")
    @Action
    fun chatWithLlm(userInput: UserInput, context: OperationContext): Solution = context.ai()
        .withDefaultLlm().withPromptContributor(teacher)
        .withToolGroups(setOf(CoreToolGroups.MATH))
        .create(
            """
            根据数学问题描述, 提供数学思维解答
            
            # 数学问题描述
            ${userInput.content}
        """.trimIndent()
        )

}