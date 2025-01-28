package ru.citeck.ecos.records3.record.dao.query.dto.query

import ecos.com.fasterxml.jackson210.annotation.JsonEnumDefaultValue

enum class Consistency {
    EVENTUAL,
    TRANSACTIONAL,
    @JsonEnumDefaultValue
    DEFAULT,
    TRANSACTIONAL_IF_POSSIBLE
}
