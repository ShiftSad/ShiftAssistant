package dev.shiftsad.shiftAssistant.service

import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.repository.LuceneVectorRepository
import dev.shiftsad.shiftAssistant.repository.Neighbor

class RetrievalService(
    private val repo: LuceneVectorRepository,
    private val embeddingService: EmbeddingService = EmbeddingService()
) {
    suspend fun ingestDocument(
        sourceId: String,
        text: String
    ): Int {
        val config = ConfigHolder.get()
        val chunks = chunkText(text, config.retrieval.maxChunkChars)
        var count = 0
        for ((idx, chunk) in chunks.withIndex()) {
            val id = "$sourceId#$idx"
            val embedding = embeddingService.embed(chunk)
            repo.upsertChunk(id = id, text = chunk, embedding = embedding)
            count++
        }
        repo.commit()
        return count
    }

    suspend fun search(query: String): List<Neighbor> {
        val config = ConfigHolder.get()
        val embedding = embeddingService.embed(query)
        return repo.topK(
            embedding = embedding,
            k = config.retrieval.topK,
            minCosine = config.retrieval.minCosine
        )
    }
}

fun chunkText(text: String, maxChunkChars: Int): List<String> {
    if (text.length <= maxChunkChars) return listOf(text)
    val chunks = mutableListOf<String>()
    var i = 0
    while (i < text.length) {
        val end = (i + maxChunkChars).coerceAtMost(text.length)
        chunks.add(text.substring(i, end))
        i = end
    }
    return chunks
}