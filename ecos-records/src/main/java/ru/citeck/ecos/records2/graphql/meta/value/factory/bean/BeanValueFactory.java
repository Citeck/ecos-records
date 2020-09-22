package ru.citeck.ecos.records2.graphql.meta.value.factory.bean;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.SimpleMetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.factory.MetaValueFactory;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        private final BeanTypeContext typeCtx;

        Value(Object bean) {
            this.bean = bean;
            typeCtx = BeanTypeUtils.getTypeContext(bean.getClass());
        }

        @Override
        public String getId() {
            if (typeCtx.hasProperty(".id")) {
                return getAttWithType(".id", String.class);
            }
            try {
                Object id = PropertyUtils.getProperty(bean, "id");
                return id != null ? id.toString() : null;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }

        @Override
        public Double getDouble() {
            if (typeCtx.hasProperty(".num")) {
                return getAttWithType(".num", Double.class);
            }
            String str = getString();
            return str != null ? Double.parseDouble(str) : null;
        }

        @Override
        public Boolean getBool() {
            if (typeCtx.hasProperty(".bool")) {
                return getAttWithType(".bool", Boolean.class);
            }
            return Boolean.parseBoolean(getString());
        }

        @Override
        public RecordRef getRecordType() {
            return getAttWithType(".type", RecordRef.class);
        }

        @Override
        public String getDisplayName() {
            if (typeCtx.hasProperty(".disp")) {
                return getAttWithType(".disp", String.class);
            }
            return getString();
        }

        @Override
        public String getString() {
            if (typeCtx.hasProperty(".str")) {
                return getAttWithType(".str", String.class);
            }
            return bean.toString();
        }

        @Override
        public Object getAttribute(@NotNull String name, @NotNull MetaField field) throws Exception {
            if (bean instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> beanAsMap = (Map<String, Object>) bean;
                return beanAsMap.get(name);
            }
            return typeCtx.getProperty(bean, name);
        }

        @Override
        public boolean has(@NotNull String name) {
            return typeCtx.hasProperty(name);
        }

        @Override
        public Object getJson() {
            if (typeCtx.hasProperty(".json")) {
                return getAttWithType(".json", DataValue.class);
            }
            return Json.getMapper().toJson(bean);
        }

        Object getBean() {
            return bean;
        }

        @Override
        public MetaEdge getEdge(@NotNull String name, @NotNull MetaField field) {
            return new BeanEdge(name, this);
        }

        private <T> T getAttWithType(String name, Class<T> type) {
            try {
                return Json.getMapper().convert(getAttribute(name, EmptyMetaField.INSTANCE), type);
            } catch (Exception e) {
                return null;
            }
        }
    }

    static class BeanEdge extends SimpleMetaEdge {

        private final Object bean;
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

                ParameterizedType parameterType = (ParameterizedType) returnType;
                return (Class<?>) parameterType.getActualTypeArguments()[0];
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
                    ExceptionUtils.throwException(e);
                    throw new RuntimeException(e); // never executed
                }
            }
            return descriptor;
        }
    }
}
