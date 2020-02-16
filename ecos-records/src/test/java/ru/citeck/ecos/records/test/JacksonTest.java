package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.attributes.Attributes;
import ru.citeck.ecos.records2.request.query.QueryConsistency;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.SortBy;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonTest {

    @Test
    public void test() throws IOException {

        new RecordsServiceFactory();

        RecordsQuery query = new RecordsQuery();
        query.setMaxItems(10);
        query.setSourceId("sourceId");
        query.setLanguage("language");
        query.setGroupBy(Arrays.asList("one", "two"));
        query.setAfterId(RecordRef.valueOf("aaa/bbb@123"));
        query.setSkipCount(55);
        query.setQuery(new SomeQuery());
        query.setConsistency(QueryConsistency.TRANSACTIONAL);
        query.setSortBy(Arrays.asList(
            new SortBy("att", true),
            new SortBy("att2", true))
        );

        ObjectMapper mapper = new ObjectMapper();

        String mJsonString = mapper.writeValueAsString(query);
        String eJsonString = JsonUtils.toString(query);

        RecordsQuery res0 = JsonUtils.read(mJsonString, RecordsQuery.class);
        RecordsQuery res1 = mapper.readValue(mJsonString, RecordsQuery.class);
        RecordsQuery res2 = mapper.readValue(eJsonString, RecordsQuery.class);
        RecordsQuery res3 = mapper.readValue(eJsonString, RecordsQuery.class);

        assertEquals(query, res0);
        assertEquals(query, res1);
        assertEquals(query, res2);
        assertEquals(query, res3);

        Attributes atts = new Attributes();
        atts.set("abc", 2134124);
        atts.set("eeeeee", "adasd");
        atts.set("double", 24.23);
        atts.set("complex", new SomeQuery());
        atts.set("bool", true);

        String mAttsJsonString = mapper.writeValueAsString(atts);
        String eAttsJsonString = JsonUtils.toString(atts);

        Attributes attsRes0 = mapper.readValue(mAttsJsonString, Attributes.class);
        Attributes attsRes1 = mapper.readValue(eAttsJsonString, Attributes.class);
        Attributes attsRes2 = JsonUtils.read(mAttsJsonString, Attributes.class);
        Attributes attsRes3 = JsonUtils.read(eAttsJsonString, Attributes.class);

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

        TestDto check0 = JsonUtils.convert(mNode, TestDto.class);
        TestDto check1 = JsonUtils.convert(eNode, TestDto.class);

        assertEquals(controlTestDto, check0);
        assertEquals(controlTestDto, check1);
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
}
