package ru.citeck.ecos.records2.evaluator;

/**
 * Evaluator interface to filter recordRefs by custom conditions.
 *
 * @see RecordEvaluatorDto
 */
public interface RecordEvaluator<ReqMetaT, ResMetaT, ConfigT> {

    /**
     * Get attributes which is required for Evaluator.
     *
     * @param config evaluator configuration
     * @return Map&lt;String, String&gt; or meta class instance or null if attributes is not required
     */
    ReqMetaT getMetaToRequest(ConfigT config);

    /**
     * Evaluate result by meta and config.
     *
     * @param meta metadata received from recordRef
     * @param config evaluator config
     */
    boolean evaluate(ResMetaT meta, ConfigT config);

    /**
     * Get evaluator type.
     *
     * @see RecordEvaluatorDto#getType
     */
    String getType();
}
