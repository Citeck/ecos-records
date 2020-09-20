package ru.citeck.ecos.records.test.schema;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.meta.schema.read.AttsSchemaReader;
import ru.citeck.ecos.records2.meta.schema.write.AttsSchemaWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaTest {

    private final AttsSchemaReader reader = new AttsSchemaReader();
    private final AttsSchemaWriter writer = new AttsSchemaWriter();

    @Test
    public void edgeGqlTest() {

        assertEdgeMetaVal("vals", true);
        assertEdgeMetaVal("val", false);
        assertEdgeMetaVal("options", true);
        assertEdgeMetaVal("distinct", true);
        assertEdgeMetaVal("createVariants", true);

        assertEdgeScalar("protected", "bool");
        assertEdgeScalar("canBeRead", "bool");
        assertEdgeScalar("multiple", "bool");
        assertEdgeScalar("isAssoc", "bool");

        assertEdgeScalar("name", "str");
        assertEdgeScalar("title", "str");
        assertEdgeScalar("description", "str");
        assertEdgeScalar("javaClass", "str");
        assertEdgeScalar("editorKey", "str");
        assertEdgeScalar("type", "str");

        assertAtt(".att(n:\"_edge\"){a:att(n:\"cm:name\"){a:att(n:\"protected\"){a:bool}}}",
            ".edge(n:\"cm:name\"){protected}");
    }

    private void assertEdgeMetaVal(String inner, boolean multiple) {
        String edgeName = "cm:name";
        String innerInner = "att(n:\"title\"){a:disp}";
        assertAtt(".att(n:\"_edge\"){a:att(n:\"" + edgeName + "\"){a:att"
                + (multiple ? "s" : "") + "(n:\"" + inner + "\"){a:" + innerInner + "}}}",
            ".edge(n:\"" + edgeName + "\"){a:" + inner + "{a:" + innerInner + "}}");
    }

    private void assertEdgeScalar(String inner, String innerInner) {
        String edgeName = "cm:name";
        assertAtt(".att(n:\"_edge\"){a:att(n:\"" + edgeName + "\"){a:att(n:\"" + inner + "\"){a:" + innerInner + "}}}",
            ".edge(n:\"" + edgeName + "\"){a:" + inner + "}");
    }

    @Test
    public void edgeSimpleTest() {
        assertAtt(".att(n:\"_edge\"){a:att(n:\"cm:name\"){a:att(n:\"protected\"){a:bool}}}",
            "#cm:name?protected");
    }

    @Test
    public void hasAttTest() {
        assertAtt(".att(n:\"permissions\"){a:att(n:\"_has\"){a:att(n:\"Write\"){a:bool}}}", "permissions?has('Write')");
    }

    @Test
    public void asAttTest() {
        assertAtt(".att(n:\"permissions\"){" +
            "a:att(n:\"_as\"){a:att(n:\"NodeRef\"){" +
            "a:att(n:\"inner\"){" +
            "a:att(n:\"att\"){a:str}" +
        "}}}}", ".att(n:\"permissions\"){a:as('NodeRef'){a:att('inner'){a:att('att'){str}}}}");
    }

    @Test
    public void objAttTest() {
        assertAtt(".att(n:\"cm:name\"){a:att(n:\"first\"){a:num},b:att(n:\"second\"){a:str}}", "cm:name{first?num,second?str}");
    }

    @Test
    public void attTest() {

        assertAtt(".att(n:\"cm:name\"){a:str}", ".att(n:\"cm:name\"){str}");

        assertAtt(".att(n:\"cm:title\"){a:att(n:\"cm:name\"){a:str}}", "cm:title.cm:name?str");
        assertAtt(".att(n:\"cm:title\"){a:att(n:\"cm:name\"){a:att(n:\"deep\"){a:att(n:\"field\"){a:disp}}}}",
            "cm:title.cm:name.deep.field?disp");

        assertAtt(".att(n:\"cm:name\"){a:disp}", "cm:name");
        assertAtt(".att(n:\"cm:name\"){a:att(n:\"cm:title\"){a:disp}}", "cm:name.cm:title");
        assertAtt(".att(n:\"cm:name\"){al:att(n:\"title\"){a:disp}}", "cm:name{al:title}");

        assertAtt(".atts(n:\"cm:name\"){al:att(n:\"title\"){a:disp}}", "cm:name[]{al:title}");
        assertAtt(".atts(n:\"cm:name\"){al:atts(n:\"title\"){a:disp}}", "cm:name[]{al:title[]}");
        assertAtt(".att(n:\"cm:name\"){al:atts(n:\"title\"){a:disp}}", "cm:name{al:title[]}");

        assertAtt(".atts(n:\"cm:name\"){a:att(n:\"cm:title\"){a:disp}}", "cm:name[].cm:title");
        assertAtt(".atts(n:\"cm:name\"){a:atts(n:\"cm:title\"){a:disp}}", "cm:name[].cm:title[]");
        assertAtt(".att(n:\"cm:name\"){a:atts(n:\"cm:title\"){a:disp}}", "cm:name.cm:title[]");
    }

    private void assertAtt(String expected, String source) {
        String gqlAtt = writer.writeToString(reader.readAttribute(source));
        assertEquals(expected, gqlAtt);
        assertEquals(gqlAtt, writer.writeToString(reader.readAttribute(gqlAtt)));
    }
}
