package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordMeta;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordMetaTest {

    @Test
    void testJson() {

        ObjectMapper mapper = new ObjectMapper();

        RecordMeta meta = new RecordMeta();
        meta.setId("someId");
        meta.setAttribute("TestAtt", "TestAtt2");
        meta.setAttribute("TestAtt1", IntNode.valueOf(5));
        meta.setAttribute("TestAtt2", true);
        meta.setAttribute("TestAtt3", new Date());

        String json;
        RecordMeta meta2;
        try {
            json = mapper.writeValueAsString(meta);
            meta2 = mapper.readValue(json, RecordMeta.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertEquals(meta, meta2);
    }
}
