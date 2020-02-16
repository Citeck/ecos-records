package ru.citeck.ecos.records.test.evaluator;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.attributes.Attributes;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.evaluator.evaluators.GroupEvaluator;
import ru.citeck.ecos.records2.evaluator.evaluators.PredicateEvaluator;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.utils.JsonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EvaluatorsTest extends LocalRecordsDAO implements LocalRecordsMetaDAO<Object>,
                                                               RecordEvaluator<EvaluatorsTest.RequiredMeta,
                                                                   EvaluatorsTest.RequiredMeta,
                                                                   Object> {

    private static final String ID = "test";

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);

        RecordEvaluatorService evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(this);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType(ID);

        RecordRef meta0Ref = RecordRef.create(ID, "Meta0");
        RecordRef meta1Ref = RecordRef.create(ID, "Meta1");
        RecordRef unknownRef = RecordRef.create("", "unknown");

        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(unknownRef, evaluatorDto));

        evaluatorDto.setInverse(true);

        assertFalse(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertTrue(evaluatorsService.evaluate(meta1Ref, evaluatorDto));
        assertTrue(evaluatorsService.evaluate(unknownRef, evaluatorDto));

        evaluatorDto = alwaysTrueEvaluator();

        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertTrue(evaluatorsService.evaluate(meta1Ref, evaluatorDto));
        assertTrue(evaluatorsService.evaluate(unknownRef, evaluatorDto));

        evaluatorDto = alwaysFalseEvaluator();

        assertFalse(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(unknownRef, evaluatorDto));

        evaluatorDto = hasAttEvaluator("field0");

        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        evaluatorDto.getConfig().set("attribute", "unknown_field");
        assertFalse(evaluatorsService.evaluate(meta0Ref, evaluatorDto));

        RecordEvaluatorDto groupEvaluator = groupEvaluator(
            GroupEvaluator.JoinType.AND,
            hasAttEvaluator("field0"),
            alwaysTrueEvaluator()
        );

        assertTrue(evaluatorsService.evaluate(meta0Ref, groupEvaluator));

        groupEvaluator = groupEvaluator(
            GroupEvaluator.JoinType.AND,
            hasAttEvaluator("unknown"),
            alwaysTrueEvaluator()
        );

        assertFalse(evaluatorsService.evaluate(meta0Ref, groupEvaluator));

        groupEvaluator = groupEvaluator(
            GroupEvaluator.JoinType.AND,
            hasAttEvaluator("field0"),
            hasAttEvaluator("field1"),
            alwaysTrueEvaluator()
        );

        assertTrue(evaluatorsService.evaluate(meta0Ref, groupEvaluator));

        groupEvaluator = groupEvaluator(
            GroupEvaluator.JoinType.AND,
            hasAttEvaluator("field0"),
            hasAttEvaluator("unknown"),
            alwaysTrueEvaluator()
        );

        assertFalse(evaluatorsService.evaluate(meta0Ref, groupEvaluator));

        groupEvaluator = groupEvaluator(
            GroupEvaluator.JoinType.OR,
            hasAttEvaluator("field0"),
            hasAttEvaluator("unknown"),
            alwaysTrueEvaluator()
        );

        assertTrue(evaluatorsService.evaluate(meta0Ref, groupEvaluator));

        groupEvaluator = groupEvaluator(
            GroupEvaluator.JoinType.OR,
            hasAttEvaluator("unknown"),
            hasAttEvaluator("unknown"),
            alwaysTrueEvaluator()
        );

        assertTrue(evaluatorsService.evaluate(meta0Ref, groupEvaluator));

        evaluatorDto = toEvaluatorDto(Predicates.eq("field0", "field0"));
        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));

        evaluatorDto = toEvaluatorDto(Predicates.and(
            Predicates.eq("field0", "field0"),
            Predicates.eq("field1", "field1")
        ));
        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));

        evaluatorDto = toEvaluatorDto(Predicates.eq("intField2", 10));
        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));

        evaluatorDto = toEvaluatorDto(Predicates.gt("intField2", 10));
        assertFalse(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));

        evaluatorDto = toEvaluatorDto(Predicates.ge("intField2", 10));
        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));

        evaluatorDto = toEvaluatorDto(Predicates.le("intField2", 10));
        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));

        evaluatorDto = toEvaluatorDto(Predicates.gt("intField2", 5));
        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));

        evaluatorDto = toEvaluatorDto(Predicates.lt("intField2", 15));
        assertTrue(evaluatorsService.evaluate(meta0Ref, evaluatorDto));
        assertFalse(evaluatorsService.evaluate(meta1Ref, evaluatorDto));
    }

    private RecordEvaluatorDto toEvaluatorDto(Predicate predicate) {

        RecordEvaluatorDto predicateEvaluator = new RecordEvaluatorDto();
        predicateEvaluator.setType(PredicateEvaluator.TYPE);

        PredicateEvaluator.Config config = new PredicateEvaluator.Config();
        config.setPredicate(predicate);

        predicateEvaluator.setConfig(JsonUtils.convert(config, Attributes.class));
        return predicateEvaluator;
    }

    private RecordEvaluatorDto groupEvaluator(GroupEvaluator.JoinType joinType, RecordEvaluatorDto... evaluators) {

        RecordEvaluatorDto groupEvaluator = new RecordEvaluatorDto();
        groupEvaluator.setType("group");

        GroupEvaluator.Config groupConfig = new GroupEvaluator.Config();
        groupConfig.setJoinBy(joinType);
        groupConfig.setEvaluators(Arrays.asList(evaluators));

        groupEvaluator.setConfig(JsonUtils.convert(groupConfig, Attributes.class));

        return groupEvaluator;
    }

    private RecordEvaluatorDto alwaysFalseEvaluator() {
        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("false");
        return evaluatorDto;
    }

    private RecordEvaluatorDto alwaysTrueEvaluator() {
        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("true");
        return evaluatorDto;
    }

    private RecordEvaluatorDto hasAttEvaluator(String att) {
        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("has-attribute");
        Attributes attributes = new Attributes();
        attributes.set("attribute", att);
        evaluatorDto.setConfig(attributes);
        return evaluatorDto;
    }

    @Override
    public String getType() {
        return ID;
    }

    @Override
    public RequiredMeta getMetaToRequest(Object o) {
        return new RequiredMeta();
    }

    @Override
    public boolean evaluate(RequiredMeta meta, Object o) {
        return "field0".equals(meta.getField0());
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream()
            .map(r -> r.getId().equals("Meta0") ? new Meta0() : new Meta1())
            .collect(Collectors.toList());
    }

    @Data
    public static final class RequiredMeta {
        private String field0;
    }

    @Data
    public static final class Meta0 {

        private String field0 = "field0";
        private String field1 = "field1";
        private int intField2 = 10;
    }

    @Data
    public static final class Meta1 {

        private String field0 = "meta1field0";
        private String field1 = "meta1field1";
    }
}

