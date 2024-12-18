package ru.citeck.ecos.records3.test.script

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.script.ScriptUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class RecordsJavaScriptTest : AbstractRecordsScriptTest() {

    companion object {
        private val arrakisRef = EntityRef.valueOf("dune@$DEFAULT_ARRAKIS_ID")
    }

    @Test
    fun `query without attributes`() {

        createArrakis(id = "abcdef", name = "abcdef")

        val result0 = """
            return Records.query({
                sourceId: 'dune',
                language: 'predicate',
                query: {t: 'eq', a:'name', v: "abcdef" }
            }).records;
        """.eval()
        assertThat(result0.isArray()).isTrue
        assertThat(result0.size()).isEqualTo(1)
        assertThat(result0[0].asText()).isEqualTo("dune@abcdef")

        val result00 = """
            return Records.queryOne({
                sourceId: 'dune',
                language: 'predicate',
                query: {t: 'eq', a:'name', v: "abcdef" }
            });
        """.eval()
        assertThat(result00.isTextual()).isTrue
        assertThat(result00.asText()).isEqualTo("dune@abcdef")

        val result1 = """
            return Records.query({
                sourceId: 'dune',
                language: 'predicate',
                query: {t: 'eq', a:'name', v: "abcdef" }
            }, null).records;
        """.eval()
        assertThat(result1.isArray()).isTrue
        assertThat(result1.size()).isEqualTo(1)
        assertThat(result1[0].asText()).isEqualTo("dune@abcdef")

        val result11 = """
            return Records.queryOne({
                sourceId: 'dune',
                language: 'predicate',
                query: {t: 'eq', a:'name', v: "abcdef" }
            }, null);
        """.eval()
        assertThat(result11.isTextual()).isTrue
        assertThat(result11.asText()).isEqualTo("dune@abcdef")
    }

    @Test
    fun `create records via script`() {
        val created = createArrakis()
        assertEquals(arrakisRef.toString(), created.asText())
    }

    @Test
    fun `create records via script service and get meta via records`() {
        createArrakis()

        assertEquals(DEFAULT_ARRAKIS_NAME, arrakisRef.getAtt("name").asText())
        assertEquals(DEFAULT_ARRAKIS_TEMP, arrakisRef.getAtt("temp").asDouble())
    }

    @Test
    fun `create records via script service and get meta via script`() {
        val createdMeta = """
            var saved = Records.get("dune@");

            saved.att("id", "$DEFAULT_ARRAKIS_ID");
            saved.att("name", "$DEFAULT_ARRAKIS_NAME");
            saved.att("temp", $DEFAULT_ARRAKIS_TEMP);

            saved = saved.save();

            return {
                "name": saved.load("name"),
                "temp": saved.load("temp?num")
            }
        """.trimIndent().eval()

        assertEquals(DEFAULT_ARRAKIS_NAME, createdMeta["name"].asText())
        assertEquals(DEFAULT_ARRAKIS_TEMP, createdMeta["temp"].asDouble())
    }

    @Test
    fun `modify created record via script`() {
        createArrakis()

        """
            var arrakis = Records.get("$arrakisRef");

            arrakis.att("temp", 45.0);
            arrakis.save();
        """.trimIndent().eval()

        assertEquals(45.0, arrakisRef.getAtt("temp").asDouble())
    }

    @Test
    fun `multiple modify created record via script`() {
        val multipleModifyResult = """
            var arrakis = Records.get("dune@");

            arrakis.att("id", "$DEFAULT_ARRAKIS_ID");
            arrakis.att("name", "$DEFAULT_ARRAKIS_NAME");
            arrakis.att("temp", $DEFAULT_ARRAKIS_TEMP);

            arrakis = arrakis.save();

            arrakis.att("temp", 45.0);
            var savedOne = arrakis.save();
            var savedOneTemp = arrakis.load("temp?num");

            savedOne.att("temp", 30.0);
            var savedTwo = savedOne.save();
            var savedTwoTemp = arrakis.load("temp?num");

            savedTwo.att("temp", 43.5);
            savedTwo.save();
            var savedThreeTemp = arrakis.load("temp?num");

            return {
                "savedOneTemp": savedOneTemp,
                "savedTwoTemp": savedTwoTemp,
                "savedThreeTemp": savedThreeTemp
            }

        """.trimIndent().eval()

        assertEquals(45.0, multipleModifyResult["savedOneTemp"].asDouble())
        assertEquals(30.0, multipleModifyResult["savedTwoTemp"].asDouble())
        assertEquals(43.5, multipleModifyResult["savedThreeTemp"].asDouble())
    }

    @Test
    fun `ressetted script before save should keep its previous state`() {
        createArrakis()

        """
            var arrakis = Records.get("$arrakisRef")

            arrakis.att("temp", 25);
            arrakis.reset();
            arrakis.save()
        """.trimIndent().eval()

        assertEquals(DEFAULT_ARRAKIS_TEMP, arrakisRef.getAtt("temp?num").asDouble())
    }

    @Test
    fun `get meta via script service from existing record`() {
        val spiceMeta = """
            var spice = Records.get("$duneSpice");
            return {
                "intensity": spice.load("intensity?num"),
                "weight": spice.load("weight?num")
            }
        """.trimIndent().eval()

        assertEquals(DEFAULT_SPICE_INTENSITY, spiceMeta["intensity"].asDouble())
        assertEquals(DEFAULT_SPICE_WEIGHT, spiceMeta["weight"].asDouble())
    }

    @Test
    fun `query equals via script`() {
        val queryResult = """
            var result = Records.query(
                {
                    sourceId: "dune",
                    language: "predicate",
                    query: {
                        t: "eq",
                        att: "intensity?num",
                        val: 15.0
                    }
                },
                ["id", "intensity"]
            )

            return {
                "size": result["records"].length,
                "foundSpiceIntensity": result["records"][0]["intensity"]
            }
        """.trimIndent().eval()

        assertEquals(1, queryResult["size"].asInt())
        assertEquals(15.0, queryResult["foundSpiceIntensity"].asDouble())
    }

    @Test
    fun `query greater then, via script`() {
        val queryResult = """
            var result = Records.query(
                {
                    sourceId: "dune",
                    language: "predicate",
                    query: {
                        t: "gt",
                        att: "intensity?num",
                        val: 20.0
                    }
                },
                ["id", "intensity"]
            )

            return {
                "size": result["records"].length
            }
        """.trimIndent().eval()

        assertEquals(3, queryResult["size"].asInt())
    }

    @Test
    fun `query less or equals, via script`() {
        val queryResult = """
            var result = Records.query(
                {
                    sourceId: "dune",
                    language: "predicate",
                    query: {
                        t: "le",
                        att: "intensity?num",
                        val: 15.0
                    }
                },
                ["id", "intensity"]
            )

            return {
                "size": result["records"].length
            }
        """.trimIndent().eval()

        assertEquals(2, queryResult["size"].asInt())
    }

    @Test
    fun `query one test`() {
        createArrakis(name = "custom-name")

        val queryResult0 = """
            return Records.queryOne(
                {
                    sourceId: "dune",
                    language: "predicate",
                    query: {t: "eq", a: "name", v: "custom-name"}
                },
                ["name"]
            )
        """.eval()

        assertThat(queryResult0.isObject()).isTrue()
        assertThat(queryResult0["name"].asText()).isEqualTo("custom-name")
        assertThat(queryResult0["id"].asText()).isNotBlank()

        val queryResult1 = """
            return Records.queryOne(
                {
                    sourceId: "dune",
                    language: "predicate",
                    query: {t: "eq", a: "name", v: "custom-name"}
                },
                "name"
            )
        """.eval()

        assertThat(queryResult1.isTextual()).isTrue()
        assertThat(queryResult1.asText()).isEqualTo("custom-name")
    }

    private fun createArrakis(id: String = DEFAULT_ARRAKIS_ID, name: String = DEFAULT_ARRAKIS_NAME): DataValue {
        val script = """
            var arrakis = Records.get("dune@");

            arrakis.att("id", "$id");
            arrakis.att("name", "$name");
            arrakis.att("temp", $DEFAULT_ARRAKIS_TEMP);

            return arrakis.save().getRef();
        """.trimIndent()

        return script.eval()
    }

    private fun EntityRef.getAtt(att: String): DataValue {
        return recordsService.getAtt(this, att)
    }

    private fun String.eval(): DataValue {
        val scriptModel = mapOf(
            Pair("Records", rScript)
        )

        return ScriptUtils.execute(this, scriptModel)
    }
}
