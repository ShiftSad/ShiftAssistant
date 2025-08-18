package dev.shiftsad.shiftAssistant.service

import com.aallam.openai.api.embedding.EmbeddingRequest
import com.aallam.openai.api.model.ModelId
import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.holder.OpenAIHolder

class EmbeddingService {
    suspend fun embed(text: String): FloatArray {
        val cfg = ConfigHolder.get()
        val client = OpenAIHolder.get()

        val req = EmbeddingRequest(
            model = ModelId(cfg.openAI.embeddingsModel),
            input = listOf(text)
        )
        val res = client.embeddings(req)
        val vector = res.embeddings.firstOrNull()?.embedding
            ?: error("No embedding returned")

        return FloatArray(vector.size) { i -> vector[i].toFloat() }
    }
}