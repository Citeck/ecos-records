package ru.citeck.ecos.records3.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.query.SortBy;
import ru.citeck.ecos.records3.record.dao.query.dto.query.Consistency;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonTest {

    @Test
    public void test() throws IOException {

        new RecordsServiceFactory();

        RecordsQuery query = RecordsQuery.create()
            .withMaxItems(10)
            .withSourceId("sourceId")
            .withLanguage("language")
            .withGroupBy(Arrays.asList("one", "two"))
            .withAfterId(RecordRef.valueOf("aaa/bbb@123"))
            .withSkipCount(55)
            .withQuery(new SomeQuery())
            .withConsistency(Consistency.TRANSACTIONAL)
            .withSortBy(Arrays.asList(
                new SortBy("att", true),
                new SortBy("att2", true))
            ).build();

        ObjectMapper mapper = new ObjectMapper();

        String mJsonString = mapper.writeValueAsString(query);
        String eJsonString = Json.getMapper().toString(query);

        RecordsQuery res0 = Json.getMapper().read(mJsonString, RecordsQuery.class);
        RecordsQuery res1 = mapper.readValue(mJsonString, RecordsQuery.class);
        RecordsQuery res2 = mapper.readValue(eJsonString, RecordsQuery.class);
        RecordsQuery res3 = mapper.readValue(eJsonString, RecordsQuery.class);

        assertEquals(query, res0);
        assertEquals(query, res1);
        assertEquals(query, res2);
        assertEquals(query, res3);

        ObjectData atts = ObjectData.create();
        atts.set("abc", 2134124);
        atts.set("eeeeee", "adasd");
        atts.set("double", 24.23);
        atts.set("complex", new SomeQuery());
        atts.set("bool", true);

        String mAttsJsonString = mapper.writeValueAsString(atts);
        String eAttsJsonString = Json.getMapper().toString(atts);

        ObjectData attsRes0 = mapper.readValue(mAttsJsonString, ObjectData.class);
        ObjectData attsRes1 = mapper.readValue(eAttsJsonString, ObjectData.class);
        ObjectData attsRes2 = Json.getMapper().read(mAttsJsonString, ObjectData.class);
        ObjectData attsRes3 = Json.getMapper().read(eAttsJsonString, ObjectData.class);

        assertEquals(atts, attsRes0);
        assertEquals(atts, attsRes1);
        assertEquals(atts, attsRes2);
        assertEquals(atts, attsRes3);

        com.fasterxml.jackson.databind.node.ObjectNode mNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        mNode.put("test", IntNode.valueOf(232));
        mNode.put("test2", TextNode.valueOf("12312"));
        mNode.put("test3", BooleanNode.TRUE);

        ecos.com.fasterxml.jackson210.databind.node.ObjectNode eNode = ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory.instance.objectNode();
        eNode.put("test", ecos.com.fasterxml.jackson210.databind.node.IntNode.valueOf(232));
        eNode.put("test2", ecos.com.fasterxml.jackson210.databind.node.TextNode.valueOf("12312"));
        eNode.put("test3", ecos.com.fasterxml.jackson210.databind.node.BooleanNode.TRUE);

        TestDto controlTestDto = new TestDto();
        controlTestDto.setTest(232);
        controlTestDto.setTest2("12312");
        controlTestDto.setTest3(true);

        TestDto check0 = Json.getMapper().convert(mNode, TestDto.class);
        TestDto check1 = Json.getMapper().convert(eNode, TestDto.class);

        assertEquals(controlTestDto, check0);
        assertEquals(controlTestDto, check1);

        Map<String, String> data = new HashMap<>();
        String value = "test";
        data.put("field", value);
        WithJsonField valueWithJsonField = Json.getMapper().convert(data, WithJsonField.class);
        assertEquals(value, valueWithJsonField.field.asText());
    }

    @Data
    public static class TestDto {
        private int test;
        private String test2;
        private boolean test3;
    }

    @Data
    public static class SomeQuery {
        private String queryField0 = "2424";
        private String queryField1 = "451231";
        private int intField = 222;
        private boolean boolField = true;
    }

    @Data
    public static class WithJsonField {

        private JsonNode field;
    }
}
