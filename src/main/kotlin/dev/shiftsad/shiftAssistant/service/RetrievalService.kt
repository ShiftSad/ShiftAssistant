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
        val cfg = ConfigHolder.get()
        val chunks = chunkText(text, cfg.retrieval.maxChunkChars)
        var count = 0
        for ((idx, chunk) in chunks.withIndex()) {
            val id = "$sourceId#$idx"
            val emb = embeddingService.embed(chunk)
            repo.upsertChunk(id = id, text = chunk, embedding = emb)
            count++
        }
        repo.commit()
        return count
    }

    suspend fun search(query: String): List<Neighbor> {
        val cfg = ConfigHolder.get()
        val emb = embeddingService.embed(query)
        return repo.topK(
            embedding = emb,
            k = cfg.retrieval.topK,
            minCosine = cfg.retrieval.minCosine
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