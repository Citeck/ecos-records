package ru.citeck.ecos.records2.meta;

import ru.citeck.ecos.records2.utils.ObjectKeyGenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AttributesMetaResolver {

    public AttributesSchema createSchema(Map<String, String> attributes) {
        return createSchema(attributes, true);
    }

    public AttributesSchema createSchema(Map<String, String> attributes, boolean generateKeys) {

        if (attributes.isEmpty()) {
            return new AttributesSchema("", Collections.emptyMap());
        }

        StringBuilder schema = new StringBuilder();
        ObjectKeyGenerator keys = new ObjectKeyGenerator();

        Map<String, String> keysMapping = new HashMap<>();

        attributes.forEach((name, path) -> {

            String key = generateKeys ? keys.incrementAndGet() : name;

            keysMapping.put(key, name);
            schema.append(key).append(":");

            if (path.charAt(0) != '.') {
                path = convertAttDefinition(path, "disp", false);
            }

            schema.append(path, 1, path.length());
            schema.append(",");
        });
        schema.setLength(schema.length() - 1);

        return new AttributesSchema(schema.toString(), keysMapping);
    }

    public String convertAttDefinition(String def, String defaultScalar, boolean multiple) {

        String fieldName = def;
        String scalarField = defaultScalar;

        int questionIdx = fieldName.indexOf('?');
        if (questionIdx >= 0) {
            scalarField = fieldName.substring(questionIdx + 1);
            fieldName = fieldName.substring(0, questionIdx);
        }

        if (fieldName.startsWith("#")) {

            if (scalarField == null) {
                throw new IllegalArgumentException("Illegal attribute: '" + def + "'");
            }

            String inner;
            switch (scalarField) {
                case "options":
                case "distinct":
                    inner = "{label:disp,value:str}";
                    break;
                case "createVariants":
                    inner = "{json}";
                    break;
                default:
                    inner = "";
            }

            return ".edge(n:\"" + fieldName.substring(1) + "\"){" + scalarField + inner + "}";

        } else {

            String[] attsPath = fieldName.split("(?<!\\\\)\\.");
            if (multiple && !fieldName.contains("[]")) {
                attsPath[0] = attsPath[0] + "[]";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < attsPath.length; i++) {
                if (i == 0) {
                    sb.append(".");
                } else {
                    sb.append("{");
                }
                sb.append("att");

                String pathElem = attsPath[i];
                if (pathElem.endsWith("[]")) {
                    sb.append("s");
                    pathElem = pathElem.substring(0, pathElem.length() - 2);
                }
                if (pathElem.contains("\\.")) {
                    pathElem = pathElem.replaceAll("\\\\.", ".");
                }
                sb.append("(n:\"").append(pathElem).append("\")");
            }

            if (scalarField != null) {
                sb.append("{").append(scalarField).append("}");
                for (int i = 0; i < attsPath.length - 1; i++) {
                    sb.append("}");
                }
            }

            return sb.toString();
        }
    }
}
