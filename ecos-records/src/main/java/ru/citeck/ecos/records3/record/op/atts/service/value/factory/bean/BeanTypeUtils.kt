package ru.citeck.ecos.records3.record.op.atts.service.value.factory.bean

import org.apache.commons.beanutils.PropertyUtils
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import java.beans.PropertyDescriptor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.primaryConstructor

object BeanTypeUtils {

    private val typeCtxCache = ConcurrentHashMap<Class<*>, BeanTypeContext>()

    @JvmStatic
    fun getTypeContext(type: Class<*>): BeanTypeContext {
        return typeCtxCache.computeIfAbsent(type) {
            BeanTypeContext(getGetters(it), getPropsPath(it))
        }
    }

    private fun getPropsPath(type: Class<*>): Map<String, String> {

        val descriptors = PropertyUtils.getPropertyDescriptors(type)
        val paths = HashMap<String, String>()

        for (descriptor in descriptors) {
            val attName = getAnnotatedWriteAttName(type, descriptor)
            if (attName == "...") {
                getWriteAttNames(descriptor.propertyType).forEach { paths[it] = descriptor.name }
            }
        }

        return paths
    }

    private fun getWriteAttNames(type: Class<*>): List<String> {

        val descriptors = PropertyUtils.getPropertyDescriptors(type)
        val names = ArrayList<String>()

        for (descriptor in descriptors) {
            descriptor.writeMethod ?: continue
            names.add(getAnnotatedWriteAttName(type, descriptor) ?: descriptor.name)
        }
        return names
    }

    private fun getGetters(type: Class<*>): Map<String, (Any) -> Any?> {

        val descriptors: Array<PropertyDescriptor> = PropertyUtils.getPropertyDescriptors(type)
        val getters = HashMap<String, (Any) -> Any?>()

        for (descriptor in descriptors) {

            val readMethod = descriptor.readMethod ?: continue
            readMethod.isAccessible = true

            val getter: (Any) -> Any? = { bean -> readMethod.invoke(bean) }

            var attAnnName: String? = getAnnotatedReadAttName(type, descriptor)

            if (attAnnName != null) {

                if (attAnnName == "...") {

                    getGetters(descriptor.propertyType).forEach { (k, innerGetter) ->
                        if (!getters.containsKey(k)) {
                            getters[k] = {
                                val innerValue = getter.invoke(it)
                                if (innerValue != null) {
                                    innerGetter.invoke(innerValue)
                                } else {
                                    null
                                }
                            }
                        }
                    }
                } else {

                    if (attAnnName == ".type" || attAnnName == "?type") {
                        attAnnName = "_type"
                    }
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
            } else {

                getters[descriptor.name] = getter
            }
        }
        return getters
    }

    private fun getAnnotatedReadAttName(scope: Class<*>, descriptor: PropertyDescriptor): String? {
        val method = descriptor.readMethod ?: return null
        val attName: AttName? = getAnnotation(scope, method, descriptor.name, AttName::class.java)
        return attName?.value ?: getAnnotation(scope, method, descriptor.name, MetaAtt::class.java)?.value
    }

    private fun getAnnotatedWriteAttName(scope: Class<*>, descriptor: PropertyDescriptor): String? {
        val method = descriptor.writeMethod ?: return null
        val attName: AttName? = getAnnotation(scope, method, descriptor.name, AttName::class.java)
        return attName?.value ?: getAnnotation(scope, method, descriptor.name, MetaAtt::class.java)?.value
    }

    private fun <T : Annotation> getAnnotation(
        scope: Class<*>,
        method: Method?,
        name: String,
        type: Class<T>
    ): T? {
        return method?.getAnnotation(type) ?: getFieldAnnotation(scope, name, type)
    }

    private fun <T : Annotation> getFieldAnnotation(
        scope: Class<*>,
        name: String,
        type: Class<T>
    ): T? {
        var annotation: T? = null

        val primaryConstructor = scope.kotlin.primaryConstructor
        val primaryParam = primaryConstructor?.parameters?.firstOrNull { it.name == name }
        if (primaryParam != null) {
            @Suppress("UNCHECKED_CAST")
            annotation = primaryParam.annotations.firstOrNull { type.kotlin.isInstance(it) } as? T?
            if (annotation != null) {
                return annotation
            }
        }

        var scopeIt: Class<*>? = scope
        while (scopeIt != null) {
            try {
                val field = scopeIt.getDeclaredField(name)
                annotation = field.getAnnotation(type)
                break
            } catch (e: Exception) {
                annotation = null
            }
            scopeIt = scopeIt.superclass
        }
        return annotation
    }
}
