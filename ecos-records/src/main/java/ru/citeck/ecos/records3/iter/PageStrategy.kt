package ru.citeck.ecos.records3.iter

enum class PageStrategy {

    /**
     * Iterate by sortBy property in RecordsQuery page.
     * If sortBy not defined then '_created' attribute will be used.
     */
    SORT_BY,

    /**
     * Iterate by RecordRef's
     * First query will be sent with afterId = RecordRef.EMPTY
     * If we got [ref0, ref1, ref2] then next query will be with afterId = ref2
     **/
    AFTER_ID
}
