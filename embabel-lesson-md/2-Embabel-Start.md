Embabel核心的功能就是规划步骤,
其并非使用LLM的AI算法实现.而是基于GOAP([目标导向行动规划 (Goal Oriented Action Planning)](https://www.reddit.com/r/godot/comments/xgrk0g/goap_goaloriented_action_planning_is_absolutely/))
算法。本文就是针对设计原理对照源码(版本0.1.1)进行讲解

# 为什么要有规划步骤?

LLM 很智能，但却不透明。它们无法向我们解释*为什么*
会产生特定的输出，也无法解释为什么选择特定的方法来解决问题。它们可以使用多种工具执行复杂的任务。但在这种情况下，它们的表现难以预测，而且有些任务过于复杂，或者需要与现有系统深度集成。

因此，我们引入了Agent框架。Agent框架通过将*动作*
链接在一起来工作。不同框架的术语各不相同，但概念却大同小异。每个动作都会运行一个高度针对性的提示或执行代码。鉴于LLM“思维”的不透明性，动作链接通常以代码形式完成：例如，通过显式编写状态机代码，或通过定义序列。这种方法易于解释，并能产生更确定的结果。然而，它在锁定方面做得有些过头了。它使系统更具确定性，但限制了其功能，并使其难以扩展。有没有一种方法可以实现
*安全*自主，使系统足够智能，能够执行那些无需直接编码的操作？

只要规划是可解释且确定性的，我们就能从人工智能的规划中受益匪浅。Embabel使用的规划算法是[目标导向行动规划 (Goal Oriented Action Planning)](https://www.reddit.com/r/godot/comments/xgrk0g/goap_goaloriented_action_planning_is_absolutely/)
。这是一种在游戏中广泛使用的算法，它让我想起了[通用问题求解器 (General Problem Solver)](https://en.wikipedia.org/wiki/General_Problem_Solver)
，这是一种在很多年就发明的非常早期的人工智能算法。我们行业的许多核心理念*都源于*此，而许多古老的理念在今天仍然具有现实意义。

# GOAP算法原理

GOAP 本质上是一种路径查找算法。它可以找到从未被编程过的路径。然而，它是确定性的，它只能选择明确添加到系统中的单个操作（步骤）的路径。如果
GOAP 找到了一条路径，它可以解释为什么这条路径是最便宜的有效路径。

GOAP 根据事实寻找路径，这些路径表现为前置(pre), 后置(post)和条件(conditions)。 **行动**
具有先决条件和预期的后置条件。因此，在给定状态和目标之间可以找到零个或多个行动链。

前提条件是绝对的。预期的后置条件只是：*预期*会实现某种副作用。信任，然后验证。任何操作执行后，世界状态都会重新评估，从而允许重新规划。

# Embabel实现解析

这张图展示了Embabel在执行GOAP的步骤

![image](assets/image-20250827073514-qfxhj0j.png)

## 源码解析

源码解析前需要了解的名词包括

- Goal（目标） ：最终执行结果
- Action（行动）: 最小执行单元， 可以认为是执行步骤（Step）
- Condition (条件): action可执行的判定依据
- Agent：包含Action，Goal的定义域
- AgentProcess（Agent进程）：具体的Goal执行实例

- processOptions（进程配置）：Agent进程配置数据， 包含`contextId`， `blackboard`等
- WordStatus(世界状态)： AgentProcess的状态，用与Condition判定Action的可执行依据
- Plan（规划步骤）： 完成Goal的需要执行的Action链路， 可以认为是List<Actio>

### 流程描述

Embabel框架从源码角度来看， 其实就像是一个提供用户自定义去设置Agent包括Goal和Action，Condition。

运行时

1. 确认Goal创建AgentProcess
2. 然后根据WordStatus的既定的Goal和Condition去规划Plan，若已达到Goal或者无法达到Goal则执行第4步
3. 然后按Plan执行Action， 执行后再重复第2步
4. 完成或者异常输出结果

### 源码透析

第一步： 确认Goal创建AgentProcess

Embabel选择目标的方式有2种

1. **程序员绑定API与Agent:**   用户是以传统交互方式触发, 比如界面UI的按钮会触发某个HTTP接口, 而这个接口固定执行某些Agent。比如下面代码：

    ```kotlin
    @PostMapping("/plan")
        fun planJourney(
            @ModelAttribute form: JourneyPlanForm,
            model: Model
        ): String {
            // 根据API接口参数构建Agent输入数据模型
            val travelBrief = JourneyTravelBrief(
                from = form.from,
                to = form.to,
                transportPreference = form.transportPreference,
                brief = form.brief,
                departureDate = form.departureDate,
                returnDate = form.returnDate,
            )

            // Convert form travelers to domain objects
            val travelersList = form.travelers.map { travelerForm ->
                Traveler(name = travelerForm.name, about = travelerForm.about)
            }
            val travelers = Travelers(travelers = travelersList)
    		// 手动选择对应agent
            val agent = agentPlatform.agents().singleOrNull { it.name.lowercase().contains("trip") }
                ?: error("No travel agent found. Please ensure the tripper agent is registered.")
            // 构建agent进程
            val agentProcess = agentPlatform.createAgentProcessFrom(
                agent = agent,
                processOptions = ProcessOptions(
                    verbosity = Verbosity(
                        showPrompts = true,
                        showLlmResponses = true,
                    ),
                    // This is expensive and that's OK
                    budget = Budget(
                        tokens = Budget.DEFAULT_TOKEN_LIMIT * 3,
                    )
                ),
                travelBrief, travelers
            )
            // 执行
            agentPlatform.start(agentProcess)
        }
    ```

2. **理解用户对话选择合适Agent:**  用户是以AI对话(文本, 语音)方式表达意图， 然后基于算法选择合适的Agent运行。比如下面代码：

    ```kotlin
    @Service
    class Autonomy(
        val agentPlatform: AgentPlatform,
        private val ranker: Ranker,
        val properties: AutonomyProperties,
    ) {

    	/**
    	 * 根据用户输入内容 @param intent选择目标(Goal)以及Agent进行运行
    	 */
    	@Throws(ProcessExecutionException::class)
        fun chooseAndRunAgent(
            intent: String,
            processOptions: ProcessOptions = ProcessOptions(),
        ): AgentProcessExecution {
            val userInput = UserInput(intent)
    		
    		// ranker 用户根据用户输入内容确认目标(Goal)的行为
            val rankerToUse = if (processOptions.test && ranker !is FakeRanker) {
                RandomRanker()
            } else {
                ranker
            }

            val agentChoiceEvent = RankingChoiceRequestEvent(
                agentPlatform = agentPlatform,
                type = Agent::class.java,
                basis = userInput,
                choices = agentPlatform.agents(),
            )
            eventListener.onPlatformEvent(agentChoiceEvent)
    		// 通过ranker获取可执行agent
            val agentRankings = rankerToUse
                .rank(
                    description = "agent",
                    userInput = userInput.content,
                    rankables = agentPlatform.agents()
                )
    		// 获取可信度最高的agent进行执行
            val credibleAgents = agentRankings
                .rankings()
                .filter { it.score > properties.agentConfidenceCutOff }
            val agentChoice = credibleAgents.firstOrNull()
            if (agentChoice == null) {
                eventListener.onPlatformEvent(
                    agentChoiceEvent.noDeterminationEvent(
                        rankings = agentRankings,
                        confidenceCutoff = properties.agentConfidenceCutOff
                    )
                )
                throw NoAgentFound(agentRankings = agentRankings, basis = userInput)
            }

            logger.debug(
                "Agent choice {} with confidence {} for user intent {}: Choices were {}",
                agentChoice.match.name,
                agentChoice.score,
                intent,
                agentPlatform.agents().joinToString("\n") { it.name },
            )
            eventListener.onPlatformEvent(
                agentChoiceEvent.determinationEvent(
                    choice = agentChoice,
                    rankings = agentRankings,
                )
            )

            val agent = agentChoice.match
            eventListener.onPlatformEvent(
                DynamicAgentCreationEvent(
                    agent = agentChoice.match,
                    agentPlatform = agentPlatform,
                    basis = userInput,
                )
            )
    		// 执行用户输入(userInput)和进程上下文(processOptions)根据输入执行Agent
            return runAgent(userInput, processOptions, agent)
        }
    }
    ```

第二, 三, 四步：AgentProcess运行。以SimpleAgentProcess的formulateAndExecutePlan实现为例：

```kotlin
override fun formulateAndExecutePlan(worldState: WorldState): AgentProcess {
    // 进行路径规划， 找到最优执行路径 （这里面会根据A*算法（AStarGoapPlanner）进行路径规划）
    val plan = planner.bestValuePlanToAnyGoal(system = agent.planningSystem)
    if (plan == null) {
        logger.info(
            "❌ Process $id stuck\n" +
                    """|No plan from:
                   |${worldState.infoString(verbose = true, indent = 1)}
                   |in:
                   |${agent.planningSystem.infoString(verbose = true, 1)}
                   |context:
                   |${blackboard.infoString(true, 1)}
                   |"""
                        .trimMargin()
                        .indentLines(1)
        )
        setStatus(AgentProcessStatusCode.STUCK)
        return this
    }

    if (goal != null && goal?.name != plan.goal.name) {
        logger.info("Process {} goal changed: {} -> {}", this.id, goal?.name, plan.goal.name)
        require(processOptions.allowGoalChange) {
            "Process ${this.id} goal changed from ${goal?.name} to ${plan.goal.name}, but allowGoalChange is false"
        }
    }
    _goal = plan.goal

    if (plan.isComplete()) {
        logger.debug(
            "✅ Process {} completed, achieving goal {} in {} seconds",
            this.id,
            plan.goal.name,
            this.runningTime.seconds,
        )
        platformServices.eventListener.onProcessEvent(
            GoalAchievedEvent(
                agentProcess = this,
                worldState = worldState,
                goal = plan.goal,
            )
        )
        logger.debug("Final blackboard: {}", blackboard.infoString())
        setStatus(AgentProcessStatusCode.COMPLETED)
    } else {
        platformServices.eventListener.onProcessEvent(
            AgentProcessPlanFormulatedEvent(
                agentProcess = this,
                worldState = worldState,
                plan = plan,
            )
        )
        logger.debug("▶️ Process {} running: {}\n\tPlan: {}", id, worldState, plan.infoString())
        val agent = agent.actions.singleOrNull { it.name == plan.actions.first().name }
            ?: error(
                "No unique action found for ${plan.actions.first().name} in ${agent.actions.map { it.name }}: Actions are\n${
                    agent.actions.joinToString(
                        "\n"
                    ) { it.name }
                }")
        // 执行下一步Action
        val actionStatus = executeAction(agent)
        // 设置进程状态
        setStatus(actionStatusToAgentProcessStatus(actionStatus))
    }
    return this
}
```

注：**A*算法实现源码**

```kotlin
class AStarGoapPlanner(worldStateDeterminer: WorldStateDeterminer) :
    OptimizingGoapPlanner(worldStateDeterminer) {

    override fun planToGoalFrom(
        startState: GoapWorldState,
        actions: Collection<GoapAction>,
        goal: GoapGoal,
    ): GoapPlan? {
        // Open list - states to be evaluated
        val openList = PriorityQueue<SearchNode>()

        // Maps each state to its best known cost
        val gScores = mutableMapOf<GoapWorldState, Double>().withDefault { Double.MAX_VALUE }

        // Maps each state to its best predecessor state and action
        val cameFrom = mutableMapOf<GoapWorldState, Pair<GoapWorldState, GoapAction?>>()

        // Set to track states that have been fully evaluated
        val closedSet = mutableSetOf<GoapWorldState>()

        // Initialize with start node
        gScores[startState] = 0.0
        openList.add(SearchNode(startState, 0.0, heuristic(startState, goal)))

        // Track the best goal state found so far
        var bestGoalNode: SearchNode? = null
        var bestGoalScore = Double.MAX_VALUE

        // Track number of iterations to prevent potential infinite loops
        var iterationCount = 0
        val maxIterations = 10000 // Adjust as needed

        while (openList.isNotEmpty() && iterationCount < maxIterations) {
            iterationCount++
            val current = openList.poll()

            // If we've already found a goal state with a better score, we can skip this node
            if (bestGoalNode != null && current.gScore >= bestGoalScore) {
                continue
            }

            // Skip if we've already processed this state
            if (current.state in closedSet) continue

            // Mark as processed
            closedSet.add(current.state)

            // Check if this is a goal state
            if (goal.isAchievable(current.state)) {
                // Only update if this is a better goal state than we've found before
                if (bestGoalNode == null || current.gScore < bestGoalScore) {
                    bestGoalNode = current
                    bestGoalScore = current.gScore
                }
                continue // No need to explore further from this goal state
            }

            // Try each possible action from the current state
            for (action in actions) {
                if (!action.isAchievable(current.state)) continue

                // Calculate the new state after applying this action
                val nextState = applyAction(current.state, action)

                // Skip if this action doesn't actually change the state (prevents loops)
                if (nextState == current.state) continue

                // Calculate total cost to reach nextState via this path
                val tentativeGScore = gScores.getValue(current.state) + action.cost

                // Skip if this path would already be more expensive than our best goal so far
                if (bestGoalNode != null && tentativeGScore >= bestGoalScore) {
                    continue
                }

                // If we found a better path to nextState
                if (tentativeGScore < gScores.getValue(nextState)) {
                    // Record this better path
                    cameFrom[nextState] = Pair(current.state, action)
                    gScores[nextState] = tentativeGScore

                    // Only add to open list if not in closed set, or if we've found a better path
                    if (nextState !in closedSet) {
                        openList.add(SearchNode(nextState, tentativeGScore, heuristic(nextState, goal)))
                    } else {
                        // If we find a better path to a "closed" state, reopen it
                        closedSet.remove(nextState)
                        openList.add(SearchNode(nextState, tentativeGScore, heuristic(nextState, goal)))
                    }
                }
            }
        }

        // If we found a goal state, reconstruct and optimize the plan
        if (bestGoalNode != null) {
            val plan = reconstructPath(cameFrom, bestGoalNode.state)

            // First pass: apply aggressive backward planning optimization
            val optimizedPlan = backwardPlanningOptimization(plan, startState, goal)

            // Second pass: remove any actions that don't contribute to the goal
            val finalPlan = forwardPlanningOptimization(optimizedPlan, startState, goal)

            return GoapPlan(finalPlan, goal, worldState = startState)
        }

        // No path found
        return null
    }

}
```