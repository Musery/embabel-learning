package com.xsr.technique.config

import com.embabel.agent.common.RetryProperties
import com.embabel.common.ai.model.Llm
import com.embabel.common.ai.model.PerTokenPricingModel
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate

@ConfigurationProperties(prefix = "embabel.deekseek")
data class DeepSeekProperties(
    override val maxAttempts: Int = 10,
    override val backoffMillis: Long = 5000L,
    override val backoffMultiplier: Double = 5.0,
    override val backoffMaxInterval: Long = 180000L,
) : RetryProperties

@Configuration
@ConditionalOnProperty("DEEPSEEK_API_KEY")
class DeepSeekModels(
    @Value("\${DEEPSEEK_BASE_URL:#{null}}")
    baseUrl: String?,
    @Value("\${DEEPSEEK_API_KEY}")
    apiKey: String,
    observationRegistry: ObservationRegistry,
    private val properties: DeepSeekProperties,
) : DeepSeekCompatibleModelFactory(baseUrl, apiKey, observationRegistry) {

    init {
        logger.info("DeepSeek models are available: {}", properties)
    }

    @Bean
    fun chat(): Llm {
        return deepseekCompatible(
            model = DEEPSEEK_CHAT,
            pricingModel = PerTokenPricingModel(
                usdPer1mInputTokens = 0.40,
                usdPer1mOutputTokens = 0.60,
            ), provider = PROVIDER, knowledgeCutoffDate = LocalDate.of(2025, 3, 25)
        )
    }


    companion object {
        const val DEEPSEEK_CHAT = "deepseek-chat"

        const val PROVIDER = "deepseek"
        private val logger = LoggerFactory.getLogger(DeepSeekModels::class.java)
    }
}