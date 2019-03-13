package ru.citeck.ecos.records2;

import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.PredicateServiceImpl;
import ru.citeck.ecos.records2.graphql.GqlContext;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.graphql.meta.value.factory.*;
import ru.citeck.ecos.records2.graphql.types.GqlMetaQueryDef;
import ru.citeck.ecos.records2.graphql.types.GqlTypeDefinition;
import ru.citeck.ecos.records2.graphql.types.MetaEdgeTypeDef;
import ru.citeck.ecos.records2.graphql.types.MetaValueTypeDef;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.meta.RecordsMetaServiceImpl;

import java.util.ArrayList;
import java.util.List;

public class RecordsServiceFactory {

    public RecordsService createRecordsService() {
        return new RecordsServiceImpl(createRecordsMetaService(),
                                      createPredicateService());
    }

    public RecordsMetaService createRecordsMetaService() {
        return new RecordsMetaServiceImpl(createRecordsMetaGraphQL());
    }

    public PredicateService createPredicateService() {
        return new PredicateServiceImpl();
    }

    protected RecordsMetaGql createRecordsMetaGraphQL() {
        return new RecordsMetaGql(getGqlTypes(), GqlContext::new);
    }

    protected List<GqlTypeDefinition> getGqlTypes() {

        List<GqlTypeDefinition> types = new ArrayList<>();

        MetaValueTypeDef metaValueTypeDef = new MetaValueTypeDef();
        metaValueTypeDef.setMetaValueFactories(getMetaValueFactories());

        types.add(metaValueTypeDef);

        GqlMetaQueryDef gqlMetaQueryDef = new GqlMetaQueryDef();
        gqlMetaQueryDef.setMetaValueTypeDef(metaValueTypeDef);

        types.add(gqlMetaQueryDef);
        types.add(new MetaEdgeTypeDef(metaValueTypeDef));

        return types;
    }

    protected List<MetaValueFactory> getMetaValueFactories() {

        List<MetaValueFactory> metaValueFactories = new ArrayList<>();

        metaValueFactories.add(new RecordMetaValueFactory());
        metaValueFactories.add(new BeanValueFactory());
        metaValueFactories.add(new BooleanValueFactory());
        metaValueFactories.add(new DateValueFactory());
        metaValueFactories.add(new DoubleValueFactory());
        metaValueFactories.add(new IntegerValueFactory());
        metaValueFactories.add(new JsonNodeValueFactory());
        metaValueFactories.add(new LongValueFactory());
        metaValueFactories.add(new StringValueFactory());

        return metaValueFactories;
    }
}
