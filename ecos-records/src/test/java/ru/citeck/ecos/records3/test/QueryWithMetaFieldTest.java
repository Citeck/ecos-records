package ru.citeck.ecos.records3.test;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryWithMetaFieldTest extends AbstractRecordsDao implements RecordsQueryDao {

    private static final String ID = "test";

    private RecordsService recordsService;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Override
    public RecsQueryRes<Object> queryRecords(@NotNull RecordsQuery query) {

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
        assertEquals("field0", innerAttributesMap.get("field0"));
        assertEquals("field1?num", innerAttributesMap.get("field1"));
        assertEquals("field2[]", innerAttributesMap.get("field2"));
        assertEquals("sum(\"field1\")?num", innerAttributesMap.get("sum(\"field1\")"));
        assertEquals("sum(\\\"field2\\\")?num", innerAttributesMap.get("sum(\\\"field2\\\")"));
        assertEquals("_edge.field0(\"param\").options[]{?disp,?str}", innerAttributesMap.get("_edge"));

        TestDto dto = new TestDto();
        dto.setField0("value0");
        dto.setField1(100);
        dto.setField2(Arrays.asList("1", "2"));

        return new RecsQueryRes<>(Collections.singletonList(dto));
    }

    @Test
    void test() {

        RecordsQuery query = RecordsQuery.create()
            .withSourceId(ID)
            .build();

        RecsQueryRes<TestDto> result = recordsService.query(query, TestDto.class);

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
