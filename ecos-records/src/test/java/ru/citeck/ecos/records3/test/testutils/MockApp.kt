package ru.citeck.ecos.records3.test.testutils

import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.rest.v1.RestHandlerV1

class MockApp(
    val name: String,
    val factory: RecordsServiceFactory,
    val ctxAtts: MutableMap<String, Any?>
) {
    fun getRestHandlerV1(): RestHandlerV1 {
        return factory.restHandlerAdapter.getV1Handler()
    }
}
