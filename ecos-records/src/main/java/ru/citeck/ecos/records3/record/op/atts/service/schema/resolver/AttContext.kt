package ru.citeck.ecos.records3.record.op.atts.service.schema.resolver

import ru.citeck.ecos.records2.RecordsServiceFactory
import ru.citeck.ecos.records2.meta.util.AttStrUtils
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import java.util.*

class AttContext {

    companion object {

        private const val CONTEXT_UNDEFINED_MSG = "Current context is null"

        private val current: ThreadLocal<AttContext> = ThreadLocal()

        fun getCurrentSchemaAtt() : SchemaAtt {
            return getCurrentNotNull().schemaAtt
        }

        fun getInnerAttsMap() : Map<String, String> {

            val attContext = getCurrentNotNull()
            val writer = attContext.serviceFactory.attSchemaWriter
            val result = HashMap<String, String>()

            val schemaAtt = attContext.schemaAtt

            schemaAtt.inner.forEach { att ->
                result[att.name] = writer.write(att)
            }
            return result
        }

        fun getCurrentSchemaAttAsStr() : String {

            val context = getCurrentNotNull()
            val schemaAtt = context.schemaAtt

            return context.serviceFactory.attSchemaWriter.write(schemaAtt)
        }

        fun getCurrentSchemaAttInnerStr() : String {
            val att = getCurrentSchemaAttAsStr()
            return att.trim().substring(AttStrUtils.indexOf(att, "{") + 1, att.length - 1)
        }

        fun getCurrentNotNull() : AttContext {
            return current.get() ?: error(CONTEXT_UNDEFINED_MSG)
        }
        fun getCurrent(): AttContext? {
            return current.get()
        }

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
    }

    private lateinit var serviceFactory: RecordsServiceFactory
    private lateinit var schemaAtt: SchemaAtt

    fun getSchemaAtt() : SchemaAtt {
        return schemaAtt
    }

    fun setSchemaAtt(schemaAtt: SchemaAtt) {
        this.schemaAtt = schemaAtt
    }
}
