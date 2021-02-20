package ru.citeck.ecos.records3.test.evaluator;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.evaluator.details.EvalDetails;
import ru.citeck.ecos.records2.evaluator.evaluators.GroupEvaluator;
import ru.citeck.ecos.records2.evaluator.evaluators.PredicateEvaluator;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluatorsTest extends AbstractRecordsDao implements RecordsAttsDao,
                                                               RecordEvaluator<EvaluatorsTest.RequiredMeta,
                                                                   EvaluatorsTest.RequiredMeta,
                                                                   Object> {

    private static final String ID = "test";

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);

        RecordEvaluatorService evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(this);
        evaluatorsService.register(new EvaluatorWithDetails());

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

        String cause0 = "cause0";
        evaluatorDto = withDetails(false, cause0);
        EvalDetails evalDetails = evaluatorsService.evalWithDetails(meta0Ref, evaluatorDto);
        assertFalse(evalDetails.getResult());
        assertEquals(1, evalDetails.getCauses().size());
        assertEquals(cause0, evalDetails.getCauses().get(0).getMessage());

        String cause1 = "cause1";
        evaluatorDto = withDetails(true, cause0, cause1);
        evalDetails = evaluatorsService.evalWithDetails(meta0Ref, evaluatorDto);
        assertTrue(evalDetails.getResult());
        assertEquals(2, evalDetails.getCauses().size());
        assertEquals(cause0, evalDetails.getCauses().get(0).getMessage());
        assertEquals(cause1, evalDetails.getCauses().get(1).getMessage());
    }

    private RecordEvaluatorDto withDetails(boolean result, String... cause) {

        RecordEvaluatorDto evaluatorWithDetails = new RecordEvaluatorDto();
        evaluatorWithDetails.setType(EvaluatorWithDetails.TYPE);

        EvaluatorWithDetails.Config config = new EvaluatorWithDetails.Config();
        config.setResult(result);
        config.setCause(cause);

        evaluatorWithDetails.setConfig(Json.getMapper().convert(config, ObjectData.class));
        return evaluatorWithDetails;
    }

    private RecordEvaluatorDto toEvaluatorDto(Predicate predicate) {

        RecordEvaluatorDto predicateEvaluator = new RecordEvaluatorDto();
        predicateEvaluator.setType(PredicateEvaluator.TYPE);

        PredicateEvaluator.Config config = new PredicateEvaluator.Config();
        config.setPredicate(predicate);

        predicateEvaluator.setConfig(Json.getMapper().convert(config, ObjectData.class));
        return predicateEvaluator;
    }

    private RecordEvaluatorDto groupEvaluator(GroupEvaluator.JoinType joinType, RecordEvaluatorDto... evaluators) {

        RecordEvaluatorDto groupEvaluator = new RecordEvaluatorDto();
        groupEvaluator.setType("group");

        GroupEvaluator.Config groupConfig = new GroupEvaluator.Config();
        groupConfig.setJoinBy(joinType);
        groupConfig.setEvaluators(Arrays.asList(evaluators));

        groupEvaluator.setConfig(Json.getMapper().convert(groupConfig, ObjectData.class));

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
        ObjectData attributes = ObjectData.create();
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
    public List<?> getRecordsAtts(List<String> records) {
        return records.stream()
            .map(r -> r.equals("Meta0") ? new Meta0() : new Meta1())
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

