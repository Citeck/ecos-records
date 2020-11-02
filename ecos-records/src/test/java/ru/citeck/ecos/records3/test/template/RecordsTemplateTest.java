package ru.citeck.ecos.records3.test.template;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.meta.RecordsTemplateService;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsTemplateTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String ID = "first";

    private RecordsTemplateService recordsTemplateService;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsTemplateService = factory.getRecordsTemplateService();
        factory.getRecordsServiceV1().register(this);
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
    public List<?> getRecordsAtts(List<String> records) {
        return records.stream().map(r -> {
            if (r.equals("rec")) {
                return new RecData();
            } else {
                return EmptyAttValue.INSTANCE;
            }
        }).collect(Collectors.toList());
    }

    @Data
    public static class RecData {
        private String contractor = "Поставщик №1";
        private int field1 = 11;
        @AttName("_docNum")
        private int docNum = 100;
    }
}
