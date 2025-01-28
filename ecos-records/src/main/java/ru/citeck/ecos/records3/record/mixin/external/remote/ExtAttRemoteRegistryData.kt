package ru.citeck.ecos.records3.record.mixin.external.remote

data class ExtAttRemoteRegistryData(
    val typeId: String,
    val priority: Float,
    val attribute: String,
    val requiredAtts: Set<String>
)
