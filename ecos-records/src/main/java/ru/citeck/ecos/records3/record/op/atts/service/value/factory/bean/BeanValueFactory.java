package ru.citeck.ecos.records3.record.op.atts.service.value.factory.bean;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.SimpleAttEdge;
import ru.citeck.ecos.records3.record.op.atts.service.value.factory.AttValueFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class BeanValueFactory implements AttValueFactory<Object> {

    @Override
    public AttValue getValue(Object value) {
        return new Value(value);
    }

    @Override
    public List<Class<?>> getValueTypes() {
        return Collections.singletonList(Object.class);
    }

    static class Value implements AttValue {

        private final Object bean;
        private final BeanTypeContext typeCtx;

        Value(Object bean) {
            this.bean = bean;
            typeCtx = BeanTypeUtils.getTypeContext(bean.getClass());
        }

        @Override
        public String getId() {
            if (typeCtx.hasProperty("?id")) {
                return getAttWithType("?id", String.class);
            }
            try {
                Object id = PropertyUtils.getProperty(bean, "id");
                return id != null ? id.toString() : null;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }

        @Override
        public Double asDouble() {
            if (typeCtx.hasProperty("?num")) {
                return getAttWithType("?num", Double.class);
            }
            String str = asText();
            return str != null ? Double.parseDouble(str) : null;
        }

        @Override
        public Boolean asBool() {
            if (typeCtx.hasProperty("?bool")) {
                return getAttWithType("?bool", Boolean.class);
            }
            return Boolean.parseBoolean(asText());
        }

        @Override
        public RecordRef getType() {
            return getAttWithType("?type", RecordRef.class);
        }

        @Override
        public String getDispName() {
            if (typeCtx.hasProperty("?disp")) {
                return getAttWithType("?disp", String.class);
            }
            return asText();
        }

        @Override
        public String asText() {
            if (typeCtx.hasProperty("?str")) {
                return getAttWithType("?str", String.class);
            }
            return bean.toString();
        }

        @Override
        public Object getAtt(@NotNull String name) throws Exception {
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
        public Object asJson() {
            if (typeCtx.hasProperty("?json")) {
                return getAttWithType("?json", DataValue.class);
            }
            return Json.getMapper().toJson(bean);
        }

        Object getBean() {
            return bean;
        }

        @Override
        public AttEdge getEdge(@NotNull String name) {
            return new BeanEdge(name, this);
        }

        private <T> T getAttWithType(String name, Class<T> type) {
            try {
                return Json.getMapper().convert(getAtt(name), type);
            } catch (Exception e) {
                return null;
            }
        }
    }

    static class BeanEdge extends SimpleAttEdge {

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
