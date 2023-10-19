package ru.citeck.ecos.records3.test.orelse;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder;
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrElseAttTest {

    private RecordsService recordsService;

    private final EntityRef TEST_REF = EntityRef.create("test", "test");
    private final EntityRef PROXY_REF = EntityRef.create("proxy", "test");

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();

        recordsService.register(RecordsDaoBuilder.create("test")
            .addRecord("test", new RecordData())
            .build());

        recordsService.register(new RecordsDaoProxy("proxy", "test", null));
    }

    @Test
    void test() {
        assertAtt("strField", "strField");
        assertAtt(null, "strNullField");
        assertAtt("orElse", "strNullField!'orElse'");
        assertAtt("strField", "strNullField!strField");
        assertAtt("strField", "strField!strNullField");

        assertAtt(1000.0, "numberField?num");
        assertAtt(null, "numberNullField?num");
        assertAtt(999.0, "numberNullField?num!999.0");
        assertAtt(1000.0, "numberNullField?num!numberField?num");
        assertAtt(1000.0, "numberField?num!numberNullField?num");

        assertAtt("2020-01-01T00:00:00Z", "dateField!'2020-01-01T00:00:01Z'");
        assertAtt(null, "dateNullField");
        assertAtt("2020-01-01T00:00:01Z", "dateNullField!'2020-01-01T00:00:01Z'");
        assertAtt("2020-01-01T00:00:00Z", "dateNullField!dateField");
        assertAtt("2020-01-01T00:00:00Z", "dateField!dateNullField");

        assertAtt(true, "boolField?bool");
        assertAtt(null, "boolNullField?bool");
        assertAtt(true, "boolNullField?bool!boolField?bool");
        assertAtt(true, "boolNullField?bool!true");
        assertAtt(true, "boolField?bool!boolNullField?bool");

        assertAtt("123", "emptyStr?str!'123'");
    }

    @Test
    void longAttsTest() {

        assertAtt(true, ".att('boolNullField'){bool}!.att('numberNullField'){num}!.att('strNullField'){disp}!true");
        assertAtt(true, "boolNullField?bool!numberNullField?num!strNullField!true");
        assertAtt(false, "boolNullField?bool!numberNullField?num!strNullField!false");
        assertAtt(false, ".att('boolNullField'){bool}!.att('numberNullField'){num}!.att('strNullField'){disp}!false");

        assertAtt("strField", "boolNullField?bool!numberNullField?num!strField");
    }

    @Test
    void innerTest() {
        assertAtt(true, "meta?as('recordData').boolField?bool");
        assertAtt(true, "meta?as('recordData').meta?has('abc')");
        assertAtt(false, "meta?as('recordData').meta?has('def')");
    }

    @Test
    void multiOrTest() {
        assertAtt("abc", "strField1!strField1!strField1!'abc'");
        assertAtt("strField", "strField!strField!strField!'abc'");
    }

    @Test
    void arrOrElseTest() {
        assertAtt(Collections.emptyList(), "strEmptyList[]?disp![]");
        assertAtt(Collections.emptyList(), "strNullList[]?disp![]");
        assertAtt(Collections.emptyList(), "strListWithNullElem[]?disp![]");
        assertAtt(Collections.emptyList(), "strDvListWithNullElem[]?disp![]");
        assertAtt(Collections.emptyList(), "strDvNullList[]?disp![]");

        assertAtt(Collections.emptyList(), "strEmptyList[]?disp");
        assertAtt(Collections.emptyList(), "strNullList[]?disp");
        assertAtt(Collections.emptyList(), "strListWithNullElem[]?disp");
        assertAtt(Collections.emptyList(), "strDvListWithNullElem[]?disp");
        assertAtt(Collections.emptyList(), "strDvNullList[]?disp");

        assertAtt(Arrays.asList(new String[]{null}), "dvListWithListWithNullElem[].unknownField");
        assertAtt(Arrays.asList(new String[]{null}), "dvListWithListWithNullElem[].unknownField![]");
        assertAtt(Collections.emptyList(), "dvListWithListWithNullElem.unknownField.unknownField[].unknownField![]");
    }

    void assertAtt(Object expected, String att) {

        assertEquals(DataValue.create(expected), recordsService.getAtt(TEST_REF, att));
        assertEquals(DataValue.create(expected), recordsService.getAtt(PROXY_REF, att));

        RecordsQuery query = RecordsQuery.create()
            .withQuery(Predicates.alwaysTrue())
            .build();

        assertEquals(DataValue.create(expected), recordsService.queryOne(query.withSourceId(TEST_REF.getSourceId()), att));
        assertEquals(DataValue.create(expected), recordsService.queryOne(query.withSourceId(PROXY_REF.getSourceId()), att));
    }

    @Data
    public static class RecordData {
        private String emptyStr = "";
        private String strField = "strField";
        private String strNullField = null;
        private Double numberField = 1000.0;
        private Double numberNullField = null;
        private Date dateField = Date.from(Instant.parse("2020-01-01T00:00:00Z"));
        private Date dateNullField = null;
        private Boolean boolField = true;
        private Boolean boolNullField = null;
        private MetaData meta = new MetaData();
        private List<String> strEmptyList = new ArrayList<>();
        private List<String> strNullList = new ArrayList<>();
        private List<String> strListWithNullElem = Arrays.asList(new String[]{null});
        private DataValue strDvListWithNullElem = DataValue.createArr().add(null);
        private DataValue dvListWithListWithNullElem = DataValue.createArr().add(DataValue.createObj());
        private DataValue strDvNullList = null;
    }

    public static class MetaData implements AttValue {

        @Override
        public Object getAs(@NotNull String type) {
            if (type.equals("recordData")) {
                return new RecordData();
            }
            return null;
        }

        @Override
        public boolean has(@NotNull String name) {
            switch (name) {
                case "abc":
                    return true;
                case "def":
                    return false;
            }
            return false;
        }
    }
}

