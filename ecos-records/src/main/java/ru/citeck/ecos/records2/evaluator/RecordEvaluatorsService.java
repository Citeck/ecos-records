package ru.citeck.ecos.records2.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;
import java.util.Map;

public interface RecordEvaluatorsService {

    boolean evaluate(RecordRef recordRef, EvaluatorDto evaluator);

    Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs, EvaluatorDto evaluator);

    Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs, List<EvaluatorDto> evaluators);

    Map<String, String> getRequiredMetaAttributes(EvaluatorDto evalDto);

    boolean evaluateWithMeta(EvaluatorDto evalDto, ObjectNode metaNode);

    void register(RecordEvaluator<?, ?, ?> evaluator);
}
