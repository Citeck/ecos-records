package ru.citeck.ecos.records2.test.template;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.meta.RecordsTemplateService;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsTemplateTest extends LocalRecordsDao implements LocalRecordsMetaDao<Object> {

    private static final String ID = "first";

    private RecordsTemplateService recordsTemplateService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsTemplateService = factory.getRecordsTemplateService();
        setId(ID);
        factory.getRecordsService().register(this);
    }

    @Test
    void test() {

        MLText text = new MLText();
        text.set(new Locale("ru"), "Договор №${_docNum} для ${contractor}");
        text.set(new Locale("en"), "Contract №${_docNum} for ${contractor}");

        MLText expected = new MLText();
        expected.set(new Locale("ru"), "Договор №100 для Поставщик №1");
        expected.set(new Locale("en"), "Contract №100 for Поставщик №1");

        MLText resolved = recordsTemplateService.resolve(text, RecordRef.create(ID, "rec"));
        assertEquals(expected, resolved);
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream().map(r -> {
            if (r.getId().equals("rec")) {
                return new RecData();
            } else {
                return EmptyValue.INSTANCE;
            }
        }).collect(Collectors.toList());
    }

    @Data
    public static class RecData {
        private String contractor = "Поставщик №1";
        private int field1 = 11;
        @MetaAtt("_docNum")
        private int docNum = 100;
    }
}
