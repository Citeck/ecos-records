package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.InnerMetaValue;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.resolver.AttContext;
import ru.citeck.ecos.records3.record.operation.meta.schema.write.AttSchemaGqlWriter;
import ru.citeck.ecos.records3.record.operation.meta.schema.write.AttSchemaWriter;

import java.util.*;

public class RecordRefValueFactory implements AttValueFactory<RecordRef> {

    private static final String ATT_ID = "?id";
    private static final String ATT_STR = "?str";

    private final RecordsService recordsService;
    private final AttSchemaWriter schemaWriter = new AttSchemaGqlWriter();

    public RecordRefValueFactory(RecordsServiceFactory serviceFactory) {
        this.recordsService = serviceFactory.getRecordsService();
    }

    @Override
    public AttValue getValue(RecordRef value) {
        return new RecordRefValue(value);
    }

    @Override
    public List<Class<? extends RecordRef>> getValueTypes() {
        return Collections.singletonList(RecordRef.class);
    }

    public class RecordRefValue implements AttValue {

        private final RecordRef ref;
        private final RecordAtts atts;

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
                this.atts = recordsService.getAtts(ref, attsMap);
            } else {
                this.atts = new RecordAtts(ref);
            }

            if (attsMap.containsKey("?assoc") && !attsMap.containsKey("?str")) {
                atts.set("?str", atts.getStringOrNull("?assoc"));
            }
        }

        @Override
        public String getString() {
            return ref.toString();
        }

        @Override
        public String getDispName() {
            return atts.getStringOrNull("?disp");
        }

        @Override
        public Double getDouble() {
            return atts.getDoubleOrNull("?num");
        }

        @Override
        public Boolean getBool() {
            return atts.getBoolOrNull("?bool");
        }

        @Override
        public Object getJson() {
            return atts.getAttribute("?json");
        }

        @Override
        public boolean has(@NotNull String name) {
            return atts.getAttribute("?has").asBoolean();
        }

        @Override
        public Object getAs(@NotNull String type) {
            return new InnerMetaValue(atts.getAttribute("?as"));
        }

        @Override
        public Object getAtt(@NotNull String name) {
            DataValue result = atts.get(name);
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
