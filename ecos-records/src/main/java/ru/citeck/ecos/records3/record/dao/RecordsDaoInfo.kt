package ru.citeck.ecos.records3.record.dao

class RecordsDaoInfo(
    var id: String = "",
    var supportedLanguages: List<String> = emptyList(),
    var columnsSourceId: String? = null,
    var features: Features = Features()
) {
    data class Features(
        var query: Boolean = false,
        var getAtts: Boolean = false,
        var mutate: Boolean = false,
        var delete: Boolean = false
    )
}
