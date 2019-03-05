package ru.citeck.ecos.records.test;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.meta.RecordsMetaService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsMetaQueryTest {

    private RecordsMetaService recordsMetaService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsMetaService = factory.createRecordsMetaService();
    }

    @Test
    void testQueryBuild() {

        Map<String, String> attributes = recordsMetaService.getAttributes(SimplePojo.class);

        assertEquals(2, attributes.size());

        assertEquals(".att(n:\"aaa\"){atts(n:\"bbb\"){str}}", attributes.get("someatt"));
        assertEquals(".edge(n:\"cm:title\"){multiple}", attributes.get("edge"));
    }

    public static class SimplePojo {

        @MetaAtt(".att(n:'aaa'){atts(n:'bbb')}")
        private List<String> someatt;

        @MetaAtt("#cm:title?multiple")
        private boolean edge;

        public boolean isEdge() {
            return edge;
        }

        public void setEdge(boolean edge) {
            this.edge = edge;
        }

        public List<String> getSomeatt() {
            return someatt;
        }

        public void setSomeatt(List<String> someatt) {
            this.someatt = someatt;
        }
    }

}
