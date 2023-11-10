package ru.citeck.ecos.records3.record.mixin.external.remote

interface ExtAttRemoteRegistry {

    /**
     * AppName -> List<ExtAtt>
     */
    fun getRemoteData(): Map<String, List<ExtAttRemoteRegistryData>>

    fun onRemoteDataUpdated(action: () -> Unit)
}
