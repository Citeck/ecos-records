package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaEdgeTest extends LocalRecordsDAO
                          implements RecordsMetaLocalDAO<Object> {

    private static final String SOURCE_ID = "test-source";
    private static final String EDGE_FIELD_NAME = "test00";

    private RecordsService recordsService;

    @Override
    public List<Object> getMetaValues(List<RecordRef> records) {
        return Collections.singletonList(new MetaTestVal());
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        String schema = "edge(n:\"" + EDGE_FIELD_NAME + "\"){name,distinct{str,disp},options{str,disp},javaClass,editorKey,type,isAssoc}";
        List<RecordRef> records = Collections.singletonList(RecordRef.create(SOURCE_ID, "test"));
        RecordsResult<RecordMeta> result = recordsService.getMeta(records, schema);

        RecordMeta meta = result.getRecords().get(0);

        JsonNode edgeNode = meta.get("edge");

        assertEquals(MetaTestEdge.TYPE, edgeNode.get("type").asText());
        assertEquals(MetaTestEdge.EDITOR_KEY, edgeNode.get("editorKey").asText());
        assertEquals(MetaTestEdge.IS_ASSOC, edgeNode.get("isAssoc").asBoolean(false));

        assertEquals(EDGE_FIELD_NAME, edgeNode.path("name").asText());

        List<String> distinctVars = new ArrayList<>();
        for (JsonNode value : edgeNode.get("distinct")) {
            distinctVars.add(value.get("str").asText());
        }
        assertEquals(MetaTestEdge.distinctVariants, distinctVars);

        List<String> optionsVars = new ArrayList<>();
        for (JsonNode value : edgeNode.get("options")) {
            optionsVars.add(value.get("str").asText());
        }
        assertEquals(MetaTestEdge.optionsVariants, optionsVars);

        assertEquals(String.class.getName(), edgeNode.get("javaClass").asText());

        RecordsResult<JavaClassDto> classMeta = recordsService.getMeta(records, JavaClassDto.class);
        assertEquals(String.class, classMeta.getRecords().get(0).getJavaClass());
        assertEquals(String.class, classMeta.getRecords().get(0).getJavaClass2());
    }

    public static class JavaClassDto {

        @MetaAtt(".edge(n:'test'){javaClass}")
        private Class<?> javaClass;

        @MetaAtt("#test?javaClass")
        private Class<?> javaClass2;

        public Class<?> getJavaClass2() {
            return javaClass2;
        }

        public void setJavaClass2(Class<?> javaClass2) {
            this.javaClass2 = javaClass2;
        }

        public Class<?> getJavaClass() {
            return javaClass;
        }

        public void setJavaClass(Class<?> javaClass) {
            this.javaClass = javaClass;
        }
    }

    public static class MetaTestVal implements MetaValue {

        @Override
        public String getString() {
            return null;
        }

        @Override
        public MetaEdge getEdge(String name, MetaField field) {
            return new MetaTestEdge(name);
        }
    }

    public static class MetaTestEdge implements MetaEdge {

        static String EDITOR_KEY = "editor key";
        static String TYPE = "_type_";
        static boolean IS_ASSOC = true;

        static List<?> distinctVariants = Arrays.asList(
            "first",
            "second",
            "third"
        );

        static List<?> optionsVariants = Arrays.asList(
            "opt_first",
            "opt_second",
            "opt_third"
        );

        private String name;

        MetaTestEdge(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getValue(MetaField field) {
            return new MetaTestVal();
        }

        @Override
        public List<?> getDistinct() {
            return distinctVariants;
        }

        @Override
        public List<?> getOptions() {
            return optionsVariants;
        }

        @Override
        public Class<?> getJavaClass() {
            return String.class;
        }

        @Override
        public String getEditorKey() {
            return EDITOR_KEY;
        }

        @Override
        public String getType() {
            return TYPE;
        }

        @Override
        public boolean isAssociation() {
            return IS_ASSOC;
        }
    }
}
