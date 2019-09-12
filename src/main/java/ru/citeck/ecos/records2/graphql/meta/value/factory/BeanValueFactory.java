package ru.citeck.ecos.records2.graphql.meta.value.factory;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.records2.graphql.meta.annotation.DisplayName;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.SimpleMetaEdge;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
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
        private String displayName;

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
        public String getDisplayName() {

            if (displayName != null) {
                return displayName;
            }

            PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(bean);

            for (PropertyDescriptor descriptor : descriptors) {

                Method readMethod = descriptor.getReadMethod();
                if (readMethod != null) {
                    DisplayName annotation = readMethod.getAnnotation(DisplayName.class);
                    if (annotation != null) {
                        try {
                            if (!String.class.equals(readMethod.getReturnType())) {
                                throw new IllegalStateException("DisplayName getter should return "
                                                                + "String value. bean: "  + bean);
                            }
                            if (readMethod.getParameters().length != 0) {
                                throw new IllegalStateException("DisplayName getter should not "
                                                                + "receive any parameters. bean: " + bean);
                            }
                            displayName = (String) readMethod.invoke(bean);
                            break;
                        } catch (Exception e) {
                            log.error("Can't get DisplayName", e);
                            break;
                        }
                    }
                }
            }

            if (displayName == null) {
                displayName = getString();
            }

            return displayName;
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
                log.debug("Property not found", e);
            }
            return null;
        }

        @Override
        public boolean has(String name) throws Exception {
            try {
                return PropertyUtils.getPropertyDescriptor(bean, name) != null;
            } catch (NoSuchMethodException e) {
                log.debug("Property not found", e);
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
                    log.debug("Descriptor not found", e);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            return descriptor;
        }
    }
}
