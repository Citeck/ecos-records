package ru.citeck.ecos.records3.record.op.atts.service.value

interface HasListView<T : Any> {

    fun getListView(): List<T>
}
