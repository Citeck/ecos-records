package ru.citeck.ecos.records2.evaluator.evaluators;

import ru.citeck.ecos.records2.evaluator.RecordEvaluator;

public class AlwaysTrueEvaluator implements RecordEvaluator<Object, Object, Object> {

    public static final String ID = "true";

    @Override
    public boolean evaluate(Object config, Object meta) {
        return true;
    }

    @Override
    public Object getRequiredMeta(Object config) {
        return null;
    }

    @Override
    public Class<Object> getEvalMetaType() {
        return null;
    }

    @Override
    public Class<Object> getConfigType() {
        return null;
    }

    @Override
    public String getId() {
        return ID;
    }
}
