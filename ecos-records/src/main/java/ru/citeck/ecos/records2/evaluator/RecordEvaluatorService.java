package ru.citeck.ecos.records2.evaluator;

import ru.citeck.ecos.records2.evaluator.details.EvalDetails;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.Map;

public interface RecordEvaluatorService {

    boolean evaluate(EntityRef recordRef,
                     RecordEvaluatorDto evaluator);

    Map<EntityRef, Boolean> evaluate(List<EntityRef> recordRefs,
                                     RecordEvaluatorDto evaluator);

    Map<EntityRef, List<Boolean>> evaluate(List<EntityRef> recordRefs,
                                           List<RecordEvaluatorDto> evaluators);

    EvalDetails evalWithDetails(EntityRef recordRef,
                                RecordEvaluatorDto evaluator);

    Map<EntityRef, EvalDetails> evalWithDetails(List<EntityRef> recordRefs,
                                                RecordEvaluatorDto evaluator);

    Map<EntityRef, List<EvalDetails>> evalWithDetails(List<EntityRef> recordRefs,
                                                      List<RecordEvaluatorDto> evaluators);

    Map<String, String> getRequiredMetaAttributes(RecordEvaluatorDto evalDto);

    boolean evaluateWithMeta(RecordEvaluatorDto evalDto, RecordAtts fullRecordMeta);

    EvalDetails evalDetailsWithMeta(RecordEvaluatorDto evalDto, RecordAtts fullRecordMeta);

    void register(RecordEvaluator<?, ?, ?> evaluator);
}
