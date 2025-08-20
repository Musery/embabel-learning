package com.xsr.technique

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.Condition
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.prompt.persona.Persona


data class Solution(val answer: String, val judgement: Judgement?)

data class Judgement(val judgement: Boolean, val reason: String)

/**
 * 自迭代优化评估模式
 * x "应用题: 已知小明家到公司的距离是15公里, 小明7:30出门以步行15公里的速度从家去上班, 30分钟后发现电脑忘带, 于是立刻借了一辆自行车以25公里每小时的速度回家取电脑然后去公司, 请问其几点能到公司?"
 *
 * 重点标记:
 * 1. @Action(canRerun=true)  canRerun标记代表action能重复运行
 * 2. pre=\[condition]        标记必须满足条件才能运行
 * 3. 最优先级action根据源码来判定, 会根据顺序+cost去判定
 *
 */
@Agent(description = "应用题计算")
class Unit3OptimizationAgent {


    /**
     * 角色锚定, 保持问题解答一致性
     */
    val student = Persona.create(
        name = "李木辛",
        persona = "11岁小学生",
        voice = "焦躁",
        objective = "进行数学应用题解答, 写出解题步骤"
    )

    /**
     * 角色锚定, 保持问题解答一致性
     */
    val teacher = Persona.create(
        name = "华罗庚先生",
        persona = "小学数学老师",
        voice = "耐心",
        objective = "批改应用题解答内容"
    )

    /**
     * 解答
     */
    @Action(post = [NEED_JUDGEMENT])
    fun answer(userInput: UserInput, context: OperationContext): Solution =
        Solution(answer(userInput = userInput, solution = null, context = context), null)

    /**
     * 根据答案进行批改
     */
    @Action(pre = [NEED_JUDGEMENT], post = [SATISFACTORY], canRerun = true)
    fun marking(userInput: UserInput, solution: Solution, context: OperationContext): Solution =
        Solution(
            solution.answer,
            context.ai()
                .withDefaultLlm().withPromptContributor(teacher)
                //.withToolGroups(setOf(CoreToolGroups.MATH))
                .create<Judgement>(
                    """
            根据数学问题描述和学生做题答案, 判断答案是否符合标准
            
            # 数学问题描述
            ${userInput.content}
            
            # 学生做题答案
            ${solution.answer}
            
            # 答案标准要求
            1. 要求答案必须以中文'解:'标记
            2. 要求罗列解题数学计算式子 比如 (30 - 10) / 20 = 1 等类似
        """.trimIndent()
                )
        )

    /**
     * 根据批改意见订正
     */
    @Action(post = [NEED_JUDGEMENT], canRerun = true)
    fun reAnswer(userInput: UserInput, solution: Solution, context: OperationContext): Solution =
        Solution(answer(userInput = userInput, solution = solution, context = context), null)


    fun answer(userInput: UserInput, solution: Solution?, context: OperationContext): String = context.ai()
        .withDefaultLlm().withPromptContributor(student)
        //.withToolGroups(setOf(CoreToolGroups.MATH))
        .create(
            """
            根据数学问题描述和老师批改意见修改之前的解题答案, 
            
            # 数学问题描述
            ${userInput.content}
            
            # 之前的解题答案
            ${solution?.answer ?: "暂无"}
            
            # 老师批改意见
            ${solution?.judgement?.reason ?: "暂无"}
        """.trimIndent()
        )


    @AchievesGoal(description = "根据题目进行解答, 并确定解答内容是否完整")
    @Action(pre = [SATISFACTORY])
    fun last(userInput: UserInput, solution: Solution, context: OperationContext): Solution = solution


    @Condition(name = SATISFACTORY)
    fun makeSatisfactory(
        solution: Solution,
    ): Boolean = solution.judgement?.judgement ?: false

    @Condition(name = NEED_JUDGEMENT)
    fun makeJudgement(
        solution: Solution,
    ): Boolean = null == solution.judgement

    companion object {
        const val SATISFACTORY = "SATISFACTORY"
        const val NEED_JUDGEMENT = "NEED_JUDGEMENT"
    }
}