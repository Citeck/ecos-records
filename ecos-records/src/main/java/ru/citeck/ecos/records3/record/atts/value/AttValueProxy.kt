package ru.citeck.ecos.records3.record.atts.value

/**
 * Marker interface to indicate that value is not a target value but a proxy for it.
 * It's important for special attributes (_str, _type, etc...)
 * In normal mode attributes resolver for these attributes will
 * call asText, getType on AttValue but for proxy value getAtt will be called.
 */
interface AttValueProxy
