package ru.citeck.ecos.records.test;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.meta.AttributesSchema;
import ru.citeck.ecos.records2.meta.RecordsMetaService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsMetaQueryTest {

    private RecordsMetaService recordsMetaService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsMetaService = factory.createRecordsMetaService();
    }

    @Test
    void testQueryBuild() {

        Map<String, String> attributes = recordsMetaService.getAttributes(SimplePojo.class);

        assertEquals(8, attributes.size());

        assertEquals(".att(n:\"aaa\"){atts(n:\"bbb\"){disp}}", attributes.get("someatt"));
        assertEquals(".edge(n:\"cm:title\"){multiple}", attributes.get("edge"));
        assertEquals(".edge(n:\"cm:field\"){options{label:disp,value:str}}", attributes.get("options"));
        assertEquals(".att(n:\"cm:name\"){disp}", attributes.get("value0"));
        assertEquals(".att(n:\"cm:caseStatus\"){att(n:\"cm:statusName\"){disp}}", attributes.get("status"));
        assertEquals(".att(n:\"cm:caseStatus\"){att(n:\"cm:statusName\"){str}}", attributes.get("status1"));
        assertEquals(".atts(n:\"cm:caseStatus\"){att(n:\"cm:statusName\"){str}}", attributes.get("statuses"));
        assertEquals(".att(n:\"cm:caseStatus\"){atts(n:\"cm:statusName\"){str}}", attributes.get("statuses1"));

        Map<String, String> attributesMap = new TreeMap<>(Comparator.comparingInt(Integer::parseInt));
        attributesMap.put("0", "name.title.field0.field1");
        attributesMap.put("1", "name.title[].field0.field1");
        attributesMap.put("2", "name.title.field0[].field1");
        attributesMap.put("3", "name.title.field0.field1?str");
        attributesMap.put("4", "name.title.field0.field1?num");
        attributesMap.put("5", "name");
        attributesMap.put("6", ".att(n:\"name\"){att(n:\"title\"){str}}");
        attributesMap.put("7", "name?num");
        attributesMap.put("8", "name\\.withdot.title?num");

        AttributesSchema schema = recordsMetaService.createSchema(attributesMap);
        assertEquals(""
            + "a:att(n:\"name\"){att(n:\"title\"){att(n:\"field0\"){att(n:\"field1\"){disp}}}},"
            + "b:att(n:\"name\"){atts(n:\"title\"){att(n:\"field0\"){att(n:\"field1\"){disp}}}},"
            + "c:att(n:\"name\"){att(n:\"title\"){atts(n:\"field0\"){att(n:\"field1\"){disp}}}},"
            + "d:att(n:\"name\"){att(n:\"title\"){att(n:\"field0\"){att(n:\"field1\"){str}}}},"
            + "e:att(n:\"name\"){att(n:\"title\"){att(n:\"field0\"){att(n:\"field1\"){num}}}},"
            + "f:att(n:\"name\"){disp},"
            + "g:att(n:\"name\"){att(n:\"title\"){str}},"
            + "h:att(n:\"name\"){num},"
            + "i:att(n:\"name.withdot\"){att(n:\"title\"){num}}", schema.getSchema());
    }

    public static class SimplePojo {

        @MetaAtt("cm:name")
        private String value0;

        @MetaAtt("cm:caseStatus.cm:statusName")
        private String status;

        @MetaAtt("cm:caseStatus.cm:statusName?str")
        private String status1;

        @MetaAtt("cm:caseStatus.cm:statusName?str")
        private List<String> statuses;

        @MetaAtt("cm:caseStatus.cm:statusName[]?str")
        private List<String> statuses1;

        @MetaAtt(".att(n:'aaa'){atts(n:'bbb')}")
        private List<String> someatt;

        @MetaAtt("#cm:title?multiple")
        private boolean edge;

        @MetaAtt("#cm:field?options")
        private List<Map<String, String>> options;

        public List<String> getStatuses1() {
            return statuses1;
        }

        public void setStatuses1(List<String> statuses1) {
            this.statuses1 = statuses1;
        }

        public List<String> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<String> statuses) {
            this.statuses = statuses;
        }

        public String getStatus1() {
            return status1;
        }

        public void setStatus1(String status1) {
            this.status1 = status1;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getValue0() {
            return value0;
        }

        public void setValue0(String value0) {
            this.value0 = value0;
        }

        public List<Map<String, String>> getOptions() {
            return options;
        }

        public void setOptions(List<Map<String, String>> options) {
            this.options = options;
        }

        public boolean isEdge() {
            return edge;
        }

        public void setEdge(boolean edge) {
            this.edge = edge;
        }

        public List<String> getSomeatt() {
            return someatt;
        }

        public void setSomeatt(List<String> someatt) {
            this.someatt = someatt;
        }
    }

}
