package ru.citeck.ecos.records3.record.atts.value.factory.bean

import org.apache.commons.beanutils.PropertyUtils
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.beans.PropertyDescriptor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

object BeanTypeUtils {

    private val typeCtxCache = ConcurrentHashMap<Class<*>, BeanTypeContext>()

    @JvmStatic
    fun getTypeContext(type: Class<*>): BeanTypeContext {
        return typeCtxCache.computeIfAbsent(type) {
            BeanTypeContext(
                getGetters(it),
                getPropsPath(it),
                getMethodWithStrArg(it, "getAs"),
                getMethodWithStrArg(it, "has"),
                getMethodWithStrArg(it, "getEdge"),
                getMethodWithStrArg(it, "getAtt")
            )
        }
    }

    private inline fun <reified T> getMethodWithStrArg(type: Class<*>, name: String): ((Any, String) -> T)? {

        val getAsMethod: Method = try {
            type.getMethod(name, String::class.java)
        } catch (e: Exception) {
            null
        } ?: return null
        if (!getAsMethod.returnType.kotlin.isSubclassOf(T::class)) {
            return null
        }
        return { value, arg ->
            @Suppress("UNCHECKED_CAST")
            getAsMethod.invoke(value, arg) as T
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

            if (descriptor.name == "class") {
                continue
            }

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
                    val scalar = ScalarType.getBySchemaOrMirrorAtt(attAnnName)
                    if (scalar != null) {
                        getters[scalar.schema] = getter
                    } else {
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
            } else {

                getters[descriptor.name] = getter
            }
        }
        if (!getters.containsKey(ScalarType.DISP.schema)) {
            var dispNameGetter = getters["displayName"]
            if (dispNameGetter == null) {
                dispNameGetter = getters["label"]
                if (dispNameGetter == null) {
                    dispNameGetter = getters["title"]
                    if (dispNameGetter == null) {
                        dispNameGetter = getters["name"]
                    }
                }
            }
            if (dispNameGetter != null) {
                getters[ScalarType.DISP.schema] = dispNameGetter
            }
        }
        if (!getters.containsKey(ScalarType.ID.schema)) {
            val idGetter = getters["id"]
            if (idGetter != null) {
                getters[ScalarType.ID.schema] = idGetter
            }
        }
        if (!getters.containsKey("_type")) {
            val typeGetter = getters["ecosType"]
            if (typeGetter != null) {
                getters["_type"] = typeGetter
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
        annType: Class<T>
    ): T? {

        var annotation: T? = getPrimaryConstructorParamAnnotation(scope, name, annType)
        if (annotation != null) {
            return annotation
        }

        var scopeIt: Class<*>? = scope
        while (scopeIt != null) {
            try {
                val field = scopeIt.getDeclaredField(name)
                annotation = field.getAnnotation(annType)
                break
            } catch (e: Exception) {
                annotation = null
            }
            scopeIt = scopeIt.superclass
        }
        return annotation
    }

    private fun <T : Annotation> getPrimaryConstructorParamAnnotation(
        scope: Class<*>,
        name: String,
        annType: Class<T>
    ): T? {

        var annotation: T? = null

        val primaryConstructor = scope.kotlin.primaryConstructor
        val primaryParam = primaryConstructor?.parameters?.firstOrNull { it.name == name }
        if (primaryParam != null) {
            @Suppress("UNCHECKED_CAST")
            annotation = primaryParam.annotations.firstOrNull { annType.kotlin.isInstance(it) } as? T?
        }

        return annotation
    }
}
