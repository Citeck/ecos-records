package ru.citeck.ecos.records2.utils;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public class MandatoryParam {

    public static <T> void check(String name, T value, Function<T, Boolean> validator) {
        if (!validator.apply(value)) {
            throw new IllegalArgumentException(name + " is a mandatory parameter");
        }
    }

    public static void check(String name, Object value) {
        check(name, value, Objects::nonNull);
    }

    public static void checkString(String name, CharSequence value) {
        check(name, value, StringUtils::isNotBlank);
    }

    public static void checkCollection(String name, Collection value) {
        check(name, value);
        if (value.size() == 0) {
            throw new IllegalArgumentException(name + " collection must contain at least one item");
        }
    }
}
