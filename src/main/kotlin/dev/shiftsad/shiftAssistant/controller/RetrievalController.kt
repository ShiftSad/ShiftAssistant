package dev.shiftsad.shiftAssistant.controller

import dev.shiftsad.shiftAssistant.repository.LuceneVectorRepository
import dev.shiftsad.shiftAssistant.repository.Neighbor
import dev.shiftsad.shiftAssistant.service.EmbeddingService
import dev.shiftsad.shiftAssistant.service.RetrievalService
import java.nio.file.Path

class RetrievalController(indexDir: Path) : AutoCloseable {

    private val repo = LuceneVectorRepository(indexDir)
    private val embeddingService = EmbeddingService()
    private val retrievalService = RetrievalService(repo, embeddingService)

    suspend fun ingest(sourceId: String, text: String): Int {
        return retrievalService.ingestDocument(sourceId, text)
    }

    suspend fun search(query: String): List<Neighbor> {
        return retrievalService.search(query)
    }

    fun clearIndex() {
        repo.clear()
    }

    override fun close() {
        repo.close()
    }
}