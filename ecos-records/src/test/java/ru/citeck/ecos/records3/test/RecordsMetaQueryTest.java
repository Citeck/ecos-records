package ru.citeck.ecos.records3.test;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.schema.write.AttSchemaWriter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsMetaQueryTest {

    private DtoSchemaReader dtoSchemaReader;
    private AttSchemaWriter attSchemaWriter;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        attSchemaWriter = factory.getAttSchemaWriter();
        dtoSchemaReader = factory.getDtoSchemaReader();
    }

    @Test
    void testQueryBuild() {

        Map<String, String> attributes = attSchemaWriter.writeToMap(dtoSchemaReader.read(SimplePojo.class));

        assertEquals(12, attributes.size());

        assertEquals(".att(n:\"jsonArrayNode\"){json}", attributes.get("jsonArrayNode"));
        assertEquals(".att(n:\"jsonObjectNode\"){json}", attributes.get("jsonObjectNode"));
        assertEquals(".att(n:\"jsonNode\"){json}", attributes.get("jsonNode"));
        assertEquals(".att(n:\"aaa\"){atts(n:\"bbb\"){disp}}", attributes.get("someatt"));
        assertEquals(".edge(n:\"cm:title\"){multiple}", attributes.get("edge"));
        assertEquals(".edge(n:\"cm:field\"){options{label:disp,value:str}}", attributes.get("options"));
        assertEquals(".att(n:\"cm:name\"){disp}", attributes.get("value0"));
        assertEquals(".att(n:\"cm:caseStatus\"){att(n:\"cm:statusName\"){disp}}", attributes.get("status"));
        assertEquals(".att(n:\"cm:caseStatus\"){att(n:\"cm:statusName\"){str}}", attributes.get("status1"));
        assertEquals(".atts(n:\"cm:caseStatus\"){att(n:\"cm:statusName\"){str}}", attributes.get("statuses"));
        assertEquals(".att(n:\"cm:caseStatus\"){atts(n:\"cm:statusName\"){str}}", attributes.get("statuses1"));
        assertEquals(".att(n:\"enumField\"){str}", attributes.get("enumField"));
    }

    public static class SimplePojo {

        @AttName("cm:name")
        private String value0;

        @AttName("cm:caseStatus.cm:statusName")
        private String status;

        @AttName("cm:caseStatus.cm:statusName?str")
        private String status1;

        @AttName("cm:caseStatus.cm:statusName?str")
        private List<String> statuses;

        @AttName("cm:caseStatus.cm:statusName[]?str")
        private List<String> statuses1;

        @AttName(".att(n:'aaa'){atts(n:'bbb')}")
        private List<String> someatt;

        @AttName("#cm:title?multiple")
        private boolean edge;

        @AttName("#cm:field?options")
        private List<Map<String, String>> options;

        private JsonNode jsonNode;
        private ObjectNode jsonObjectNode;
        private ArrayNode jsonArrayNode;

        @Getter @Setter
        private EnumType enumField;

        public ArrayNode getJsonArrayNode() {
            return jsonArrayNode;
        }

        public void setJsonArrayNode(ArrayNode jsonArrayNode) {
            this.jsonArrayNode = jsonArrayNode;
        }

        public ObjectNode getJsonObjectNode() {
            return jsonObjectNode;
        }

        public void setJsonObjectNode(ObjectNode jsonObjectNode) {
            this.jsonObjectNode = jsonObjectNode;
        }

        public JsonNode getJsonNode() {
            return jsonNode;
        }

        public void setJsonNode(JsonNode jsonNode) {
            this.jsonNode = jsonNode;
        }

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

    public enum EnumType { FIRST, SECOND }
}
