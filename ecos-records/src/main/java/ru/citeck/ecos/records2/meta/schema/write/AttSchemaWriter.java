package ru.citeck.ecos.records2.meta.schema.write;

import ru.citeck.ecos.records2.meta.schema.AttsSchema;
import ru.citeck.ecos.records2.meta.schema.GqlKeyUtils;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttSchemaWriter {

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

        String alias = attribute.getAlias();
        String name = attribute.getName();

        if (sb.length() > 0 && !alias.isEmpty()) {
            sb.append(GqlKeyUtils.escape(alias)).append(":");
        }
        if (name.startsWith(".")) {
            sb.append(name.substring(1));
            return;
        }
        sb.append("att");
        if (attribute.isMultiple()) {
            sb.append("s");
        }
        sb.append("(n:\"")
            .append(name)
            .append("\"){");

        List<SchemaAtt> inner = attribute.getInner();
        for (int i = 0; i < inner.size(); i++) {
            writeToString(inner.get(i), sb);
            if (i < inner.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("}");
    }
}
