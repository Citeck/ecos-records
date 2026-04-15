package ru.citeck.ecos.records3.rest.v2.query

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import ru.citeck.ecos.records3.rest.v1.query.QueryBody

@JsonIgnoreProperties(value = ["version"])
open class QueryBodyV2 : QueryBody() {

    open fun getV(): Int {
        return 2
    }
}
