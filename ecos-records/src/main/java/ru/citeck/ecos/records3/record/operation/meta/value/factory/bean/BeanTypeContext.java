package ru.citeck.ecos.records3.record.operation.meta.value.factory.bean;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;

import java.util.Map;

@Slf4j
public class BeanTypeContext {

    private final Map<String, UncheckedFunction<Object, Object>> getters;

    protected BeanTypeContext(Map<String, UncheckedFunction<Object, Object>> getters) {
        this.getters = getters;
    }

    public boolean hasProperty(String name) {
        return getters.containsKey(name);
    }

    public Object getProperty(Object bean, String name) throws Exception {
        if (bean == null) {
            return null;
        }
        if (getters.containsKey(name)) {
            return getters.get(name).apply(bean);
        } else {
            log.debug("Property not found: " + name + " in type " + bean.getClass());
        }
        return null;
    }
}
