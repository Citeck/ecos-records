package ru.citeck.ecos.records3.record.dao.impl.source

data class RecordsSourceFeatures(
    var query: Boolean = false,
    var getAtts: Boolean = false,
    var mutate: Boolean = false,
    var delete: Boolean = false
)
