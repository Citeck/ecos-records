package ru.citeck.ecos.records.test.schema;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records2.meta.schema.write.AttSchemaWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaTest {

    private final AttSchemaReader reader = new AttSchemaReader();
    private final AttSchemaWriter writer = new AttSchemaWriter();

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

        assertAtt(".att(n:\"_edge\"){att(n:\"cm:name\"){att(n:\"protected\"){bool}}}",
            ".edge(n:\"cm:name\"){protected}");
    }

    private void assertEdgeMetaVal(String inner, boolean multiple) {
        String edgeName = "cm:name";
        String innerInner = "att(n:\"title\"){disp}";
        assertAtt(".att(n:\"_edge\"){att(n:\"" + edgeName + "\"){att"
                + (multiple ? "s" : "") + "(n:\"" + inner + "\"){" + innerInner + "}}}",
            ".edge(n:\"" + edgeName + "\"){" + inner + "{" + innerInner + "}}");
    }

    private void assertEdgeScalar(String inner, String innerInner) {
        String edgeName = "cm:name";
        assertAtt(".att(n:\"_edge\"){att(n:\"" + edgeName + "\"){att(n:\"" + inner + "\"){" + innerInner + "}}}",
            ".edge(n:\"" + edgeName + "\"){" + inner + "}");
    }

    @Test
    public void edgeSimpleTest() {
        assertAtt(".att(n:\"_edge\"){att(n:\"cm:name\"){att(n:\"protected\"){bool}}}",
            "#cm:name?protected");
    }

    @Test
    public void hasAttTest() {
        assertAtt(".att(n:\"permissions\"){att(n:\"_has\"){att(n:\"Write\"){bool}}}", "permissions?has('Write')");
    }

    @Test
    public void asAttTest() {
        assertAtt(".att(n:\"permissions\"){" +
            "att(n:\"_as\"){att(n:\"NodeRef\"){" +
                "att(n:\"inner\"){" +
                    "att(n:\"att\"){str}" +
        "}}}}", ".att(n:\"permissions\"){as('NodeRef'){att('inner'){att('att'){str}}}}");
    }

    @Test
    public void objAttTest() {
        assertAtt(".att(n:\"cm:name\"){" +
            "first:att(n:\"first\"){num}," +
            "second:att(n:\"second\"){str}," +
            "third:att(n:\"third\"){disp}" +
        "}", "cm:name{first?num,second?str,third}");
    }

    @Test
    public void attTest() {

        assertAtt(".att(n:\"cm:title\"){att(n:\"cm:name\"){str}}", "cm:title.cm:name?str");

        assertAtt(".att(n:\"cm:name\"){str}", ".att(n:\"cm:name\"){str}");
        assertAtt(".att(n:\"cm:title\"){att(n:\"cm:name\"){att(n:\"deep\"){att(n:\"field\"){disp}}}}",
            "cm:title.cm:name.deep.field?disp");

        assertAtt(".att(n:\"cm:name\"){disp}", "cm:name");
        assertAtt(".att(n:\"cm:name\"){att(n:\"cm:title\"){disp}}", "cm:name.cm:title");
        assertAtt(".att(n:\"cm:name\"){al:att(n:\"title\"){disp}}", "cm:name{al:title}");

        assertAtt(".atts(n:\"cm:name\"){al:att(n:\"title\"){disp}}", "cm:name[]{al:title}");
        assertAtt(".atts(n:\"cm:name\"){al:atts(n:\"title\"){disp}}", "cm:name[]{al:title[]}");
        assertAtt(".att(n:\"cm:name\"){al:atts(n:\"title\"){disp}}", "cm:name{al:title[]}");

        assertAtt(".atts(n:\"cm:name\"){att(n:\"cm:title\"){disp}}", "cm:name[].cm:title");
        assertAtt(".atts(n:\"cm:name\"){atts(n:\"cm:title\"){disp}}", "cm:name[].cm:title[]");
        assertAtt(".att(n:\"cm:name\"){atts(n:\"cm:title\"){disp}}", "cm:name.cm:title[]");

        assertAtt(".att(n:\"cm:name\"){_u002E_disp:disp,_u002E_str:str}", "cm:name{.disp,.str}");
        assertAtt(".att(n:\"cm:name\"){disp:disp,_u002E_str:str}", "cm:name{disp:.disp,.str}");
        assertAtt(".att(n:\"cm:name\"){name:att(n:\"name\"){disp},displayName:att(n:\"displayName\"){disp}}",
            "cm:name{name,displayName}");

        assertAtt(".atts(n:\"cm:name\"){" +
            "cm_u003A_name:att(n:\"cm:name\"){disp}," +
            "cm_u003A_title:att(n:\"cm:title\"){disp}" +
        "}", "cm:name[]{'cm:name','cm:title'}");

        assertAtt(".att(n:\"cm:name\"){disp}", "cm:name");
        assertAtt(".att(n:\"cm:na.me\"){disp}", "'cm:na.me'");
        assertAtt(".att(n:\"cm:na\"){att(n:\"me\"){disp}}", "cm:na.me");

        assertAtt(".att(n:\"cm:name[]\"){disp}", "\"cm:name[]\"");
        assertAtt(".att(n:\"cm:name[]\"){disp}", "'cm:name[]'");
        assertAtt(".atts(n:\"cm:name\"){disp}", "cm:name[]");
        assertAtt(".att(n:\"cm:name[]?str\"){disp}", "'cm:name[]?str'");
        assertAtt(".atts(n:\"cm:name\"){str}", "cm:name[]?str");

        assertAtt(".att(n:\"cm:name\"){strange_u0020_alias_u0020_name:disp,_u002E_str:str}", "cm:name{'strange alias name':.disp,.str}");
    }

    @Test
    public void testInnerAliases() {
        assertAtt(".att(n:\"cm:name\"){_u002E_disp:disp,_u002E_str:str}", "cm:name{.disp,.str}");
        assertAtt(".att(n:\"cm:name\"){_u002E_disp:disp,_u002E_str:str}", "cm:name{'.disp',\".str\"}");
    }

    private void assertAtt(String expected, String source) {
        String gqlAtt = writer.writeToString(reader.readAttribute(source));
        assertEquals(expected, gqlAtt);
        assertEquals(gqlAtt, writer.writeToString(reader.readAttribute(gqlAtt)));
    }
}
