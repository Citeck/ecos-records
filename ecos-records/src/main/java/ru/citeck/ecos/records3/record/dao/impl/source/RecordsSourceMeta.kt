package ru.citeck.ecos.records3.record.dao.impl.source

import ru.citeck.ecos.records3.record.dao.impl.source.client.ClientMeta

class RecordsSourceMeta(
    var id: String = "",
    var supportedLanguages: List<String> = emptyList(),
    var columnsSourceId: String? = null,
    var features: RecordsSourceFeatures = RecordsSourceFeatures(),
    var client: ClientMeta? = null
)
