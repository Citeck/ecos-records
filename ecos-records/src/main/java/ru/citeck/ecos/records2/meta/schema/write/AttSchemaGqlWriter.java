package ru.citeck.ecos.records2.meta.schema.write;

import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.meta.schema.GqlKeyUtils;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;
import ru.citeck.ecos.records2.meta.schema.SchemaRootAtt;

import java.util.List;

/**
 * Writer to convert schema to legacy GraphQL format.
 */
public class AttSchemaGqlWriter implements AttSchemaWriter {

    @Override
    public void write(SchemaAtt attribute, StringBuilder sb) {
        sb.append(".");
        writeInner(attribute, sb);
    }

    private void writeInner(SchemaAtt attribute, StringBuilder sb) {

        String alias = attribute.getAlias();
        String name = attribute.getName();
        List<SchemaAtt> inner = attribute.getInner();

        if (sb.length() > 0 && !alias.isEmpty()) {
            sb.append(GqlKeyUtils.escape(alias)).append(":");
        }

        char firstCharInName = name.charAt(0);

        if (firstCharInName == '.') {
            sb.append(name.substring(1));
            return;
        }
        if (firstCharInName == '_' && inner.size() == 1) {
            if (name.equals(RecordConstants.ATT_EDGE)) {
                writeEdge(attribute, sb);
                return;
            }
            if (name.equals(RecordConstants.ATT_AS)) {
                writeAs(attribute, sb);
                return;
            }
            if (name.equals(RecordConstants.ATT_HAS)) {
                writeHas(attribute, sb);
                return;
            }
        }

        sb.append("att");
        if (attribute.isMultiple()) {
            sb.append("s");
        }
        sb.append("(n:\"")
            .append(name)
            .append("\"){");

        for (int i = 0; i < inner.size(); i++) {
            writeInner(inner.get(i), sb);
            if (i < inner.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("}");
    }

    private void writeEdge(SchemaAtt attribute, StringBuilder sb) {

        SchemaAtt innerAtt = attribute.getInner().get(0);

        sb.append("edge(n:\"")
            .append(innerAtt.getName())
            .append("\"){");

        for (SchemaAtt att : innerAtt.getInner()) {
            writeEdgeInner(att, sb);
        }

        sb.append("}");
    }

    private void writeEdgeInner(SchemaAtt attribute, StringBuilder sb) {

        String alias = attribute.getAlias();
        if (!alias.isEmpty()) {
            sb.append(GqlKeyUtils.escape(alias)).append(":");
        }
        sb.append(attribute.getName());

        switch (attribute.getName()) {
            case "val":
            case "vals":
            case "options":
            case "distinct":
            case "createVariants":
                sb.append("{");
                for (SchemaAtt att : attribute.getInner()) {
                    writeInner(att, sb);
                }
                sb.append("}");
        }
    }

    private void writeAs(SchemaAtt attribute, StringBuilder sb) {

        SchemaAtt innerAtt = attribute.getInner().get(0);

        sb.append("as(n:\"")
            .append(innerAtt.getName())
            .append("\"){");

        for (SchemaAtt att : innerAtt.getInner()) {
            writeInner(att, sb);
        }

        sb.append("}");
    }

    private void writeHas(SchemaAtt attribute, StringBuilder sb) {
        sb.append("has(n:\"")
            .append(attribute.getInner().get(0).getName())
            .append("\")");;
    }
}
