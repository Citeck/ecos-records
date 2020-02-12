package ru.citeck.ecos.records2.evaluator;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;
import java.util.Map;

public interface RecordEvaluatorService {

    boolean evaluate(RecordRef recordRef,
                     RecordEvaluatorDto evaluator);

    boolean evaluate(RecordRef recordRef,
                     RecordEvaluatorDto evaluator,
                     Object model);

    Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs,
                                     RecordEvaluatorDto evaluator);

    Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs,
                                     RecordEvaluatorDto evaluator,
                                     Object model);

    Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs,
                                           List<RecordEvaluatorDto> evaluators);

    Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs,
                                           List<RecordEvaluatorDto> evaluators,
                                           Object model);

    Map<String, String> getRequiredMetaAttributes(RecordEvaluatorDto evalDto);

    boolean evaluateWithMeta(RecordEvaluatorDto evalDto, RecordMeta fullRecordMeta);

    void register(RecordEvaluator<?, ?, ?> evaluator);
}
