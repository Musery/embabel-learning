package com.xsr.technique

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.fromForm
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.api.common.createObjectIfPossible
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.Person
import com.embabel.ux.form.Text
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonClassDescription("用户信息")
data class UserInformation(
    @Text(label = "出生日期")
    val day: String
)

@JsonClassDescription("卜卦用户")
@JsonDeserialize(`as` = YiPerson::class)
data class YiPerson(
    override val name: String,
    @get:JsonPropertyDescription("出生日期")
    val day: String
) : Person

/**
 * shell  x "我叫任重, 给我算一下今天运势"
 *
 * 当前例子主要展示通过fromForm提示补充输入
 *
 */
@Agent(description = "卜卦算命")
class Unit2InteractAgent {

    @Action
    fun extractPerson(userInput: UserInput, context: OperationContext): Person? =
        // All prompts are typesafe
        context.ai().withDefaultLlm().createObjectIfPossible(
            """
            根据用户输入提取名字, 创建Person对象:
            ${userInput.content}
            """.trimIndent()
        )

    @Action(cost = 100.0) // Make it costly so it won't be used in a plan unless there's no other path
    internal fun makeInformation(
        person: Person,
    ): UserInformation =
        fromForm("Let's get some astrological details for ${person.name}")


    @Action
    fun assembleYiPerson(
        person: Person,
        information: UserInformation,
    ): YiPerson {
        return YiPerson(
            name = person.name,
            day = information.day,
        )
    }

    @Action
    fun extractYiPerson(userInput: UserInput, context: OperationContext): YiPerson? =
        context.ai().withAutoLlm().createObjectIfPossible(
            """
            根据用户输入提取名字和生日, 创建Person对象:
            ${userInput.content}
            """.trimIndent()
        )

    @Action
    @AchievesGoal(description = "卜卦算命")
    fun yi(yiPerson: YiPerson, context: OperationContext): String =
        context.ai().withAutoLlm().create(
            """
            根据姓名:${yiPerson.name}和出生日期:${yiPerson.day} 算一下今天的运势
            """.trimIndent()
        )
}