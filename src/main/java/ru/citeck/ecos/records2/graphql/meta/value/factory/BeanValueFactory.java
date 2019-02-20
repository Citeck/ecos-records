package ru.citeck.ecos.records2.graphql.meta.value.factory;

import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

public class BeanValueFactory implements MetaValueFactory<Object> {

    @Override
    public MetaValue getValue(Object value) {
        return new Value(value);
    }

    @Override
    public List<Class<?>> getValueTypes() {
        return Collections.singletonList(Object.class);
    }

    static class Value implements MetaValue {

        private final Object bean;

        public Value(Object bean) {
            this.bean = bean;
        }

        @Override
        public String getId() {
            try {
                Object id = PropertyUtils.getProperty(bean, "id");
                return id != null ? id.toString() : null;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }

        @Override
        public String getString() {
            return bean.toString();
        }

        @Override
        public Object getAttribute(String name) throws Exception {
            return PropertyUtils.getProperty(bean, name);
        }

        @Override
        public boolean has(String name) throws Exception {
            return PropertyUtils.getPropertyDescriptor(bean, name) != null;
        }

        @Override
        public Object getJson() {
            return bean;
        }
    }
}
