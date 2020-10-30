package ru.citeck.ecos.records2.evaluator;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.evaluator.details.EvalDetails;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;

import java.util.List;
import java.util.Map;

public interface RecordEvaluatorService {

    boolean evaluate(RecordRef recordRef,
                     RecordEvaluatorDto evaluator);

    Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs,
                                     RecordEvaluatorDto evaluator);

    Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs,
                                           List<RecordEvaluatorDto> evaluators);

    EvalDetails evalWithDetails(RecordRef recordRef,
                                RecordEvaluatorDto evaluator);

    Map<RecordRef, EvalDetails> evalWithDetails(List<RecordRef> recordRefs,
                                                RecordEvaluatorDto evaluator);

    Map<RecordRef, List<EvalDetails>> evalWithDetails(List<RecordRef> recordRefs,
                                                      List<RecordEvaluatorDto> evaluators);

    Map<String, String> getRequiredMetaAttributes(RecordEvaluatorDto evalDto);

    boolean evaluateWithMeta(RecordEvaluatorDto evalDto, RecordAtts fullRecordMeta);

    EvalDetails evalDetailsWithMeta(RecordEvaluatorDto evalDto, RecordAtts fullRecordMeta);

    void register(RecordEvaluator<?, ?, ?> evaluator);
}
