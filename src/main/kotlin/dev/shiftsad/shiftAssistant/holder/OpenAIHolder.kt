package dev.shiftsad.shiftAssistant.holder

import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import dev.shiftsad.shiftAssistant.AppConfig
import java.util.concurrent.atomic.AtomicReference

object OpenAIHolder {
    private val ref = AtomicReference<OpenAI?>()

    fun requireGet(): OpenAI =
        ref.get() ?: error("OpenAI client not initialized")

    fun initFromConfig(cfg: AppConfig) {
        val client =
            OpenAI(
                token = cfg.openAI.apiKey,
                host = OpenAIHost(baseUrl = cfg.openAI.baseUrl)
            )
        ref.set(client)
    }
}