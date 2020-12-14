package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.op.atts.dto.CreateVariant;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaEdgeTest extends AbstractRecordsDao
                          implements RecordsAttsDao {

    private static final String SOURCE_ID = "test-source";
    private static final String EDGE_FIELD_NAME = "test00";

    private RecordsService recordsService;

    @NotNull
    @Override
    public String getId() {
        return SOURCE_ID;
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return Collections.singletonList(new MetaTestVal());
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();

        recordsService.register(this);
    }

    @Test
    void test() {

        String att = ".edge(n:\"" + EDGE_FIELD_NAME + "\"){name,distinct{str,disp},options{str,disp},javaClass,editorKey,type,isAssoc,createVariants{json},unreadable}";
        List<RecordRef> records = Collections.singletonList(RecordRef.create(SOURCE_ID, "test"));
        List<RecordAtts> result = recordsService.getAtts(records, Collections.singletonMap("edge", att));

        RecordAtts meta = result.get(0);

        DataValue edgeNode = meta.getAtt("edge");

        assertTrue(edgeNode.get("unreadable").asBoolean());

        assertEquals(MetaTestEdge.TYPE, edgeNode.get("type").asText());
        assertEquals(MetaTestEdge.EDITOR_KEY, edgeNode.get("editorKey").asText());
        assertEquals(MetaTestEdge.IS_ASSOC, edgeNode.get("isAssoc").asBoolean(false));

        CreateVariant variant = Json.getMapper().convert(edgeNode.get("/createVariants{json}/0"), CreateVariant.class);
        assertEquals(MetaTestEdge.CREATE_VARIANT, variant);

        assertEquals(EDGE_FIELD_NAME, edgeNode.get("name").asText());

        List<String> distinctVars = new ArrayList<>();
        for (DataValue value : edgeNode.get("distinct{str,disp}")) {
            distinctVars.add(value.get("str").asText());
        }
        assertEquals(MetaTestEdge.distinctVariants, distinctVars);

        List<String> optionsVars = new ArrayList<>();
        for (DataValue value : edgeNode.get("options{str,disp}")) {
            optionsVars.add(value.get("str").asText());
        }
        assertEquals(MetaTestEdge.optionsVariants, optionsVars);

        assertEquals(String.class.getName(), edgeNode.get("javaClass").asText());

        List<JavaClassDto> classMeta = recordsService.getAtts(records, JavaClassDto.class);
        assertEquals(String.class, classMeta.get(0).getJavaClass());
        assertEquals(String.class, classMeta.get(0).getJavaClass2());
    }

    public static class JavaClassDto {

        @AttName(".edge(n:'test'){javaClass}")
        private Class<?> javaClass;

        @AttName("#test?javaClass")
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

    public static class MetaTestVal implements AttValue {

        @Override
        public String asText() {
            return null;
        }

        @Override
        public AttEdge getEdge(@NotNull String name) {
            return new MetaTestEdge(name);
        }
    }

    public static class MetaTestEdge implements AttEdge {

        static String EDITOR_KEY = "editor key";
        static String TYPE = "_type_";
        static boolean IS_ASSOC = true;
        static CreateVariant CREATE_VARIANT;

        static {
            CREATE_VARIANT = CreateVariant.create()
                .withLabel("Create label")
                .withRecordRef(RecordRef.valueOf("1231231@213123"))
                .withAttribute("test", "test2")
                .withAttribute("test4", "test3")
                .withFormKey("SomeFormKey")
                .build();
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

        // invisible

        @Override
        public boolean isUnreadable() {
            return true;
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
