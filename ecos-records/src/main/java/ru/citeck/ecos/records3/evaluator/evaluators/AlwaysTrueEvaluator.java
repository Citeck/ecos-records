package ru.citeck.ecos.records3.evaluator.evaluators;

import ru.citeck.ecos.records3.evaluator.RecordEvaluator;

public class AlwaysTrueEvaluator implements RecordEvaluator<Object, Object, Object> {

    public static final String TYPE = "true";

    @Override
    public boolean evaluate(Object config, Object meta) {
        return true;
    }

    @Override
    public Object getMetaToRequest(Object config) {
        return null;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
