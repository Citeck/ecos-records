package ru.citeck.ecos.records2.graphql.meta.value.factory;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.SimpleMetaEdge;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BeanValueFactory implements MetaValueFactory<Object> {

    private static final Log logger = LogFactory.getLog(BeanValueFactory.class);

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

        Value(Object bean) {
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
        public Object getAttribute(String name, MetaField field) throws Exception {
            try {
                return PropertyUtils.getProperty(bean, name);
            } catch (NoSuchMethodException e) {
                logger.debug("Property not found", e);
            }
            return null;
        }

        @Override
        public boolean has(String name) throws Exception {
            try {
                return PropertyUtils.getPropertyDescriptor(bean, name) != null;
            } catch (NoSuchMethodException e) {
                logger.debug("Property not found", e);
            }
            return false;
        }

        @Override
        public Object getJson() {
            return bean;
        }

        Object getBean() {
            return bean;
        }

        @Override
        public MetaEdge getEdge(String name, MetaField field) {
            return new BeanEdge(name, this);
        }
    }

    static class BeanEdge extends SimpleMetaEdge {

        private Object bean;
        private PropertyDescriptor descriptor;

        BeanEdge(String name, Value scope) {
            super(name, scope);
            bean = scope.getBean();
        }

        @Override
        public boolean isMultiple() {
            return Collection.class.isAssignableFrom(getDescriptor().getPropertyType());
        }

        @Override
        public Class<?> getJavaClass() {

            PropertyDescriptor descriptor = getDescriptor();

            if (descriptor == null) {
                return null;
            }

            Class<?> type = descriptor.getPropertyType();

            if (Collection.class.isAssignableFrom(type)) {

                Type returnType = descriptor.getReadMethod().getGenericReturnType();

                if (returnType != null) {

                    ParameterizedType parameterType = (ParameterizedType) returnType;
                    return (Class<?>) parameterType.getActualTypeArguments()[0];
                }
            }

            return type;
        }

        private PropertyDescriptor getDescriptor() {
            if (descriptor == null) {
                try {
                    descriptor = PropertyUtils.getPropertyDescriptor(bean, getName());
                } catch (NoSuchMethodException e) {
                    logger.debug("Descriptor not found", e);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            return descriptor;
        }
    }
}
