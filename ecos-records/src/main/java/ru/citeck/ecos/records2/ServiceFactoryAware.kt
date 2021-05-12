package ru.citeck.ecos.records2

import ru.citeck.ecos.records3.RecordsServiceFactory

interface ServiceFactoryAware {

    fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory)
}
