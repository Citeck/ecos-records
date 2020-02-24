package ru.citeck.ecos.records2.evaluator;

import lombok.Data;
import ru.citeck.ecos.commons.utils.ReflectUtils;

import java.util.List;

@Data
public class ParameterizedRecordEvaluator implements RecordEvaluator<Object, Object, Object> {

    private RecordEvaluator<Object, Object, Object> impl;

    private final Class<?> reqMetaType;
    private final Class<?> resMetaType;
    private final Class<?> configType;

    @SuppressWarnings("unchecked")
    public ParameterizedRecordEvaluator(RecordEvaluator<?, ?, ?> impl) {

        this.impl = (RecordEvaluator<Object, Object, Object>) impl;

        List<Class<?>> genericArgs = ReflectUtils.getGenericArgs(impl.getClass(), RecordEvaluator.class);
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
    public String getType() {
        return impl.getType();
    }
}
