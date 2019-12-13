package ru.citeck.ecos.records2.evaluator;

/**
 * Evaluator interface to filter recordRefs by custom conditions.
 *
 * @see EvaluatorDto
 */
public interface RecordEvaluator<ReqMetaT, EvalMetaT, ConfigT> {

    /**
     * Get attributes which is required for Evaluator.
     * @param config evaluator configuration
     * @return Map&lt;String, String&gt; or meta class instance or null if attributes is not required
     */
    ReqMetaT getRequiredMeta(ConfigT config);

    /**
     * Meta type for conversion of received attributes. Used for type safety.
     * @return DTO class or null
     */
    Class<EvalMetaT> getEvalMetaType();

    /**
     * Evaluator config type. Used for type safety.
     * @return config class or null
     */
    Class<ConfigT> getConfigType();

    /**
     * Evaluate result by meta and config.
     * @param meta metadata received from recordRef
     * @param config evaluator config
     */
    boolean evaluate(EvalMetaT meta, ConfigT config);

    /**
     * Get evaluator ID
     */
    String getId();
}
