package ru.citeck.ecos.records3.record.op.atts.service.computed

enum class StoringType {
    NONE,
    /**
     * Evaluate and store attribute
     */
    ON_EMPTY,
    ON_CREATE,
    ON_MUTATE
}
