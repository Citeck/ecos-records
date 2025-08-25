package ru.citeck.ecos.records3.test.script

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.citeck.ecos.commons.utils.script.ScriptUtils
import ru.citeck.ecos.records3.record.atts.computed.script.AttValueScriptCtx

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class RecordsScriptServiceTest : AbstractRecordsScriptTest() {

    @Test
    fun `create records via script service`() {
        val saved = createArrakis()
        assertTrue(saved.getId().isNotBlank())
    }

    @Test
    fun `create records via script service and get meta via records`() {
        val saved = createArrakis()

        val name = recordsService.getAtt(saved.getRef(), "name").asText()
        val temp = recordsService.getAtt(saved.getRef(), "temp").asDouble()

        assertEquals(DEFAULT_ARRAKIS_NAME, name)
        assertEquals(DEFAULT_ARRAKIS_TEMP, temp)
    }

    @Test
    fun `create records via script service and get meta via script`() {
        val saved = createArrakis()

        val name = ScriptUtils.convertToJava(saved.load("name"))
        val temp = ScriptUtils.convertToJava(saved.load("temp?num"))

        assertEquals(DEFAULT_ARRAKIS_NAME, name)
        assertEquals(DEFAULT_ARRAKIS_TEMP, temp)
    }

    @Test
    fun `modify created record via script service`() {
        val saved = createArrakis()

        saved.att("temp", 45.0)
        saved.save()

        assertEquals(45.0, ScriptUtils.convertToJava(saved.load("temp?num")))
    }

    @Test
    fun `multiple modify created record via script service`() {
        val arrakis = createArrakis()

        arrakis.att("temp", 45.0)
        val savedOne = arrakis.save()

        assertEquals(45.0, ScriptUtils.convertToJava(arrakis.load("temp?num")))

        savedOne.att("temp", 30.0)
        val savedTwo = savedOne.save()

        assertEquals(30.0, ScriptUtils.convertToJava(arrakis.load("temp?num")))

        savedTwo.att("temp", 43.5)
        savedTwo.save()

        assertEquals(43.5, ScriptUtils.convertToJava(arrakis.load("temp?num")))
    }

    @Test
    fun `ressetted script before save should keep its previous state`() {
        val arrakis = createArrakis()

        arrakis.att("temp", 25)
        arrakis.reset()
        arrakis.save()

        assertEquals(DEFAULT_ARRAKIS_TEMP, ScriptUtils.convertToJava(arrakis.load("temp?num")))
    }

    @Test
    fun `get meta via script service from existing record`() {
        val spice = rScript.get(duneSpice)

        val intensity = ScriptUtils.convertToJava(spice.load("intensity?num"))
        val weight = ScriptUtils.convertToJava(spice.load("weight?num"))

        assertEquals(DEFAULT_SPICE_INTENSITY, intensity)
        assertEquals(DEFAULT_SPICE_WEIGHT, weight)
    }

    private fun createArrakis(): AttValueScriptCtx {
        val arrakis = rScript.get("dune@")

        arrakis.att("name", DEFAULT_ARRAKIS_NAME)
        arrakis.att("temp", DEFAULT_ARRAKIS_TEMP)

        return arrakis.save()
    }
}
