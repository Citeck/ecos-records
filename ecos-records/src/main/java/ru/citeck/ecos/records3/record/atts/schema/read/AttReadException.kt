package ru.citeck.ecos.records3.record.atts.schema.read

import ru.citeck.ecos.records3.record.atts.schema.exception.AttSchemaException

class AttReadException(alias: String, attribute: String, message: String) :
    AttSchemaException("Attribute read error: $message. Alias: '$alias' Attribute: '$attribute'")
