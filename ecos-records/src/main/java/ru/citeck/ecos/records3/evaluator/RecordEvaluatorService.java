package ru.citeck.ecos.records3.evaluator;

import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.evaluator.details.EvalDetails;

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

    EvalDetails evalWithDetails(RecordRef recordRef,
                                RecordEvaluatorDto evaluator);

    EvalDetails evalWithDetails(RecordRef recordRef,
                                RecordEvaluatorDto evaluator,
                                Object model);

    Map<RecordRef, EvalDetails> evalWithDetails(List<RecordRef> recordRefs,
                                                RecordEvaluatorDto evaluator);

    Map<RecordRef, EvalDetails> evalWithDetails(List<RecordRef> recordRefs,
                                                RecordEvaluatorDto evaluator,
                                                Object model);

    Map<RecordRef, List<EvalDetails>> evalWithDetails(List<RecordRef> recordRefs,
                                                      List<RecordEvaluatorDto> evaluators);

    Map<RecordRef, List<EvalDetails>> evalWithDetails(List<RecordRef> recordRefs,
                                                      List<RecordEvaluatorDto> evaluators,
                                                      Object model);

    Map<String, String> getRequiredMetaAttributes(RecordEvaluatorDto evalDto);

    boolean evaluateWithMeta(RecordEvaluatorDto evalDto, RecordAtts fullRecordAtts);

    EvalDetails evalDetailsWithMeta(RecordEvaluatorDto evalDto, RecordAtts fullRecordAtts);

    void register(RecordEvaluator<?, ?, ?> evaluator);
}
