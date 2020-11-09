package ru.citeck.ecos.records3.record.op.atts.service.schema.resolver

import ru.citeck.ecos.records2.meta.util.AttStrUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import kotlin.collections.LinkedHashMap

class AttContext {

    companion object {

        private const val CONTEXT_UNDEFINED_MSG = "Current context is null"

        private val current: ThreadLocal<AttContext> = ThreadLocal()

        @JvmStatic
        fun getCurrentSchemaAtt(): SchemaAtt {
            return getCurrentNotNull().schemaAtt
        }

        @JvmStatic
        fun getInnerAttsMap(): Map<String, String> {

            val attContext = getCurrentNotNull()
            val writer = attContext.serviceFactory.attSchemaWriter
            val result = LinkedHashMap<String, String>()

            val schemaAtt = attContext.schemaAtt

            schemaAtt.inner.forEach { att ->
                result[att.name] = writer.write(att)
            }
            return result
        }

        @JvmStatic
        fun getCurrentSchemaAttAsStr(): String {

            val context = getCurrentNotNull()
            val schemaAtt = context.schemaAtt

            return context.serviceFactory.attSchemaWriter.write(schemaAtt)
        }

        @JvmStatic
        fun getCurrentSchemaAttInnerStr(): String {
            val att = getCurrentSchemaAttAsStr()
            return att.trim().substring(AttStrUtils.indexOf(att, "{") + 1, att.length - 1)
        }

        @JvmStatic
        fun getCurrentNotNull(): AttContext {
            return current.get() ?: error(CONTEXT_UNDEFINED_MSG)
        }

        @JvmStatic
        fun getCurrent(): AttContext? {
            return current.get()
        }

        @JvmStatic
        fun <T> doWithCtx(serviceFactory: RecordsServiceFactory, action: (AttContext) -> T): T {

            var isOwner = false
            var ctx = current.get()

            if (ctx == null) {
                ctx = AttContext()
                ctx.serviceFactory = serviceFactory
                current.set(ctx)
                isOwner = true
            }
            return try {
                action.invoke(ctx)
            } finally {
                if (isOwner) {
                    current.set(null)
                }
            }
        }

        private val EMPTY = SchemaAtt.create().withName("?id").build()
    }

    private lateinit var serviceFactory: RecordsServiceFactory
    private var schemaAtt: SchemaAtt = EMPTY

    fun getSchemaAtt(): SchemaAtt {
        return schemaAtt
    }

    fun setSchemaAtt(schemaAtt: SchemaAtt) {
        this.schemaAtt = schemaAtt
    }
}
