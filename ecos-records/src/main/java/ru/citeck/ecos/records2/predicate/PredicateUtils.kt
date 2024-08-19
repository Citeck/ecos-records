package ru.citeck.ecos.records2.predicate

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.citeck.beans.BeanUtils
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.commons.utils.TmplUtils.applyAtts
import ru.citeck.ecos.commons.utils.TmplUtils.getAtts
import ru.citeck.ecos.records2.predicate.model.*
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.function.Function

object PredicateUtils {

    @JvmStatic
    fun getAllPredicateAttributes(predicate: Predicate?): List<String> {
        val result: MutableList<String> = ArrayList()
        mapAttributePredicatesImpl(
            predicate,
            { v: AttributePredicate ->
                result.add(v.getAttribute())
                if (v is ValuePredicate) {
                    val value = v.getValue()
                    if (value.isTextual()) {
                        result.addAll(getAtts(value.asText()))
                    }
                }
                v
            },
            false
        )
        return result
    }

    @JvmStatic
    fun resolvePredicateWithAttributes(predicate: Predicate, attributes: ObjectData): Predicate {
        val result = predicate.copy<Predicate>()
        mapValuePredicatesImpl(
            result,
            { v: ValuePredicate ->
                v.setValue(applyAtts(v.getValue(), attributes))
                v
            },
            false
        )
        return result
    }

    @JvmStatic
    fun filterValuePredicates(
        predicate: Predicate,
        filter: Function<ValuePredicate, Boolean>
    ): Optional<Predicate> {
        val res = mapValuePredicatesImpl(
            predicate,
            { pred: ValuePredicate -> if (filter.apply(pred)) pred else null },
            false
        )
        return Optional.ofNullable(res)
    }

    @JvmStatic
    fun filterAttributePredicates(
        predicate: Predicate?,
        filter: Function<AttributePredicate, Boolean>
    ): Optional<Predicate> {
        val res = mapAttributePredicatesImpl(
            predicate,
            { pred: AttributePredicate -> if (filter.apply(pred)) pred else null },
            false
        )
        return Optional.ofNullable(res)
    }

    @JvmStatic
    fun mapValuePredicates(
        predicate: Predicate?,
        mapFunc: Function<ValuePredicate, Predicate?>
    ): Predicate? {
        return mapValuePredicatesImpl(predicate, mapFunc, false)
    }

    @JvmStatic
    fun mapAttributePredicates(
        predicate: Predicate?,
        mapFunc: Function<AttributePredicate, Predicate?>
    ): Predicate? {
        return mapAttributePredicatesImpl(predicate, mapFunc, false)
    }

    @JvmStatic
    @JvmOverloads
    fun mapValuePredicates(
        predicate: Predicate?,
        mapFunc: Function<ValuePredicate, Predicate?>,
        onlyAnd: Boolean,
        optimize: Boolean = true,
        filterEmptyComposite: Boolean = true
    ): Predicate? {
        return mapValuePredicatesImpl(predicate, mapFunc, onlyAnd, optimize, filterEmptyComposite)
    }

    @JvmStatic
    @JvmOverloads
    fun mapAttributePredicates(
        predicate: Predicate?,
        mapFunc: Function<AttributePredicate, Predicate?>,
        onlyAnd: Boolean,
        optimize: Boolean = true,
        filterEmptyComposite: Boolean = true
    ): Predicate? {
        return mapAttributePredicatesImpl(predicate, mapFunc, onlyAnd, optimize, filterEmptyComposite)
    }

    @JvmStatic
    fun <T : Any> convertToDto(predicate: Predicate, type: Class<T>): T {
        return convertToDto(predicate, type, false)
    }

    @JvmStatic
    fun <T : Any> convertToDto(predicate: Predicate, type: Class<T>, onlyAnd: Boolean): T {
        val dto: T = type.newInstance()
        return convertToDto(predicate, dto, onlyAnd)
    }

    @JvmStatic
    fun <T : Any> convertToDto(predicate: Predicate, dto: T): T {
        return convertToDto(predicate, dto, false)
    }

    @JvmStatic
    fun <T : Any> convertToDto(predicate: Predicate, dto: T, onlyAnd: Boolean): T {
        val dtoFields: MutableSet<String> = HashSet()
        for (descriptor in BeanUtils.getProperties(dto::class)) {
            if (descriptor.getPropClass() != Predicate::class.java) {
                dtoFields.add(descriptor.getName())
            }
        }
        val dtoData: MutableMap<String, Any> = HashMap()
        val filtered = mapValuePredicates(
            predicate,
            { pred: ValuePredicate ->
                val att = pred.getAttribute()
                if (dtoFields.contains(att)) {
                    val currentData = dtoData[att]
                    if (currentData == null || "" == currentData) {
                        dtoData[att] = pred.getValue()
                    }
                    return@mapValuePredicates null
                }
                pred
            },
            onlyAnd
        )
        val dtoDataNode = mapper.toJson(dtoData) as ObjectNode
        mapper.applyData(dto, dtoDataNode)
        try {
            BeanUtils.setProperty(dto, "predicate", filtered)
        } catch (e: IllegalAccessException) {
            return dto
        } catch (e: InvocationTargetException) {
            return dto
        } catch (e: NoSuchMethodException) {
            return dto
        } catch (e: RuntimeException) {
            return dto
        }
        return dto
    }

