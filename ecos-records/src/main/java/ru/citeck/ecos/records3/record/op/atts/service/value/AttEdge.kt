package ru.citeck.ecos.records3.record.op.atts.service.value

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.record.op.atts.dto.CreateVariant

interface AttEdge {

    @Throws(Exception::class)
    fun isProtected(): Boolean = false

    @Throws(Exception::class)
    fun isMultiple(): Boolean = true

    @Throws(Exception::class)
    fun isAssociation(): Boolean = false

    @Throws(Exception::class)
    fun isSearchable(): Boolean = true

    /**
     * Can client read value of this attribute or not.
     */
    @Throws(Exception::class)
    fun canBeRead(): Boolean = true

    @Throws(Exception::class)
    fun getOptions(): List<*>? = null

    @Throws(Exception::class)
    fun getDistinct(): List<*>? = null

    @Throws(Exception::class)
    fun getCreateVariants(): List<CreateVariant>? = null

    @Throws(Exception::class)
    fun getJavaClass(): Class<*>? = null

    @Throws(Exception::class)
    fun getEditorKey(): String? = null

    /**
     * Type of attribute.
     */
    @Throws(Exception::class)
    fun getType(): String? = null

    @Throws(Exception::class)
    fun getTitle(): MLText? = null

    @Throws(Exception::class)
    fun getDescription(): MLText? = null

    @Throws(Exception::class)
    fun getName(): String? = null

    @Throws(Exception::class)
    fun getValue(): Any? = null
}
