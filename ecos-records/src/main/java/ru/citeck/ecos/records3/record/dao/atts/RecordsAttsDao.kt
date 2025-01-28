package ru.citeck.ecos.records3.record.dao.atts

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordsAttsDao : RecordsDao {

    /**
     * Retrieve attributes associated with specific record identifiers.
     *
     * @param recordIds A list of record identifiers.
     * @returns a list of entities of any type that are converted into the implementation of the AttValue interface
     * using registered implementations of AttValueFactory. The order of results should either match the
     * order of identifiers in recordIds, or each result should have an identifier,
     * which can be used to sort the returned values in RecordsService.
     * If this method return null, then all records will be processed as EmptyAttValue.
     *
     * @see ru.citeck.ecos.records3.record.atts.value.AttValue
     * @see ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
     * @see ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
     */
    @Throws(Exception::class)
    fun getRecordsAtts(recordIds: List<String>): List<*>?
}
