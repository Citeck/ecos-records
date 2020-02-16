package ru.citeck.ecos.records.test;

import ecos.com.fasterxml.jackson210.databind.node.IntNode;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.utils.JsonUtils;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            json = JsonUtils.toString(meta);
            meta2 = JsonUtils.read(json, RecordMeta.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertEquals(meta, meta2);

        assertEquals(new BigDecimal(5), meta.get("intValue", new BigDecimal(10)));
        assertEquals(new BigDecimal(10), meta.get("stringValue", new BigDecimal(10)));
        assertEquals(new BigDecimal(10), meta.get("MISSING", new BigDecimal(10)));
    }
}
