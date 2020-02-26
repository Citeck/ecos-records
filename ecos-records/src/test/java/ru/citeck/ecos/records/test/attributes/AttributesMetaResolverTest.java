package ru.citeck.ecos.records.test.attributes;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.meta.AttributesMetaResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttributesMetaResolverTest {

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        AttributesMetaResolver attsMetaResolver = factory.getAttributesMetaResolver();

        String res = attsMetaResolver.convertAttToGqlFormat("field{id,str,config?json}", "str", false);

        assertEquals(".att(n:\"field\"){id:att(n:\"id\"){str},str:att(n:\"str\"){str},config:att(n:\"config\"){json}}", res);
    }
}
