package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.EvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorsService;
import ru.citeck.ecos.records2.evaluator.evaluators.GroupEvaluator;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;

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

        RecordEvaluatorsService evaluatorsService = factory.getRecordEvaluatorsService();
        evaluatorsService.register(this);

        EvaluatorDto evaluatorDto = new EvaluatorDto();
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
        evaluatorDto.getConfig().set("attribute", TextNode.valueOf("unknown_field"));
        assertFalse(evaluatorsService.evaluate(meta0Ref, evaluatorDto));

        EvaluatorDto groupEvaluator = groupEvaluator(
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
    }

    private EvaluatorDto groupEvaluator(GroupEvaluator.JoinType joinType, EvaluatorDto... evaluators) {

        EvaluatorDto groupEvaluator = new EvaluatorDto();
        groupEvaluator.setType("group");

        GroupEvaluator.Config groupConfig = new GroupEvaluator.Config();
        groupConfig.setJoinType(joinType);
        groupConfig.setEvaluators(Arrays.asList(evaluators));

        groupEvaluator.setConfig(objectMapper.valueToTree(groupConfig));

        return groupEvaluator;
    }

    private EvaluatorDto alwaysFalseEvaluator() {
        EvaluatorDto evaluatorDto = new EvaluatorDto();
        evaluatorDto.setType("false");
        return evaluatorDto;
    }

    private EvaluatorDto alwaysTrueEvaluator() {
        EvaluatorDto evaluatorDto = new EvaluatorDto();
        evaluatorDto.setType("true");
        return evaluatorDto;
    }

    private EvaluatorDto hasAttEvaluator(String att) {
        EvaluatorDto evaluatorDto = new EvaluatorDto();
        evaluatorDto.setType("has-attribute");
        ObjectNode config = JsonNodeFactory.instance.objectNode();
        config.set("attribute", TextNode.valueOf(att));
        evaluatorDto.setConfig(config);
        return evaluatorDto;
    }

    @Override
    public String getType() {
        return ID;
    }

    @Override
    public RequiredMeta getRequiredMeta(Object o) {
        return new RequiredMeta();
    }

    @Override
    public Class<RequiredMeta> getEvalMetaType() {
        return RequiredMeta.class;
    }

    @Override
    public Class<Object> getConfigType() {
        return null;
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
    }

    @Data
    public static final class Meta1 {

        private String field0 = "meta1field0";
        private String field1 = "meta1field1";
    }
}

