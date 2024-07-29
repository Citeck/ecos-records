package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory

class NotExistsAttTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsService
        assertThat(records.getAtt("unknown-src@def", "_notExists?bool").asBoolean()).isTrue
    }
}
