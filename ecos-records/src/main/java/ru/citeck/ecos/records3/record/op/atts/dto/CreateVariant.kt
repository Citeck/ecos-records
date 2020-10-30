package ru.citeck.ecos.records3.record.op.atts.dto

import com.fasterxml.jackson.annotation.JsonSetter as JackJsonSetter
import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef

@JsonDeserialize(builder = CreateVariant.Builder::class)
@JackJsonDeserialize(builder = CreateVariant.Builder::class)
data class CreateVariant(
    val label: MLText?,
    val formKey: String?,
    val formRef: RecordRef?,
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

        var label: MLText? = null
        var formKey: String? = null
        var formRef: RecordRef? = null
        var recordRef: RecordRef = RecordRef.EMPTY
        var attributes = ObjectData.create()

        constructor(base: CreateVariant) : this() {
            label = MLText.copy(base.label)
            formKey = base.formKey
            formRef = base.formRef
            recordRef = base.recordRef
            attributes = ObjectData.deepCopyOrNew(base.attributes)
        }

        fun withFormKey(formKey: String?) : Builder {
            this.formKey = formKey
            return this
        }

        @JsonSetter
        @JackJsonSetter
        fun withLabel(label: MLText?) : Builder {
            this.label = label
            return this
        }

        fun withLabel(label: String?) : Builder {
            if (label != null) {
                this.label = MLText(label)
            } else {
                this.label = null
            }
            return this
        }

        fun withFormRef(formRef: RecordRef?) : Builder {
            this.formRef = formRef
            return this
        }

        fun withRecordRef(recordRef: RecordRef?) : Builder {
            this.recordRef = recordRef ?: RecordRef.EMPTY
            return this
        }

        fun withAttribute(key: String, value: Any?) : Builder {
            this.attributes.set(key, value)
            return this
        }

        fun withAttributes(attributes: ObjectData) : Builder {
            this.attributes = attributes
            return this
        }

        fun build(): CreateVariant {
            return CreateVariant(label, formKey, formRef, recordRef, attributes)
        }
    }
}