    private fun mapValuePredicatesImpl(
        predicate: Predicate?,
        mapFunc: Function<ValuePredicate, Predicate?>,
        onlyAnd: Boolean,
        optimize: Boolean = true,
        filterEmptyComposite: Boolean = true
    ): Predicate? {
        return mapAttributePredicatesImpl(
            predicate,
            { pred: AttributePredicate? ->
                if (pred is ValuePredicate) {
                    return@mapAttributePredicatesImpl mapFunc.apply(pred)
                }
                pred
            },
            onlyAnd,
            optimize,
            filterEmptyComposite
        )
    }

    private fun mapAttributePredicatesImpl(
        predicate: Predicate?,
        mapFunc: Function<AttributePredicate, Predicate?>,
        onlyAnd: Boolean,
        optimize: Boolean = true,
        filterEmptyComposite: Boolean = true
    ): Predicate? {
        if (predicate == null) {
            return null
        }
        return if (predicate is AttributePredicate) {
            mapFunc.apply(predicate)
        } else if (predicate is ComposedPredicate) {
            val isAnd = predicate is AndPredicate
            if (onlyAnd && !isAnd) {
                return predicate
            }
            val mappedPredicates: MutableList<Predicate> = ArrayList()
            for (pred in predicate.getPredicates()) {
                val mappedPred = mapAttributePredicatesImpl(pred, mapFunc, onlyAnd, optimize)
                if (mappedPred != null) {
                    if (optimize) {
                        if (isAlwaysTrue(mappedPred)) {
                            if (isAnd) {
                                continue
                            } else {
                                return mappedPred
                            }
                        } else if (isAlwaysFalse(mappedPred)) {
                            if (isAnd) {
                                return mappedPred
                            } else {
                                continue
                            }
                        }
                    }
                    mappedPredicates.add(mappedPred)
                }
            }
            if (filterEmptyComposite && mappedPredicates.isEmpty()) {
                return null
            }
            if (optimize) {
                if (mappedPredicates.isEmpty()) {
                    return if (isAnd) {
                        Predicates.alwaysTrue()
                    } else {
                        Predicates.alwaysFalse()
                    }
                } else if (mappedPredicates.size == 1) {
                    return mappedPredicates[0]
                }
            }
            when (predicate) {
                is AndPredicate -> Predicates.and(mappedPredicates)
                is OrPredicate -> Predicates.or(mappedPredicates)
                else -> null
            }
        } else if (predicate is NotPredicate) {
            val mapped = mapAttributePredicatesImpl(predicate.getPredicate(), mapFunc, onlyAnd, optimize)
            if (mapped != null) {
                if (optimize && mapped is NotPredicate) {
                    mapped.getPredicate()
                } else {
                    Predicates.not(mapped)
                }
            } else {
                null
            }
        } else {
            predicate
        }
    }

    @JvmStatic
    fun optimize(predicate: Predicate): Predicate {

        var result = predicate

        if (predicate is ComposedPredicate) {

            val comp: ComposedPredicate = predicate.copy()
            val predicates = comp.getPredicates()
            result = comp

            val isAnd = predicate is AndPredicate
            if (predicates.isEmpty()) {
                return if (isAnd) {
                    Predicates.alwaysTrue()
                } else {
                    Predicates.alwaysFalse()
                }
            }
            if (predicates.size == 1) {

                result = optimize(predicates[0])
            } else {
                comp.setPredicates(null)

                for (child in predicates) {
                    val optRes = optimize(child)
                    if (isAlwaysTrue(optRes)) {
                        if (isAnd) {
                            continue
                        } else {
                            return optRes
                        }
                    } else if (isAlwaysFalse(optRes)) {
                        if (isAnd) {
                            return optRes
                        } else {
                            continue
                        }
                    }
                    comp.addPredicate(optRes)
                }
                if (comp.getPredicates().isEmpty()) {
                    result = if (isAnd) {
                        Predicates.alwaysTrue()
                    } else {
                        Predicates.alwaysFalse()
                    }
                } else if (comp.getPredicates().size == 1) {
                    result = comp.getPredicates()[0]
                }
            }
        } else if (predicate is NotPredicate) {

            val innerPred = predicate.getPredicate()
            if (innerPred is NotPredicate) {
                return optimize(innerPred.getPredicate())
            }
            val optimizedInner = optimize(innerPred)
            if (optimizedInner === innerPred) {
                return predicate
            }
            result = Predicates.not(optimizedInner)
        }
        return result
    }

    @JvmStatic
    fun isAlwaysTrue(predicate: Predicate): Boolean {
        return predicate is VoidPredicate
    }

    @JvmStatic
    fun isAlwaysFalse(predicate: Predicate): Boolean {
        return predicate is NotPredicate && isAlwaysTrue(predicate.getPredicate())
    }
}
