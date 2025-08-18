package dev.shiftsad.shiftAssistant.repository

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.backward_codecs.lucene100.Lucene100Codec
import org.apache.lucene.backward_codecs.lucene90.Lucene90HnswVectorsFormat.DEFAULT_BEAM_WIDTH
import org.apache.lucene.backward_codecs.lucene90.Lucene90HnswVectorsFormat.DEFAULT_MAX_CONN
import org.apache.lucene.codecs.KnnVectorsFormat
import org.apache.lucene.codecs.KnnVectorsReader
import org.apache.lucene.codecs.KnnVectorsWriter
import org.apache.lucene.codecs.lucene99.Lucene99HnswVectorsFormat
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.KnnFloatVectorField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.SegmentReadState
import org.apache.lucene.index.SegmentWriteState
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.KnnFloatVectorQuery
import org.apache.lucene.store.FSDirectory
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

data class Neighbor(val id: String, val text: String, val score: Float)

class CustomHnswVectorsFormat(
    maxConn: Int = DEFAULT_MAX_CONN,
    beamWidth: Int = DEFAULT_BEAM_WIDTH
) : KnnVectorsFormat("CustomHnsw") {

    private val delegate = Lucene99HnswVectorsFormat(maxConn, beamWidth)

    override fun getMaxDimensions(fieldName: String?): Int {
        return 1536
    }

    override fun fieldsWriter(state: SegmentWriteState): KnnVectorsWriter {
        return delegate.fieldsWriter(state)
    }

    override fun fieldsReader(state: SegmentReadState): KnnVectorsReader {
        return delegate.fieldsReader(state)
    }
}

class CustomCodec : Lucene100Codec() {
    private val customFormat = CustomHnswVectorsFormat()

    override fun getKnnVectorsFormatForField(field: String?): KnnVectorsFormat {
        return customFormat
    }
}

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
        cfg.codec = CustomCodec()
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