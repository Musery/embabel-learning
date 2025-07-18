# Embabel框架学习记录

> “正常人谁写日记啊？写出来的那能叫心里话吗？下贱！”

| 模型               | Embabel | langchain |
|------------------|---------|-----------|
| ollama           | ✅       | ✅         |
| dokcer model run | ✅       | ✅         |
| Anthropic        | ✅       | ✅         |
| Bedrock          | ✅       | ✅         |
| Openai           | ✅       | ✅         |

## 启用Shell交互

```kotlin
@EnableAgentShell
class Application
```

```shell-command
       set-options: Set options
       blackboard, bb: Show last blackboard: The final state of a previous operation
       models: List available models
       tool-stats: Show tool stats
       profiles: List all active Spring profiles
       rag-service: List available rag services
       tools: List available tool groups
       execute, x: Execute a task. Put the task in double quotes. For example:
       	x "Lynda is a scorpio. Find news for her" -p
       platform: Information about the AgentPlatform
       choose-goal: Try to choose a goal for a given intent. Show all goal rankings
       ingest: ingest
       agents: List agents
       exit, quit, bye: Exit the application
       show-options: Show options
       chat: Chat
       conditions: List conditions
       actions: List actions
       runs: Show recent agent process runs. This is what actually happened, not just what was planned.
       goals: List goals
```

## Embabel基础配置

```kotlin
@ConfigurationProperties("embabel.models")
@Validated
data class ConfigurableModelProviderProperties(
    val llms: Map<String, String> = emptyMap(),
    val embeddingServices: Map<String, String> = emptyMap(),
    val defaultLlm: String = "gpt-4.1-mini",
    val defaultEmbeddingModel: String? = null,
)
```

根据ConfigurableModelProviderProperties确定defaultLlm是gpt-4.1-mini
需要根据使用的大模型, 修改defaultLlm配置, 比如

```yaml
embabel:
  models:
    default-llm: deepseek-r1:7b

```

## 启用Ollama连接配置参数

```kotlin
@AgentPlatform("ollama")
class OllamaApplication
```

注入源码类`OllamaModels`
OllamaModels永远都会加载, 但当配置@AgentPlatform("ollama")才会真正生效

```kotlin
/**
 * Load Ollama local models, both LLMs and embedding models.
 * This class will always be loaded, but models won't be loaded
 * from Ollama unless the "ollama" profile is set.
 */
@ExcludeFromJacocoGeneratedReport(reason = "Ollama configuration can't be unit tested")
@Configuration
class OllamaModels(
    @Value("\${spring.ai.ollama.base-url}")
    private val baseUrl: String,
    private val configurableBeanFactory: ConfigurableBeanFactory,
    private val environment: Environment,
    private val properties: ConfigurableModelProviderProperties,
) {
    /**
     * 通过接口获取模型信息注入
     */
    private fun loadModels(): List<Model> =
        try {
            val restClient = RestClient.create()
            val response = restClient.get()
                .uri("$baseUrl/api/tags")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body<ModelResponse>()

            response?.models?.mapNotNull { modelDetails ->
                // Additional validation to ensure model names are valid
                if (modelDetails.name.isNotBlank()) {
                    Model(
                        name = modelDetails.name.replace(":", "-").lowercase(),
                        model = modelDetails.name,
                        size = modelDetails.size
                    )
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            logger.warn("Failed to load models from {}: {}", baseUrl, e.message)
            emptyList()
        }

    @PostConstruct
    fun registerModels() {
        if (!environment.activeProfiles.contains(OLLAMA_PROFILE)) {
            logger.info("Ollama models will not be queried as the '{}' profile is not active", OLLAMA_PROFILE)
            return
        }

    }
}

```

##启用OPENAI AI配置参数

```yaml
OPENAI_BASE_URL: http
OPENAI_API_KEY: 
```

注入源码类`OpenAiModels`
当配置OPENAI_API_KEY参数时, 启用OPENAI模型

```kotlin
/**
 * Well-known OpenAI models.
 */
@Configuration
@ConditionalOnProperty("OPENAI_API_KEY")
class OpenAiModels(
    @Value("\${OPENAI_BASE_URL:#{null}}")
    baseUrl: String?,
    @Value("\${OPENAI_API_KEY}")
    apiKey: String,
    observationRegistry: ObservationRegistry,
    private val properties: OpenAiProperties,
) {
    /**
     * 注入gpt-4.1-mini
     */
    @Bean
    fun gpt41mini(): Llm {

    }

    /**
     * 注入gpt-4.1
     */
    @Bean
    fun gpt41(): Llm {

    }

    /**
     * 注入gpt-4.1-nano
     */
    @Bean
    fun gpt41nano(): Llm {

    }
}
```

ollama是会通过API接口获取模型名称, openai则是手动注入

