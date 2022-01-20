package ru.citeck.ecos.records3.rest.v2.query

import ecos.com.fasterxml.jackson210.annotation.JsonIgnoreProperties
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.rest.v1.query.QueryBody

@JsonIgnoreProperties(value = ["version"])
open class QueryBodyV2 : QueryBody() {

    override fun setQuery(query: RecordsQuery?) {
        setQueryWithoutProcessing(query)
    }

    open fun getV(): Int {
        return 2
    }
}
