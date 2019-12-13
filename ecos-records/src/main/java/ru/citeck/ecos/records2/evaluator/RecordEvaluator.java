package ru.citeck.ecos.records2.evaluator;

public interface RecordEvaluator<ReqMetaT, EvalMetaT, ConfigT> {

    ReqMetaT getRequiredMeta(ConfigT config);

    Class<EvalMetaT> getEvalMetaType();

    Class<ConfigT> getConfigType();

    boolean evaluate(EvalMetaT meta, ConfigT config);

    String getId();
}
