package ru.citeck.ecos.records3.record.atts.value

interface HasListView<T : Any> {

    fun getListView(): List<T>
}
