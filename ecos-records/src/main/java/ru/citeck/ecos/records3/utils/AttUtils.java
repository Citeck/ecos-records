package ru.citeck.ecos.records3.utils;

import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AttUtils {

    public static Map<String, String> toMap(Collection<String> attributes) {

        Map<String, String> attributesMap = new LinkedHashMap<>();
        for (String attribute : attributes) {
            attributesMap.put(attribute, attribute);
        }
        return attributesMap;
    }

    public static SchemaAtt removeProcessors(SchemaAtt att) {
        if (att.getProcessors().isEmpty() && att.getInner().isEmpty()) {
            return att;
        }
        return att.copy()
            .setProcessors(Collections.emptyList())
            .setInner(att.getInner()
                .stream()
                .map(AttUtils::removeProcessors)
                .collect(Collectors.toList()))
            .build();
    }
}
