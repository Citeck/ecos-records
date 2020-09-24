package ru.citeck.ecos.records3.record.op.meta.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AttModelUtils {

    private static final Pattern MODEL_ATT_PATTERN = Pattern.compile("^(\\.atts?\\((n:)?['\"])?\\$.+");

    public static RecordModelAtts splitModelAttributes(Collection<String> attributes) {
        Map<String, String> atts = new HashMap<>();
        attributes.forEach(att -> atts.put(att, att));
        return splitModelAttributes(atts);
    }

    public static RecordModelAtts splitModelAttributes(Map<String, String> attributes) {

        Map<String, String> modelAtts = new HashMap<>();
        Map<String, String> recordAtts = new HashMap<>();

        attributes.forEach((k, v) -> {

            if (v.indexOf('$') >= 0 && MODEL_ATT_PATTERN.matcher(v).matches()) {
                modelAtts.put(k, v.replaceFirst("\\$", ""));
            } else {
                recordAtts.put(k, v);
            }
        });

        return new RecordModelAtts(recordAtts, modelAtts);
    }
}
