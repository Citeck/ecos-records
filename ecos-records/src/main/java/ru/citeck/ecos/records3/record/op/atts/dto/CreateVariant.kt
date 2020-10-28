package ru.citeck.ecos.records3.record.op.atts.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef

@JsonDeserialize(builder = CreateVariant.Builder::class)
class CreateVariant(
    val label: MLText,
    val formRef: RecordRef,
    val recordRef: RecordRef,
    val attributes: ObjectData = ObjectData.create()
) {
    companion object {

        val log = KotlinLogging.logger {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): CreateVariant {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): CreateVariant {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        lateinit var label: MLText
        var formRef: RecordRef = RecordRef.EMPTY
        var recordRef: RecordRef = RecordRef.EMPTY
        var attributes = ObjectData.create()

        constructor(base: CreateVariant) : this() {
            label = MLText.copy(base.label) ?: MLText()
            formRef = base.formRef
            recordRef = base.recordRef
            attributes = ObjectData.deepCopyOrNew(base.attributes)
        }

        fun withLabel(label: MLText) : Builder {
            this.label = label
            return this
        }

        fun withFormRef(formRef: RecordRef) : Builder {
            this.formRef = formRef
            return this
        }

        fun withRecordRef(recordRef: RecordRef) : Builder {
            this.recordRef = recordRef
            return this
        }

        fun withAttributes(attributes: ObjectData) : Builder {
            this.attributes = attributes
            return this
        }

        fun build(): CreateVariant {
            return CreateVariant(label, formRef, recordRef, attributes)
        }
    }
}
