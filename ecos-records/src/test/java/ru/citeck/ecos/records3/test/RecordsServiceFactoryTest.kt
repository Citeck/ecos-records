package ru.citeck.ecos.records3.test

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcessor

class RecordsServiceFactoryTest {

    @Test
    fun checkThatAllAttProcessorsHasUniqueType() {

        val factory = object : RecordsServiceFactory() {
            fun getAttProcessorsPub(): List<AttProcessor> {
                return getAttProcessors()
            }
        }
        val existingProcessors = HashSet<String>()
        factory.getAttProcessorsPub().forEach {
            assertTrue(existingProcessors.add(it.getType()))
        }
    }
}
