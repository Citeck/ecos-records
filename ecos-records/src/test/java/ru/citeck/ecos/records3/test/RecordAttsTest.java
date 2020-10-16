package ru.citeck.ecos.records3.test;

import ecos.com.fasterxml.jackson210.databind.node.IntNode;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordAttsTest {

    @Test
    void testJson() {

        RecordAtts meta = new RecordAtts();
        meta.setId("someId");
        meta.setAttribute("stringValue", "TestAtt2");
        meta.setAttribute("intValue", IntNode.valueOf(5));
        meta.setAttribute("TestAtt2", true);
        meta.setAttribute("TestAtt3", new Date());

        String json;
        RecordAtts meta2;
        try {
            json = Json.getMapper().toString(meta);
            meta2 = Json.getMapper().read(json, RecordAtts.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertEquals(meta, meta2);

        assertEquals(new BigDecimal(5), meta.get("intValue", new BigDecimal(10)));
        assertEquals(new BigDecimal(10), meta.get("stringValue", new BigDecimal(10)));
        assertEquals(new BigDecimal(10), meta.get("MISSING", new BigDecimal(10)));
    }
}
