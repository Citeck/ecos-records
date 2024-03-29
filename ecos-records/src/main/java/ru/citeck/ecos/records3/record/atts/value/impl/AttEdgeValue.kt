package ru.citeck.ecos.records3.record.atts.value.impl

import ru.citeck.ecos.records3.record.atts.value.AttEdge
import ru.citeck.ecos.records3.record.atts.value.AttValue
import java.util.*

class AttEdgeValue(val edge: AttEdge?) : AttValue {

    override fun getAtt(name: String): Any? {
        return when (name) {
            "name" -> edge?.name
            "val" -> edge?.value
            "vals" -> {
                val value: Any = edge?.value ?: emptyList<Any?>()
                if (value is Collection<*>) {
                    ArrayList(value)
                } else {
                    listOf(value)
                }
            }
            "title" -> edge?.title
            "description" -> edge?.description
            "protected" -> edge?.isProtected
            "mandatory" -> edge?.isMandatory
            "unreadable" -> edge?.isUnreadable
            "multiple" -> edge?.isMultiple
            "options" -> edge?.options
            "javaClass" -> edge?.javaClass?.name
            "editorKey" -> edge?.editorKey
            "type" -> edge?.type
            "distinct" -> edge?.distinct
            "createVariants" -> edge?.createVariants
            "isAssoc" -> edge?.isAssociation
            else -> null
        }
    }
}
