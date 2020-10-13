package ru.citeck.ecos.records2.evaluator;

import lombok.Data;
import ru.citeck.ecos.commons.utils.ReflectUtils;
import ru.citeck.ecos.records2.evaluator.details.EvalDetails;
import ru.citeck.ecos.records2.evaluator.details.EvalDetailsImpl;
import ru.citeck.ecos.records2.evaluator.details.RecordEvaluatorWithDetails;

import java.util.Collections;
import java.util.List;

@Data
public class ParameterizedRecordEvaluator implements RecordEvaluatorWithDetails<Object, Object, Object> {

    private RecordEvaluatorWithDetails<Object, Object, Object> impl;

    private final Class<?> reqMetaType;
    private final Class<?> resMetaType;
    private final Class<?> configType;

    @SuppressWarnings("unchecked")
    public ParameterizedRecordEvaluator(RecordEvaluator<?, ?, ?> impl) {

        List<Class<?>> genericArgs;

        if (impl instanceof RecordEvaluatorWithDetails) {
            this.impl = (RecordEvaluatorWithDetails<Object, Object, Object>) impl;
            genericArgs = ReflectUtils.getGenericArgs(impl.getClass(), RecordEvaluatorWithDetails.class);
        } else {
            this.impl = new EvaluatorWrapperWithDetails(impl);
            genericArgs = ReflectUtils.getGenericArgs(impl.getClass(), RecordEvaluator.class);
        }

        if (genericArgs.size() != 3) {
            throw new IllegalArgumentException("Incorrect evaluator: [" + impl.getClass() + "] " + impl);
        }

        reqMetaType = genericArgs.get(0);
        resMetaType = genericArgs.get(1);
        configType = genericArgs.get(2);
    }

    @Override
    public Object getMetaToRequest(Object config) {
        return impl.getMetaToRequest(config);
    }

    @Override
    public boolean evaluate(Object meta, Object config) {
        return impl.evaluate(meta, config);
    }

    @Override
    public EvalDetails evalWithDetails(Object meta, Object config) {
        return impl.evalWithDetails(meta, config);
    }

    @Override
    public String getType() {
        return impl.getType();
    }

    private static class EvaluatorWrapperWithDetails implements RecordEvaluatorWithDetails<Object, Object, Object> {

        RecordEvaluator<Object, Object, Object> impl;

        EvaluatorWrapperWithDetails(RecordEvaluator<?, ?, ?> impl) {
            @SuppressWarnings("unchecked")
            RecordEvaluator<Object, Object, Object> evaluator = (RecordEvaluator<Object, Object, Object>) impl;
            this.impl = evaluator;
        }

        @Override
        public EvalDetails evalWithDetails(Object meta, Object config) {
            boolean result = impl.evaluate(meta, config);
            return new EvalDetailsImpl(result, Collections.emptyList());
        }

        @Override
        public Object getMetaToRequest(Object config) {
            return impl.getMetaToRequest(config);
        }

        @Override
        public String getType() {
            return impl.getType();
        }
    }
}
