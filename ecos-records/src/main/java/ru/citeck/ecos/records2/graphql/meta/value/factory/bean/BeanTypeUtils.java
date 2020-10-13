package ru.citeck.ecos.records2.graphql.meta.value.factory.bean;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BeanTypeUtils {

    private static final Map<Class<?>, BeanTypeContext> typeCtxCache = new ConcurrentHashMap<>();

    public static BeanTypeContext getTypeContext(Class<?> type) {
        return typeCtxCache.computeIfAbsent(type, t -> new BeanTypeContext(getGetters(t)));
    }

    private static Map<String, UncheckedFunction<Object, Object>> getGetters(Class<?> type) {

        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(type);
        Map<String, UncheckedFunction<Object, Object>> getters = new ConcurrentHashMap<>();

        for (PropertyDescriptor descriptor : descriptors) {

            Method readMethod = descriptor.getReadMethod();
            if (readMethod == null) {
                continue;
            }
            readMethod.setAccessible(true);
            UncheckedFunction<Object, Object> getter = bean -> readMethod.invoke(bean);

            getters.put(descriptor.getName(), getter);

            MetaAtt metaAttAnn = getReadAnnotation(type, descriptor, MetaAtt.class);
            if (metaAttAnn != null) {
                String metaAttName = metaAttAnn.value();
                if (metaAttName.contains("?")) {
                    metaAttName = metaAttName.substring(0, metaAttName.indexOf('?'));
                }
                getters.put(metaAttName, getter);
            }
        }
        return getters;
    }

    private static <T extends Annotation> T getReadAnnotation(Class<?> scope,
                                                              PropertyDescriptor descriptor,
                                                              Class<T> type) {

        Method readMethod = descriptor.getReadMethod();
        T annotation = readMethod.getAnnotation(type);
        if (annotation == null) {
            Class<?> scopeIt = scope;
            while (scopeIt != null) {
                try {
                    Field field = scopeIt.getDeclaredField(descriptor.getName());
                    annotation = field.getAnnotation(type);
                    break;
                } catch (Exception e) {
                    annotation = null;
                }
                scopeIt = scopeIt.getSuperclass();
            }
        }
        return annotation;
    }
}
