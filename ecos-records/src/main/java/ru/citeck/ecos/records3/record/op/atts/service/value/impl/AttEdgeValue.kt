package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import java.util.*

class AttEdgeValue(val edge: AttEdge?) : AttValue {

    override fun getAtt(name: String): Any? {
        return when (name) {
            "name" -> edge?.getName()
            "val" -> edge?.getValue()
            "vals" -> {
                val value: Any = edge?.getValue() ?: emptyList<Any?>()
                if (value is Collection<*>) {
                    ArrayList(value)
                } else {
                    listOf(value)
                }
            }
            "title" -> edge?.getTitle()
            "description" -> edge?.getDescription()
            "protected" -> edge?.isProtected()
            "canBeRead" -> edge?.canBeRead()
            "multiple" -> edge?.isMultiple()
            "options" -> edge?.getOptions()
            "javaClass" -> edge?.getJavaClass()?.name
            "editorKey" -> edge?.getEditorKey()
            "type" -> edge?.getType()
            "distinct" -> edge?.getDistinct()
            "createVariants" -> edge?.getCreateVariants()
            "isAssoc" -> edge?.isAssociation()
            else -> null
        }
    }
}
