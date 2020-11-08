package ru.citeck.ecos.records3.test.schema;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.schema.write.AttSchemaWriter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaTest {

    private final RecordsServiceFactory factory = new RecordsServiceFactory();
    private final AttSchemaReader reader = factory.getAttSchemaReader();
    private final AttSchemaWriter writer = factory.getAttSchemaWriter();

    @Test
    public void innerAliasTest() {

        String schema = ".edge(n:\"test\"){" +
            "distinct{str,disp}," +
            "options{str,disp}," +
            "createVariants{json}" +
            "}";

        SchemaAtt att = reader.read(schema);
        List<SchemaAtt> inner = att.getInner().get(0).getInner();

        assertEquals(3, inner.size());
        assertEquals("distinct{str,disp}", inner.get(0).getAlias());
        assertEquals("options{str,disp}", inner.get(1).getAlias());
        assertEquals("createVariants{json}", inner.get(2).getAlias());
    }

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

        assertAtt(".edge(n:\"cm:name\"){protected}", ".edge(n:\"cm:name\"){protected}");
    }

    private void assertEdgeMetaVal(String inner, boolean multiple) {
        String edgeName = "cm:name";
        String innerInner = "att(n:\"title\"){disp}";
        String att = ".edge(n:\"" + edgeName + "\"){" + inner + "{" + innerInner + "}}";
        assertAtt(att, att);
    }

    private void assertEdgeScalar(String inner, String innerInner) {

        String edgeName = "cm:name";
        String att = ".edge(n:\"" + edgeName + "\"){" + inner + "}";

        SchemaAtt schemaAtt = reader.read(att);
        assertEquals(1, schemaAtt.getInner().size());
        assertEquals(1, schemaAtt.getInner().get(0).getInner().size());
        assertEquals(inner, schemaAtt.getInner().get(0).getInner().get(0).getName());

        assertAtt(att, att);
    }

    @Test
    public void edgeSimpleTest() {
        assertAtt(".edge(n:\"cm:name\"){protected}", "#cm:name?protected");
    }

    @Test
    public void hasAttTest() {
        assertAtt(".att(n:\"permissions\"){has(n:\"Write\")}", "permissions?has('Write')");
    }

    @Test
    public void asAttTest() {
        assertAtt(".att(n:\"permissions\"){" +
            "as(n:\"NodeRef\"){" +
                "att(n:\"inner\"){" +
                    "att(n:\"att\"){str}" +
        "}}}", ".att(n:\"permissions\"){as('NodeRef'){att('inner'){att('att'){str}}}}");
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

        assertAtt(".str", "?str");

        assertAtt(".att(n:\"cm:title\"){att(n:\"cm:name\"){str}}", "cm:title.cm:name?str");

        assertAtt(".att(n:\"cm:name\"){str}", ".att(n:\"cm:name\"){str}");
        assertAtt(".att(n:\"cm:title\"){att(n:\"cm:name\"){att(n:\"deep\"){att(n:\"field\"){disp}}}}",
            "cm\\:title.cm:name.deep.field?disp");

        assertAtt(".att(n:\"cm:name\"){disp}", "cm\\:name");
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
            "_u0027_cm_u003A_name_u0027_:att(n:\"cm:name\"){disp}," +
            "_u0027_cm_u003A_title_u0027_:att(n:\"cm:title\"){disp}" +
        "}", "cm:name[]{'cm:name','cm:title'}");

        assertAtt(".att(n:\"cm:name\"){disp}", "cm:name");
        assertAtt(".att(n:\"cm:na\"){att(n:\"me\"){disp}}", "cm:na.me");

        assertAtt(".atts(n:\"cm:name\"){disp}", "\"cm:name[]\"");
        assertAtt(".atts(n:\"cm:name\"){disp}", "'cm:name[]'");
        assertAtt(".atts(n:\"cm:name\"){disp}", "cm:name[]");
        assertAtt(".atts(n:\"cm:name\"){str}", "'cm:name[]?str'");
        assertAtt(".atts(n:\"cm:name\"){str}", "cm:name[]?str");

        assertAtt(".att(n:\"cm:name\"){strange_u0020_alias_u0020_name:disp,_u002E_str:str}", "cm:name{'strange alias name':.disp,.str}");
    }

    @Test
    public void testPathWithQuotes() {

        assertAtt(".att(n:\"cm:na.me\"){disp}", "'cm:na.me'");
    }

    @Test
    public void testInnerAliases() {
        assertAtt(".att(n:\"cm:name\"){_u0027__u002E_disp_u0027_:disp,_u0022__u002E_str_u0022_:str}", "cm:name{'.disp',\".str\"}");
        assertAtt(".att(n:\"cm:name\"){_u002E_disp:disp,_u002E_str:str}", "cm:name{.disp,.str}");
    }

    private void assertAtt(String expected, String source) {
        String gqlAtt = writer.write(reader.read("a:" + source));
        assertEquals(expected, gqlAtt);
        assertEquals(gqlAtt, writer.write(reader.read(gqlAtt)));
    }
}
