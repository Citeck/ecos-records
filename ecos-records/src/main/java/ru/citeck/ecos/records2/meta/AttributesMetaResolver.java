package ru.citeck.ecos.records2.meta;

import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records2.meta.util.AttStrUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttributesMetaResolver {
/*

    public AttributesMetaResolver(RecordsServiceFactory serviceFactory) {
        this.recordsMetaGql = serviceFactory.getRecordsMetaGql();
    }

    public AttsSchema createAttsSchema(Map<String, String> attributes) {


    }*/

/*    public AttsSchemaImpl createSchema(Map<String, String> attributes, boolean generateKeys) {

        if (attributes.isEmpty()) {
            return new AttsSchemaImpl();
        }

        StringBuilder schema = new StringBuilder();
        ObjectKeyGenerator keys = new ObjectKeyGenerator();

        Map<String, AttSchemaInfoImpl> attsInfo = new HashMap<>();
        Map<String, String> schemaAtts = new HashMap<>();

        attributes.forEach((name, attribute) -> {

            String path = attribute;

            AttSchemaInfoImpl attSchemaInfo = new AttSchemaInfoImpl();
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
            schemaAtts.put(key, attribute);

            attSchemaInfo.setType(addAttToSchema(key, path, schema));
        });
        schema.setLength(schema.length() - 1);

        String strSchema = schema.toString();

        MetaField metaField = recordsMetaGql.getMetaFieldFromSchema(strSchema);

        return new AttsSchemaImpl(
            schema.toString(),
            attsInfo,
            metaField,
            schemaAtts
        );
    }*/

    private Class<?> getAttType(String attribute) {

        /*if (attribute.charAt(0) != '.') {
            attribute = convertAttToGqlFormat(attribute, "disp", false);
        }

        if (attribute.contains("{bool}") || attribute.contains("has(")) {
            return Boolean.class;
        } else if (attribute.contains("{num}")) {
            return Double.class;
        } else if (attribute.contains("{str}")
                || attribute.contains("{disp}")
                || attribute.contains("{assoc}")
                || attribute.contains("{id}")) {

            return String.class;
        }*/
        return null;
    }

    private String getValidSchemaParamName(String name) {
        return StringUtils.escapeDoubleQuotes(name);
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
