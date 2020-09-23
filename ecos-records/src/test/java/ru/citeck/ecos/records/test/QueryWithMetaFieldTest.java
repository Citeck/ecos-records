package ru.citeck.ecos.records.test;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;
import ru.citeck.ecos.records2.meta.schema.resolver.AttContext;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDao;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryWithMetaFieldTest extends LocalRecordsDao implements LocalRecordsQueryDao {

    private static final String ID = "test";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(ID);
        recordsService.register(this);
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery query) {

        SchemaAtt field = AttContext.getCurrentSchemaAtt();
        //todo
        /*
        List<String> atts = field.getInnerAttributes();
        assertEquals(6, atts.size());
        assertEquals(new HashSet<>(Arrays.asList(
            "field0",
            "field1",
            "field2",
            "sum(\"field1\")",
            "sum(\"field2\")",
            ".edge"//it's ok?
        )), new HashSet<>(atts));

        Map<String, String> innerAttributesMap = field.getInnerAttributesMap();

        assertEquals(6, innerAttributesMap.size());
        assertEquals(".att(n:\"field0\"){disp}", innerAttributesMap.get("field0"));
        assertEquals(".att(n:\"field1\"){num}", innerAttributesMap.get("field1"));
        assertEquals(".atts(n:\"field2\"){disp}", innerAttributesMap.get("field2"));
        assertEquals(".att(n:\"sum(\\\"field1\\\")\"){num}", innerAttributesMap.get("sum(\"field1\")"));
        assertEquals(".att(n:\"sum(\\\"field2\\\")\"){num}", innerAttributesMap.get("sum(\"field2\")"));
        assertEquals(".edge(n:\"field0(\\\"param\\\")\"){options{label:disp,value:str}}", innerAttributesMap.get(".edge"));

        TestDto dto = new TestDto();
        dto.setField0("value0");
        dto.setField1(100);
        dto.setField2(Arrays.asList("1", "2"));

        return new RecordsQueryResult<>(Collections.singletonList(dto));

         */
        return null;
    }

    @Test
    void test() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        RecordsQueryResult<TestDto> result = recordsService.queryRecords(query, TestDto.class);

        assertEquals(1, result.getRecords().size());
        assertEquals("value0", result.getRecords().get(0).field0);
        assertEquals(100, result.getRecords().get(0).field1);
        assertEquals(Arrays.asList("1", "2"), result.getRecords().get(0).field2);
    }

    @Data
    public static final class TestDto {

        private String field0;
        private int field1;
        private List<String> field2;

        @MetaAtt("sum(\"field1\")")
        private int sum;

        @MetaAtt("sum(\\\"field2\\\")")
        private int sum2;

        @MetaAtt("#field0(\"param\")?options")
        private List<Object> options;
    }
}
