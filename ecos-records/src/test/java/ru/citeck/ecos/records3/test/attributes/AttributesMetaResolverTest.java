package ru.citeck.ecos.records3.test.attributes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttributesMetaResolverTest {

    @Test
    void test() {

        //RecordsServiceFactory factory = new RecordsServiceFactory();
        //AttributesMetaResolver attsMetaResolver = factory.getAttributesMetaResolver();

        /*tring res = attsMetaResolver.convertAttToGqlFormat("field{id,str,config?json}", "str", false);
        assertEquals(".att(n:\"field\"){id:att(n:\"id\"){str},str:att(n:\"str\"){str},config:att(n:\"config\"){json}}", res);

        res = attsMetaResolver.convertAttToGqlFormat("createVariants{attributes?json,recordRef?id}", "str", false);
        assertEquals(".att(n:\"createVariants\"){attributes:att(n:\"attributes\"){json},recordref:att(n:\"recordRef\"){id}}", res);

        res = attsMetaResolver.convertAttToGqlFormat("field0{.att(n:\"inner\"){json}}", "str", false);
        assertEquals(".att(n:\"field0\"){att_inner:att(n:\"inner\"){json}}", res);

        res = attsMetaResolver.convertAttToGqlFormat("field0{.att(n:\"inn:er\"){json}}", "str", false);
        assertEquals(".att(n:\"field0\"){att_inn_er:att(n:\"inn:er\"){json}}", res);

        res = attsMetaResolver.convertAttToGqlFormat("field0{alias:.att(n:\"inn:er\"){json}}", "str", false);
        assertEquals(".att(n:\"field0\"){alias:att(n:\"inn:er\"){json}}", res);

        res = attsMetaResolver.convertAttToGqlFormat(".type", "str", false);
        assertEquals(".type{id}", res);

        res = attsMetaResolver.convertAttToGqlFormat(".type.disp", "str", false);
        assertEquals(".type{disp}", res);*/
    }

    @Test
    void testInner() {

        //AttributesMetaResolver attsMetaResolver = new RecordsServiceFactory().getAttributesMetaResolver();

        /*String res = attsMetaResolver.convertAttToGqlFormat("field?as('abc').field0", "str", false);
        assertEquals(".att(n:\"field\"){as('abc'){att(n:\"field0\"){str}}}", res);

        res = attsMetaResolver.convertAttToGqlFormat("field?has('edf')", "str", false);
        assertEquals(".att(n:\"field\"){has('edf')}", res);*/
    }
}
