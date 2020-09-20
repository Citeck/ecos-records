package ru.citeck.ecos.records2.meta.schema.write;

import ru.citeck.ecos.records2.meta.schema.AttsSchema;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttsSchemaWriter {

    private static final String KEYS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public Map<String, String> writeToMap(AttsSchema schema) {

        Map<String, String> result = new HashMap<>();
        schema.getSchema().forEach(att ->
            result.put(att.getAlias(), writeToString(att))
        );
        return result;
    }

    public String writeToString(SchemaAtt attribute) {
        StringBuilder sb = new StringBuilder();
        writeToString(attribute, sb);
        return "." + sb.toString();
    }

    private void writeToString(SchemaAtt attribute, StringBuilder sb) {
        writeToString(attribute, sb, 0);
    }

    private void writeToString(SchemaAtt attribute, StringBuilder sb, int idx) {

        if (sb.length() > 0) {
            String alias = attribute.getAlias();
            if (alias.length() == 0) {
                alias = "" + KEYS.charAt(idx);
            }
            sb.append(alias).append(":");
        }
        if (attribute.getName().startsWith(".")) {
            sb.append(attribute.getName().substring(1));
            return;
        }
        sb.append("att");
        if (attribute.isMultiple()) {
            sb.append("s");
        }
        sb.append("(n:\"")
            .append(attribute.getName())
            .append("\"){");

        List<SchemaAtt> inner = attribute.getInner();
        for (int i = 0; i < inner.size(); i++) {
            writeToString(inner.get(i), sb, i);
            if (i < inner.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("}");
    }
}
