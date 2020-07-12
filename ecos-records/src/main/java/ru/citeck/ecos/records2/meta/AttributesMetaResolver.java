package ru.citeck.ecos.records2.meta;

import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.ObjectKeyGenerator;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.meta.attproc.AttProcessorDef;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AttributesMetaResolver {

    private static final String ATT_TYPE = ".type";
    private static final Pattern SUBFIELDS_PATTERN = Pattern.compile("^([^{]+)\\{(.+)}$");
    private static final Pattern PROCESSOR_PATTERN = Pattern.compile("(.+)\\((.*)\\)");

    public AttributesSchema createSchema(Map<String, String> attributes) {
        return createSchema(attributes, true);
    }

    public AttributesSchema createSchema(Map<String, String> attributes, boolean generateKeys) {

        if (attributes.isEmpty()) {
            return new AttributesSchema("", Collections.emptyMap());
        }

        StringBuilder schema = new StringBuilder();
        ObjectKeyGenerator keys = new ObjectKeyGenerator();

        Map<String, AttSchemaInfo> attsInfo = new HashMap<>();

        attributes.forEach((name, path) -> {

            AttSchemaInfo attSchemaInfo = new AttSchemaInfo();
            attSchemaInfo.setOriginalKey(name);

            int pipeDelimIdx = AttStrUtils.indexOf(path, '|');
            if (pipeDelimIdx > 0) {
                attSchemaInfo.setProcessors(parseProcessors(path.substring(pipeDelimIdx + 1)));
                path = path.substring(0, pipeDelimIdx);
            }
            List<String> orElseAtts = AttStrUtils.split(path, '!');
            if (orElseAtts.size() > 1) {
                List<String> orElseAttKeys = new ArrayList<>();
                for (int i = 1; i < orElseAtts.size(); i++) {
                    String orElseAtt = orElseAtts.get(i).trim();
                    if (orElseAtt.charAt(0) != '\'' && orElseAtt.charAt(0) != '"') {
                        String key = keys.incrementAndGet();
                        orElseAttKeys.add(key);
                        addAttToSchema(key, orElseAtt, schema);
                    } else {
                        orElseAttKeys.add(orElseAtt);
                    }
                }
                attSchemaInfo.setOrElseAtts(orElseAttKeys);
                path = orElseAtts.get(0);
            }

            String key = generateKeys ? keys.incrementAndGet() : name;
            attsInfo.put(key, attSchemaInfo);

            attSchemaInfo.setType(addAttToSchema(key, path, schema));
        });
        schema.setLength(schema.length() - 1);

        return new AttributesSchema(schema.toString(), attsInfo);
    }

    private Class<?> addAttToSchema(String key, String attribute, StringBuilder schema) {

        schema.append(key).append(":");

        if (attribute.charAt(0) != '.') {
            attribute = convertAttToGqlFormat(attribute, "disp", false);
        }
        attribute = attribute.replace("'", "\"");
        attribute = attribute.replace("(\"", "(n:\"");

        schema.append(attribute, 1, attribute.length());
        schema.append(",");

        if (attribute.contains("{bool}") || attribute.contains("has(")) {
            return Boolean.class;
        } else if (attribute.contains("{num}")) {
            return Double.class;
        } else if (attribute.contains("{str}")
                || attribute.contains("{disp}")
                || attribute.contains("{assoc}")
                || attribute.contains("{id}")) {

            return String.class;
        }
        return null;
    }

    private List<AttProcessorDef> parseProcessors(String processorStr) {
        if (StringUtils.isBlank(processorStr)) {
            return Collections.emptyList();
        }
        List<AttProcessorDef> result = new ArrayList<>();

        for (String proc : AttStrUtils.split(processorStr, '|')) {
            Matcher matcher = PROCESSOR_PATTERN.matcher(proc);
            if (matcher.matches()) {
                String type = matcher.group(1).trim();
                List<DataValue> args = parseArgs(matcher.group(2).trim());
                result.add(new AttProcessorDef(type, args));
            }
        }
        return result;
    }

    private List<DataValue> parseArgs(String argsStr) {

        List<DataValue> result = new ArrayList<>();

        if (StringUtils.isBlank(argsStr)) {
            return result;
        }

        argsStr = argsStr.replace("'", "\"");
        AttStrUtils.split(argsStr, ",").stream()
            .map(String::trim)
            .map(DataValue::create)
            .forEach(result::add);

        return result;
    }

    private String getValidSchemaParamName(String name) {
        return StringUtils.escapeDoubleQuotes(name);
    }

    public String convertAttToGqlFormat(String att, String defaultScalar, boolean multiple) {

        if (att.startsWith(ATT_TYPE)) {
            if (att.length() <= ATT_TYPE.length() + 1) {
                return ATT_TYPE + "{id}";
            } else if (att.equals(ATT_TYPE + ".disp")) {
                return ATT_TYPE + "{disp}";
            }
        }

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

            String attName = getValidSchemaParamName(fieldName.substring(1));
            return ".edge(n:\"" + attName + "\"){" + scalarField + inner + "}";

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
                sb.append("(n:\"").append(getValidSchemaParamName(pathElem)).append("\")");
            }

            if (subFields != null) {

                List<String> innerAtts = Arrays.stream(subFields.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

                sb.append("{");

                int attsCounter = innerAtts.size() - 1;
                for (String innerAttWithAlias : innerAtts) {

                    String alias = null;
                    String innerAtt = null;

                    if (innerAttWithAlias.charAt(0) == '.') {

                        int paramNameIdx = innerAttWithAlias.indexOf("(n:\"");
                        if (paramNameIdx > 0) {
                            int paramNameEndIdx = innerAttWithAlias.indexOf('"', paramNameIdx + 4);
                            if (paramNameEndIdx != -1) {
                                String prefix = innerAttWithAlias.substring(1, paramNameIdx);
                                String paramName = innerAttWithAlias.substring(paramNameIdx + 4, paramNameEndIdx);
                                alias = getValidAlias(prefix + "_" + paramName);
                                innerAtt = innerAttWithAlias;
                            }
                        }

                    } else {
                        int aliasDelimIdx = innerAttWithAlias.indexOf(':');
                        if (aliasDelimIdx != -1) {
                            alias = innerAttWithAlias.substring(0, aliasDelimIdx).trim();
                            innerAtt = innerAttWithAlias.substring(aliasDelimIdx + 1).trim();
                        }
                    }
                    if (alias == null) {
                        alias = getValidAlias(innerAttWithAlias);
                        innerAtt = innerAttWithAlias;
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
                sb.append("{");
                int dotIdx = AttStrUtils.indexOf(scalarField, '.');
                if (dotIdx > 0) {
                    sb.append(scalarField.substring(0, dotIdx)).append("{");
                    String innerAtt = convertAttToGqlFormat(scalarField.substring(dotIdx + 1), defaultScalar, multiple);
                    sb.append(innerAtt, 1, innerAtt.length()).append("}}");
                } else {
                    sb.append(scalarField).append("}");
                }
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
