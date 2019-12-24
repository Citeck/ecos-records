package ru.citeck.ecos.records2.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;
import java.util.Map;

public interface RecordEvaluatorService {

    boolean evaluate(RecordRef recordRef, RecordEvaluatorDto evaluator);

    Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs, RecordEvaluatorDto evaluator);

    Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs, List<RecordEvaluatorDto> evaluators);

    Map<String, String> getRequiredMetaAttributes(RecordEvaluatorDto evalDto);

    boolean evaluateWithMeta(RecordEvaluatorDto evalDto, ObjectNode metaNode);

    void register(RecordEvaluator<?, ?, ?> evaluator);
}
