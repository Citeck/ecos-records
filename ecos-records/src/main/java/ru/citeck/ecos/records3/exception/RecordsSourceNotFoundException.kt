package ru.citeck.ecos.records3.exception

class RecordsSourceNotFoundException(val sourceId: String, val type: Class<*>?) : RecordsException(
    "Source is not found with id '" + sourceId + "' and " +
        "type '" + (if (type != null) type.name else "null") + "'"
)
