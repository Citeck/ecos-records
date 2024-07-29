package ru.citeck.ecos.records3.record.dao.query.dto.query

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class Consistency {
    EVENTUAL,
    TRANSACTIONAL,
    @JsonEnumDefaultValue
    DEFAULT,
    TRANSACTIONAL_IF_POSSIBLE
}
