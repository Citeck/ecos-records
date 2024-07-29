package ru.citeck.ecos.records2.predicate.element.raw

import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records2.predicate.element.elematts.RecordAttsElementAtts
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType

class RawElement<T>(
    val recordsService: RecordsService,
    val record: T
) : Element {

    override fun getAttributes(attributes: List<String>): RecordAttsElementAtts {
        val attsToRequest = attributes.associate {
            if (it.contains('?') || it.contains('}')) {
                it to it
            } else {
                it to "$it${ScalarType.RAW.schema}"
            }
        }
        val atts = recordsService.getAtts(record, attsToRequest)
        return RecordAttsElementAtts(atts)
    }
}
