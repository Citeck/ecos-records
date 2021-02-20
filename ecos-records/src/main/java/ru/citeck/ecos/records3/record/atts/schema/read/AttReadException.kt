package ru.citeck.ecos.records3.record.atts.schema.read

class AttReadException(alias: String, attribute: String, message: String) :
    RuntimeException("Attribute read error: $message. Alias: '$alias' Attribute: '$attribute'")
