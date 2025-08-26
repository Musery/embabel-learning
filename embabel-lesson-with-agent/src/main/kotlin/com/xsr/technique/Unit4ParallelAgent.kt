package com.xsr.technique

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.RequireNameMatch
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.prompt.persona.Persona
import org.springframework.web.bind.annotation.RequestParam

/**
 * 角色锚定, 保持内容生成一致性
 */
val chinese = Persona.create(
    name = "李一白",
    persona = "中国奇闻怪谈小说家",
    voice = "思维敏捷",
    objective = "根据用户输入主题, 创建文学小说"
)

/**
 * 角色锚定, 保持内容生成一致性
 */
val european = Persona.create(
    name = "亚历山大",
    persona = "古希腊神话小说家",
    voice = "思维敏捷",
    objective = "根据用户输入主题, 创建文学小说"
)

data class Story(val content: String)

/**
 *  非完全并行化工作流模式
 *  x "生成关于'主题: 坚持不懈的完成目标'的文化小故事"
 *
 *  这个Agent执行的链路是  getTopicFromInput -> chineseStory -> europeanStory -> merge
 *  其中 chineseStory , europeanStory 两个action并没有依赖关系, 所以是可以并行的, 但是目前的
 *  SimpleAgentBuilder只会串行执行, 后面会考虑提交pr去增加此并行功能
 *  详见https://github.com/embabel/embabel-agent/issues/90
 *
 */
@Agent(description = "主题故事生成")
class Unit4ParallelAgent {


    @Action(outputBinding = "topic")
    fun getTopicFromInput(@RequestParam userInput: UserInput, context: OperationContext): String =
        context.ai().withDefaultLlm()
            .create(
                """
                根据用户输入的内容\"${userInput.content}\"提取想要生成的主题信息, 字数不超过10个
            """.trimIndent()
            )

    @Action(outputBinding = "chinese")
    fun chineseStory(@RequireNameMatch topic: String, context: OperationContext): Story =
        createStory(topic, context, chinese)

    @Action(outputBinding = "european")
    fun europeanStory(@RequireNameMatch topic: String, context: OperationContext): Story =
        createStory(topic, context, european)


    fun createStory(topic: String, context: OperationContext, persona: Persona): Story =
        context.ai().withDefaultLlm().withPromptContributor(persona)
            .create(
                """
                创建关于${topic}的个性化文化体系的小说, 字数不超过50字
                
            """.trimIndent()
            )

    @AchievesGoal(description = "根据主题创建多个文化体系的小说")
    @Action(outputBinding = "result")
    fun merge(@RequireNameMatch european: Story, @RequireNameMatch chinese: Story, context: OperationContext): String {
        return """
                中文故事内容:
                    ${chinese.content}
                欧洲故事内容:
                    ${european.content}
            """
    }


}