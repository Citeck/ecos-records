package ru.citeck.ecos.records2.test;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.IntNode;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordMetaTest {

    @Test
    void testJson() {

        RecordMeta meta = new RecordMeta();
        meta.setId("someId");
        meta.setAttribute("stringValue", "TestAtt2");
        meta.setAttribute("intValue", IntNode.valueOf(5));
        meta.setAttribute("TestAtt2", true);
        meta.setAttribute("TestAtt3", new Date());

        String json;
        RecordMeta meta2;
        try {
            json = Json.getMapper().toString(meta);
            meta2 = Json.getMapper().read(json, RecordMeta.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertEquals(meta, meta2);

        assertEquals(new BigDecimal(5), meta.get("intValue", new BigDecimal(10)));
        assertEquals(new BigDecimal(10), meta.get("MISSING", new BigDecimal(10)));

        JsonNode metaJson = Json.getMapper().toJson(meta);
        assertEquals(2, metaJson.size());
        assertTrue(metaJson.has("attributes"));
        assertTrue(metaJson.has("id"));

        RecordAtts atts = Json.getMapper().convert(meta, RecordAtts.class);

        RecordMeta meta3 = Json.getMapper().convert(atts, RecordMeta.class);
        assertEquals(meta, meta3);

        JsonNode attsJson = Json.getMapper().toJson(atts);

        assertEquals(2, attsJson.size());
        assertTrue(attsJson.has("attributes"));
        assertTrue(attsJson.has("id"));
    }
}
