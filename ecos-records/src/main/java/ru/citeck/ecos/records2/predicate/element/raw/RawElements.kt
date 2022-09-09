package ru.citeck.ecos.records2.predicate.element.raw

import ru.citeck.ecos.records2.predicate.element.Elements
import ru.citeck.ecos.records2.predicate.element.elematts.RecordAttsElement
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType

class RawElements<T : Any>(
    private val recordsService: RecordsService,
    private val records: Iterable<T?>
) : Elements<RecordAttsElement<T>> {

    companion object {
        const val BATCH_SIZE = 1000
    }

    override fun getElements(attributes: List<String>): Iterable<RecordAttsElement<T>> {
        return IterableRawRecords(records, attributes)
    }

    inner class Iter(val impl: Iterator<T?>, attributes: List<String>) : Iterator<RecordAttsElement<T>> {

        private val attsToRequest = attributes.associate {
            if (it.contains('?') || it.contains('}')) {
                it to it
            } else {
                it to "$it${ScalarType.RAW.schema}"
            }
        }

        var recordsBatch: List<RecordAttsElement<T>> = emptyList()
        var batchIdx = -1

        override fun hasNext(): Boolean {
            if (batchIdx == -1 || batchIdx == recordsBatch.size) {
                val batch = mutableListOf<T>()
                while (batch.size < BATCH_SIZE && impl.hasNext()) {
                    val next = impl.next() ?: continue
                    batch.add(next)
                }
                val atts = recordsService.getAtts(batch, attsToRequest)
                val newBatch = mutableListOf<RecordAttsElement<T>>()
                for (i in 0 until batch.size) {
                    newBatch.add(RecordAttsElement(batch[i], atts[i]))
                }
                recordsBatch = newBatch
                batchIdx = 0
            }
            return batchIdx < recordsBatch.size
        }

        override fun next(): RecordAttsElement<T> {
            return recordsBatch[batchIdx++]
        }
    }

    inner class IterableRawRecords(
        val records: Iterable<T?>,
        val attributes: List<String>
    ) : Iterable<RecordAttsElement<T>> {

        override fun iterator(): Iterator<RecordAttsElement<T>> {
            return Iter(records.iterator(), attributes)
        }
    }
}
