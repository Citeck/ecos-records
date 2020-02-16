package ru.citeck.ecos.records2.utils;

public class LibsUtils {

    private static final String JACKSON_CHECK_CLASS = "com.fasterxml.jackson.databind.JsonNode";

    private static Boolean isJacksonPresent;

    public static boolean isJacksonPresent() {
        if (isJacksonPresent == null) {
            isJacksonPresent = isClassPresent(JACKSON_CHECK_CLASS);
        }
        return isJacksonPresent;
    }

    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
