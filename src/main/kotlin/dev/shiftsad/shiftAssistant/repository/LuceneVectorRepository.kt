package dev.shiftsad.shiftAssistant.repository

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.KnnFloatVectorField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.KnnFloatVectorQuery
import org.apache.lucene.store.FSDirectory
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

data class Neighbor(val id: String, val text: String, val score: Float)

class LuceneVectorRepository(indexPath: Path) : AutoCloseable {

    private val directory = FSDirectory.open(indexPath)
    private val writer: IndexWriter
    private val searcherRef = AtomicReference<IndexSearcher?>()

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_TEXT = "text"
        private const val FIELD_VECTOR = "vec"
    }

    init {
        val cfg = IndexWriterConfig(StandardAnalyzer())
        writer = IndexWriter(directory, cfg)
        refreshSearcher()
    }

    private fun refreshSearcher() {
        val reader = DirectoryReader.open(writer) // NRT reader
        searcherRef.getAndSet(IndexSearcher(reader))?.indexReader?.close()
    }

    fun upsertChunk(id: String, text: String, embedding: FloatArray) {
        writer.updateDocument(
            Term(FIELD_ID, id),
            Document().apply {
                add(StringField(FIELD_ID, id, Field.Store.YES))
                add(StoredField(FIELD_TEXT, text))
                add(KnnFloatVectorField(FIELD_VECTOR, embedding))
            }
        )
    }

    fun commit() {
        writer.commit()
        refreshSearcher()
    }

    fun topK(
        embedding: FloatArray,
        k: Int,
        minCosine: Double
    ): List<Neighbor> {
        val searcher = searcherRef.get() ?: error("Searcher not initialized")
        val query = KnnFloatVectorQuery(FIELD_VECTOR, embedding, max(1, k))
        val topDocs = searcher.search(query, k)
        val results = ArrayList<Neighbor>(topDocs.scoreDocs.size)
        for (sd in topDocs.scoreDocs) {
            val doc = searcher.storedFields().document(sd.doc)
            val id = doc.get(FIELD_ID) ?: ""
            val text = doc.get(FIELD_TEXT) ?: ""
            val cosine = sd.score // [0,1]
            if (cosine >= minCosine) {
                results.add(Neighbor(id = id, text = text, score = cosine))
            }
        }
        return results
    }

    override fun close() {
        searcherRef.getAndSet(null)?.indexReader?.close()
        writer.close()
        directory.close()
    }
}