package ru.citeck.ecos.records3.record.op.atts.service.value.factory.bean

import org.apache.commons.beanutils.PropertyUtils
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import java.beans.PropertyDescriptor
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.primaryConstructor

object BeanTypeUtils {

    private val typeCtxCache = ConcurrentHashMap<Class<*>, BeanTypeContext>()

    @JvmStatic
    fun getTypeContext(type: Class<*>): BeanTypeContext {
        return typeCtxCache.computeIfAbsent(type) {
            BeanTypeContext(getGetters(it))
        }
    }

    private fun getGetters(type: Class<*>): Map<String, (Any) -> Any?> {

        val descriptors: Array<PropertyDescriptor?> = PropertyUtils.getPropertyDescriptors(type)
        val getters = ConcurrentHashMap<String, (Any) -> Any?>()

        for (descriptor in descriptors) {

            val readMethod = descriptor!!.readMethod ?: continue
            readMethod.isAccessible = true

            val getter: (Any) -> Any? = { bean -> readMethod.invoke(bean) }

            getters[descriptor.name] = getter

            var attAnnName: String? = null
            val attName: AttName? = getReadAnnotation(type, descriptor, AttName::class.java)
            if (attName != null) {
                attAnnName = attName.value
            } else {
                val metaAttAnn: MetaAtt? = getReadAnnotation(type, descriptor, MetaAtt::class.java)
                if (metaAttAnn != null) {
                    attAnnName = metaAttAnn.value
                }
            }
            if (attAnnName != null) {
                if (attAnnName[0] == '.' || attAnnName[0] == '?') {
                    val name = attAnnName.substring(1)
                    getters[".$name"] = getter
                    getters["?$name"] = getter
                } else {
                    if (attAnnName.contains("?")) {
                        attAnnName = attAnnName.substring(0, attAnnName.indexOf('?'))
                    }
                }
                getters[attAnnName] = getter
            }
        }
        return getters
    }

    private fun <T : Annotation> getReadAnnotation(
        scope: Class<*>,
        descriptor: PropertyDescriptor,
        type: Class<T>
    ): T? {

        val readMethod = descriptor.readMethod
        var annotation = readMethod.getAnnotation(type)
        if (annotation == null) {
            val primaryConstructor = scope.kotlin.primaryConstructor
            val primaryParam = primaryConstructor?.parameters?.firstOrNull { it.name == descriptor.name }
            if (primaryParam != null) {
                @Suppress("UNCHECKED_CAST")
                annotation = primaryParam.annotations.firstOrNull { type.kotlin.isInstance(it) } as? T?
            }
        }
        if (annotation == null) {
            var scopeIt: Class<*>? = scope
            while (scopeIt != null) {
                try {
                    val field = scopeIt.getDeclaredField(descriptor.name)
                    annotation = field.getAnnotation(type)
                    break
                } catch (e: Exception) {
                    annotation = null
                }
                scopeIt = scopeIt.superclass
            }
        }
        return annotation
    }
}
