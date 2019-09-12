package ru.citeck.ecos.records2.meta;

import ru.citeck.ecos.records2.utils.ObjectKeyGenerator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AttributesMetaResolver {

    private static final Pattern SUBFIELDS_PATTERN = Pattern.compile("^([^{]+)\\{(.+)}$");

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
                path = convertAttToGqlFormat(path, "disp", false);
            }

            schema.append(path, 1, path.length());
            schema.append(",");
        });
        schema.setLength(schema.length() - 1);

        return new AttributesSchema(schema.toString(), keysMapping);
    }

    public String convertAttToGqlFormat(String att, String defaultScalar, boolean multiple) {

        if (att.startsWith(".")) {
            return att;
        }

        String fieldName = att;
        String scalarField = defaultScalar;

        Matcher subFieldsMatcher = SUBFIELDS_PATTERN.matcher(fieldName);
        String subFields = null;
        if (subFieldsMatcher.matches()) {
            fieldName = subFieldsMatcher.group(1);
            subFields = subFieldsMatcher.group(2);
        }

        int questionIdx = fieldName.indexOf('?');
        if (questionIdx >= 0) {
            scalarField = fieldName.substring(questionIdx + 1);
            fieldName = fieldName.substring(0, questionIdx);
        }

        if (fieldName.startsWith("#")) {

            if (scalarField == null) {
                throw new IllegalArgumentException("Illegal attribute: '" + att + "'");
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

            if (subFields != null) {

                List<String> innerAtts = Arrays.stream(subFields.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

                sb.append("{");

                int attsCounter = innerAtts.size() - 1;
                for (String innerAttWithAlias : innerAtts) {

                    int aliasDelimIdx = innerAttWithAlias.indexOf(':');
                    String alias;
                    String innerAtt;

                    if (aliasDelimIdx == -1) {
                        alias = getValidAlias(innerAttWithAlias);
                        innerAtt = innerAttWithAlias;
                    } else {
                        alias = innerAttWithAlias.substring(0, aliasDelimIdx).trim();
                        innerAtt = innerAttWithAlias.substring(aliasDelimIdx + 1).trim();
                    }

                    innerAtt = convertAttToGqlFormat(innerAtt, defaultScalar, false);
                    sb.append(alias).append(":").append(innerAtt.substring(1));

                    if (attsCounter-- > 0) {
                        sb.append(",");
                    }
                }

                sb.append("}");
                for (int i = 0; i < attsPath.length - 1; i++) {
                    sb.append("}");
                }

            } else if (scalarField != null) {
                sb.append("{").append(scalarField).append("}");
                for (int i = 0; i < attsPath.length - 1; i++) {
                    sb.append("}");
                }
            }

            return sb.toString();
        }
    }

    private String getValidAlias(String alias) {

        alias = alias.toLowerCase();

        int dotIdx = alias.indexOf('.');
        if (dotIdx > 0) {
            alias = alias.substring(0, dotIdx);
        }

        int scalarDelimIdx = alias.indexOf('?');
        if (scalarDelimIdx >= 0) {
            alias = alias.substring(0, scalarDelimIdx);
        }

        return alias.replaceAll("[^a-z0-9]", "_");
    }
}
