package ru.citeck.ecos.records3.graphql.meta.value.factory;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.graphql.meta.value.InnerMetaValue;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records3.record.op.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.meta.schema.resolver.AttContext;
import ru.citeck.ecos.records3.record.op.meta.schema.write.AttSchemaGqlWriter;
import ru.citeck.ecos.records3.record.op.meta.schema.write.AttSchemaWriter;

import java.util.*;

public class RecordRefValueFactory implements MetaValueFactory<RecordRef> {

    private static final String ATT_ID = ".id";
    private static final String ATT_STR = ".str";

    private final RecordsService recordsService;
    private final AttSchemaWriter schemaWriter = new AttSchemaGqlWriter();

    public RecordRefValueFactory(RecordsServiceFactory serviceFactory) {
        this.recordsService = serviceFactory.getRecordsService();
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

        private final RecordRef ref;
        private final RecordMeta meta;

        RecordRefValue(RecordRef recordRef) {

            SchemaAtt schemaAtt = AttContext.getCurrentSchemaAtt();

            ref = recordRef;

            Map<String, String> attsMap = new HashMap<>();

            StringBuilder sb = new StringBuilder();
            for (SchemaAtt inner : schemaAtt.getInner()) {
                String innerName = inner.getName();
                if (!innerName.equals(ATT_ID) && !innerName.equals(ATT_STR)) {
                    schemaWriter.write(inner, sb);
                    attsMap.put(inner.getAliasForValue(), sb.toString());
                    sb.setLength(0);
                }
            }

            if (attsMap.size() > 0) {
                this.meta = recordsService.getAttributes(ref, attsMap);
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
        public boolean has(@NotNull String name) {
            return meta.getAttribute(".has").asBoolean();
        }

        @Override
        public Object getAs(@NotNull String type) {
            return new InnerMetaValue(meta.getAttribute(".as"));
        }

        @Override
        public String getId() {
            return ref.toString();
        }

        @Override
        public String getLocalId() {
            return ref.getId();
        }

        @Override
        public Object getAttribute(@NotNull String name) {
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

        public RecordRef getRef() {
            return ref;
        }
    }
}
