package ru.citeck.ecos.records.test.schema;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.meta.schema.AttsSchemaParser;

public class SchemaTest {

    @Test
    public void test() {

        AttsSchemaParser parser = new AttsSchemaParser();

        //System.out.println(parser.parseAttribute(".att(n:\"cm:name\"){str}"));
        System.out.println(parser.parseAttribute(".edge(n:\"cm:name\"){protected}"));
    }

}
