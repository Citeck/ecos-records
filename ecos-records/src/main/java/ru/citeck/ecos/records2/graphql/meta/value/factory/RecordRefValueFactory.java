package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.InnerMetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.*;

public class RecordRefValueFactory implements MetaValueFactory<RecordRef> {

    private static final String ATT_ID = ".id";
    private static final String ATT_STR = ".str";

    private RecordsServiceFactory serviceFactory;

    public RecordRefValueFactory(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    @Override
    public MetaValue getValue(RecordRef value) {
        return new RecordRefValue(value);
    }

    @Override
    public List<Class<? extends RecordRef>> getValueTypes() {
        return Collections.singletonList(RecordRef.class);
    }

    public class RecordRefValue implements MetaValue {

        private RecordRef ref;
        private RecordMeta meta;

        RecordRefValue(RecordRef recordRef) {
            ref = recordRef;
        }

        @Override
        public <T extends QueryContext> void init(T context, MetaField field) {

            Map<String, String> attsMap = field.getInnerAttributesMap();
            if (Objects.equals(attsMap.get(ATT_ID), ATT_ID)) {
                attsMap.remove(ATT_ID);
            }
            if (Objects.equals(attsMap.get(ATT_STR), ATT_STR)) {
                attsMap.remove(ATT_STR);
            }

            if (attsMap.size() > 0) {
                this.meta = serviceFactory.getRecordsService().getRawAttributes(ref, attsMap);
            } else {
                this.meta = new RecordMeta(ref);
            }

            if (attsMap.containsKey(".assoc") && !attsMap.containsKey(".str")) {
                meta.set(".str", meta.getStringOrNull(".assoc"));
            }
        }

        @Override
        public String getString() {
            return ref.toString();
        }

        @Override
        public String getDisplayName() {
            return meta.getStringOrNull(".disp");
        }

        @Override
        public Double getDouble() {
            return meta.getDoubleOrNull(".num");
        }

        @Override
        public Boolean getBool() {
            return meta.getBoolOrNull(".bool");
        }

        @Override
        public Object getJson() {
            return meta.getAttribute(".json");
        }

        @Override
        public boolean has(String name) {
            return meta.getAttribute(".has").asBoolean();
        }

        @Override
        public String getId() {
            return ref.toString();
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            DataValue result = meta.get(name);
            if (result.isArray()) {
                List<InnerMetaValue> resultList = new ArrayList<>();
                for (DataValue node : result) {
                    resultList.add(new InnerMetaValue(Json.getMapper().toJson(node)));
                }
                return resultList;
            }
            return result.isNotNull() ? new InnerMetaValue(Json.getMapper().toJson(result)) : null;
        }
    }
}
