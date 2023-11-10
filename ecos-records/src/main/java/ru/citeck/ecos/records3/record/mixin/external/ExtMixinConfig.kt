package ru.citeck.ecos.records3.record.mixin.external

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

interface ExtMixinConfig {

    /**
     * ECOS Type which will be used to register external mixin attributes.
     */
    fun setEcosType(typeId: String): ExtMixinConfig

    fun setLocal(local: Boolean): ExtMixinConfig

    /**
     * Add provided attribute.
     */
    fun addProvidedAtt(attribute: String): ProvidedAttConfig

    fun addProvidedAtts(attributes: Collection<String>): ProvidedAttsConfig

    fun addProvidedAtts(vararg attributes: String): ProvidedAttsConfig

    /**
     * Add required attributes for all provided attributes.
     */
    fun addRequiredAtts(requiredAtts: Map<String, *>): ExtMixinConfig

    /**
     * Add required attributes for all provided attributes.
     */
    fun addRequiredAtts(requiredAtts: Class<*>): ExtMixinConfig

    /**
     * Set priority to all provided attributes by default.
     */
    fun setPriority(priority: Float): ExtMixinConfig

    fun withHandler(handler: (String, ObjectData) -> Any?): ExtMixinConfig

    fun withRawHandler(handler: (ExtAttHandlerContext, String, Map<String, Any?>, SchemaAtt) -> Any?): ExtMixinConfig

    fun <T : Any> withHandler(type: Class<T>, handler: (String, T) -> Any?): ExtMixinConfig

    fun reconfigure()

    interface ProvidedAttsConfig {

        fun setLocal(local: Boolean): ProvidedAttsConfig

        fun setEcosType(typeId: String): ProvidedAttsConfig

        fun addRequiredAtts(requiredAtts: Map<String, *>): ProvidedAttsConfig

        fun addRequiredAtts(requiredAtts: Class<*>): ProvidedAttsConfig

        fun setPriority(priority: Float): ProvidedAttsConfig

        fun withRawHandler(handler: (ExtAttHandlerContext, Map<String, Any?>, SchemaAtt) -> Any?): ProvidedAttsConfig

        fun withHandler(handler: (String, ObjectData) -> Any?): ProvidedAttsConfig

        fun <T : Any> withHandler(type: Class<T>, handler: (String, T) -> Any?): ProvidedAttsConfig
    }

    interface ProvidedAttConfig {

        fun setLocal(local: Boolean): ProvidedAttConfig

        fun setEcosType(typeId: String): ProvidedAttConfig

        fun addRequiredAtts(requiredAtts: Map<String, *>): ProvidedAttConfig

        fun addRequiredAtts(requiredAtts: Class<*>): ProvidedAttConfig

        fun setPriority(priority: Float): ProvidedAttConfig

        fun withRawHandler(handler: (ExtAttHandlerContext, Map<String, Any?>, SchemaAtt) -> Any?): ProvidedAttConfig

        fun withHandler(handler: (ObjectData) -> Any?): ProvidedAttConfig

        fun <T : Any> withHandler(type: Class<T>, handler: (T) -> Any?): ProvidedAttConfig
    }
}
