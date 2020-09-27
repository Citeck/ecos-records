package ru.citeck.ecos.records3.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AttUtils {

    public static Map<String, String> toMap(Collection<String> attributes) {

        Map<String, String> attributesMap = new HashMap<>();
        for (String attribute : attributes) {
            attributesMap.put(attribute, attribute);
        }
        return attributesMap;
    }
}
