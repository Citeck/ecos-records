package ru.citeck.ecos.records3.test;

import ecos.com.fasterxml.jackson210.databind.node.IntNode;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordAttsTest {

    @Test
    void testJson() {

        RecordAtts meta = new RecordAtts();
        meta.setId("someId");
        meta.setAtt("stringValue", "TestAtt2");
        meta.setAtt("intValue", IntNode.valueOf(5));
        meta.setAtt("TestAtt2", true);
        meta.setAtt("TestAtt3", new Date());

        String json;
        RecordAtts meta2;
        try {
            json = Json.getMapper().toString(meta);
            meta2 = Json.getMapper().read(json, RecordAtts.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertEquals(meta, meta2);

        assertEquals(new BigDecimal(5), meta.getAtt("intValue", new BigDecimal(10)));
        assertEquals(new BigDecimal(10), meta.getAtt("stringValue", new BigDecimal(10)));
        assertEquals(new BigDecimal(10), meta.getAtt("MISSING", new BigDecimal(10)));
    }
}
