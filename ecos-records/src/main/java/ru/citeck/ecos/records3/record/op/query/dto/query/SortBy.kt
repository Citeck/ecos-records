package ru.citeck.ecos.records3.record.op.query.dto.query

data class SortBy(
    val attribute: String,
    val isAscending: Boolean = false
)
