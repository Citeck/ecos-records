package ru.citeck.ecos.records3.record.dao.query.dto.query

data class SortBy(
    val attribute: String,
    val isAscending: Boolean = false
)
