package ru.citeck.ecos.records3.evaluator.details;

import ru.citeck.ecos.records3.evaluator.RecordEvaluator;
import ru.citeck.ecos.records3.evaluator.RecordEvaluatorDto;

/**
 * Evaluator interface to check recordRef by custom conditions with additional info about result.
 *
 * @see RecordEvaluatorDto
 */
public interface RecordEvaluatorWithDetails<ReqMetaT, ResMetaT, ConfigT>
    extends RecordEvaluator<ReqMetaT, ResMetaT, ConfigT> {

    @Override
    default boolean evaluate(ResMetaT meta, ConfigT config) {
        EvalDetails result = evalWithDetails(meta, config);
        return result != null && result.getResult();
    }

    /**
     * Evaluate result by meta and config.
     *
     * @param meta metadata received from recordRef
     * @param config evaluator config
     */
    EvalDetails evalWithDetails(ResMetaT meta, ConfigT config);
}
