package ru.citeck.ecos.records.test;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.schema.resolver.AttContext;
import ru.citeck.ecos.records3.record.op.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.op.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.RecordsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryWithMetaFieldTest extends AbstractRecordsDao implements RecordsQueryDao {

    private static final String ID = "test";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();

        setId(ID);
        recordsService.register(this);
    }

    @Override
    public RecordsQueryRes<Object> queryRecords(@NotNull RecordsQuery query) {

        SchemaAtt field = AttContext.getCurrentSchemaAtt();

        List<SchemaAtt> atts = field.getInner();
        assertEquals(6, atts.size());
        assertEquals(new HashSet<>(Arrays.asList(
            "field0",
            "field1",
            "field2",
            "sum(\"field1\")",
            "sum(\\\"field2\\\")",
            "_edge"
        )), new HashSet<>(atts.stream().map(SchemaAtt::getName).collect(Collectors.toList())));

        Map<String, String> innerAttributesMap = AttContext.getInnerAttsMap();

        assertEquals(6, innerAttributesMap.size());
        assertEquals(".att(n:\"field0\"){disp}", innerAttributesMap.get("field0"));
        assertEquals(".att(n:\"field1\"){num}", innerAttributesMap.get("field1"));
        assertEquals(".atts(n:\"field2\"){disp}", innerAttributesMap.get("field2"));
        assertEquals(".att(n:\"sum(\\\"field1\\\")\"){num}", innerAttributesMap.get("sum(\"field1\")"));
        assertEquals(".att(n:\"sum(\\\\\"field2\\\\\")\"){num}", innerAttributesMap.get("sum(\\\"field2\\\")"));
        assertEquals(".edge(n:\"field0(\\\"param\\\")\"){options{label:disp,value:str}}", innerAttributesMap.get("_edge"));

        TestDto dto = new TestDto();
        dto.setField0("value0");
        dto.setField1(100);
        dto.setField2(Arrays.asList("1", "2"));

        return new RecordsQueryRes<>(Collections.singletonList(dto));
    }

    @Test
    void test() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        RecordsQueryRes<TestDto> result = recordsService.query(query, TestDto.class);

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

        @AttName("sum(\"field1\")")
        private int sum;

        @AttName("sum(\\\"field2\\\")")
        private int sum2;

        @AttName("#field0(\"param\")?options")
        private List<Object> options;
    }
}
