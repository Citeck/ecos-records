package ru.citeck.ecos.records2.utils;

import ecos.com.fasterxml.jackson210.databind.type.TypeFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectUtils {

    public static Class<?> getGenericClassArg(Class<?> type, Class<?> genericType) {
        List<Class<?>> args = getGenericClassArgs(type, genericType);
        if (args.size() > 0) {
            return args.get(0);
        }
        return null;
    }

    public static List<Class<?>> getGenericClassArgs(Class<?> type, Class<?> genericType) {

        return getGenericArgs(type, genericType)
            .stream()
            .map(TypeFactory::rawClass)
            .collect(Collectors.toList());
    }

    public static List<Type> getGenericArgs(Class<?> type, Class<?> genericType) {

        MandatoryParam.check("type", type);
        MandatoryParam.check("genericType", genericType);

        if (type.equals(genericType)) {
            return Collections.emptyList();
        }

        return getGenericArguments(type, genericType);
    }

    private static List<Type> getGenericArguments(Type type, Class<?> genericType) {

        if (type == null) {
            return Collections.emptyList();
        }

        if (type instanceof ParameterizedType && genericType.equals(((ParameterizedType) type).getRawType())) {

            ParameterizedType paramType = (ParameterizedType) type;
            return Arrays.asList(paramType.getActualTypeArguments());

        } else if (type instanceof Class) {

            Class clazz = (Class) type;
            if (genericType.isInterface()) {
                for (Type interfaceType : clazz.getGenericInterfaces()) {
                    if (interfaceType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
                        if (genericType.equals(parameterizedType.getRawType())) {
                            return Arrays.asList(parameterizedType.getActualTypeArguments());
                        }
                    }
                }
            }

            return getGenericArguments(((Class) type).getGenericSuperclass(), genericType);
        }

        return Collections.emptyList();
    }
}
