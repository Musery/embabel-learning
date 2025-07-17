package com.xsr.technique.config

import com.embabel.agent.config.models.OpenAiModels
import com.embabel.common.ai.model.Llm
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.OptionsConverter
import com.embabel.common.ai.model.PricingModel
import com.embabel.common.util.loggerFor
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.model.NoopApiKey
import org.springframework.ai.model.SimpleApiKey
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.retry.RetryUtils
import org.springframework.retry.support.RetryTemplate
import java.time.LocalDate

open class DeepSeekCompatibleModelFactory(
    val baseUrl: String?,
    private val apiKey: String?,
    private val observationRegistry: ObservationRegistry,
) {

    @JvmOverloads
    fun deepseekCompatible(
        model: String,
        pricingModel: PricingModel,
        provider: String,
        knowledgeCutoffDate: LocalDate?,
        optionsConverter: OptionsConverter<*> = object : OptionsConverter<OpenAiChatOptions> {
            override fun convertOptions(options: LlmOptions): OpenAiChatOptions =
                OpenAiChatOptions.builder()
                    .temperature(options.temperature)
                    .topP(options.topP)
                    .maxTokens(options.maxTokens)
                    .presencePenalty(options.presencePenalty)
                    .frequencyPenalty(options.frequencyPenalty)
                    .topP(options.topP)
                    .build()
        },
        retryTemplate: RetryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE
    ): Llm {
        return Llm(
            name = model,
            model = OpenAiChatModel.builder()
                .defaultOptions(
                    OpenAiChatOptions.builder()
                        .model(model).build()
                )
                .openAiApi(openAiApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry).build(),
            provider = provider,
            optionsConverter = optionsConverter,
            pricingModel = pricingModel,
            knowledgeCutoffDate = knowledgeCutoffDate
        )
    }

    protected val openAiApi = createOpenAiApi()

    private fun createOpenAiApi(): OpenAiApi {
        val builder = OpenAiApi.builder()
            .apiKey(if (apiKey != null) SimpleApiKey(apiKey) else NoopApiKey())
        if (baseUrl != null) {
            loggerFor<OpenAiModels>().info("Using custom OpenAI base URL: {}", baseUrl)
            builder.baseUrl(baseUrl)
        }
        return builder.build()
    }
}