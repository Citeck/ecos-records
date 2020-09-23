package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.CreateVariant;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaEdgeTest extends LocalRecordsDao
                          implements LocalRecordsMetaDao {

    private static final String SOURCE_ID = "test-source";
    private static final String EDGE_FIELD_NAME = "test00";

    private RecordsService recordsService;

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records) {
        return Collections.singletonList(new MetaTestVal());
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        String att = ".edge(n:\"" + EDGE_FIELD_NAME + "\"){name,distinct{str,disp},options{str,disp},javaClass,editorKey,type,isAssoc,createVariants{json}}";
        List<RecordRef> records = Collections.singletonList(RecordRef.create(SOURCE_ID, "test"));
        RecordsResult<RecordMeta> result = recordsService.getAttributes(records, Collections.singletonMap("edge", att));

        RecordMeta meta = result.getRecords().get(0);

        DataValue edgeNode = meta.get("edge");

        assertEquals(MetaTestEdge.TYPE, edgeNode.get("type").asText());
        assertEquals(MetaTestEdge.EDITOR_KEY, edgeNode.get("editorKey").asText());
        assertEquals(MetaTestEdge.IS_ASSOC, edgeNode.get("isAssoc").asBoolean(false));

        CreateVariant variant = Json.getMapper().convert(edgeNode.get("/createVariants/0"), CreateVariant.class);
        assertEquals(MetaTestEdge.CREATE_VARIANT, variant);

        assertEquals(EDGE_FIELD_NAME, edgeNode.get("name").asText());

        List<String> distinctVars = new ArrayList<>();
        for (DataValue value : edgeNode.get("distinct")) {
            distinctVars.add(value.get("str").asText());
        }
        assertEquals(MetaTestEdge.distinctVariants, distinctVars);

        List<String> optionsVars = new ArrayList<>();
        for (DataValue value : edgeNode.get("options")) {
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
        public MetaEdge getEdge(String name) {
            return new MetaTestEdge(name);
        }
    }

    public static class MetaTestEdge implements MetaEdge {

        static String EDITOR_KEY = "editor key";
        static String TYPE = "_type_";
        static boolean IS_ASSOC = true;
        static CreateVariant CREATE_VARIANT;

        static {
            CREATE_VARIANT = new CreateVariant(RecordRef.valueOf("1231231@213123"));
            CREATE_VARIANT.setAttribute("test", "test2");
            CREATE_VARIANT.setAttribute("test4", "test3");
            CREATE_VARIANT.setFormKey("SomeFormKey");
        }

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

        private final String name;

        MetaTestEdge(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getValue() {
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

        @Override
        public List<CreateVariant> getCreateVariants() {
            List<CreateVariant> variants = new ArrayList<>();
            variants.add(CREATE_VARIANT);
            return variants;
        }
    }
}
