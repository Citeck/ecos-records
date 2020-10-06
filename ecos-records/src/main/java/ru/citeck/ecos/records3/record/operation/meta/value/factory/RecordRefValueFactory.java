package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import org.jetbrains.annotations.NotNull;
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
    private static final String ATT_LOCAL_ID = "?localId";

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
        private final InnerMetaValue innerAtts;

        RecordRefValue(RecordRef recordRef) {

            SchemaAtt schemaAtt = AttContext.getCurrentSchemaAtt();
            List<SchemaAtt> innerSchema = schemaAtt.getInner();

            ref = recordRef;

            Map<String, String> attsMap = new HashMap<>();

            StringBuilder sb = new StringBuilder();
            for (SchemaAtt inner : innerSchema) {
                String innerName = inner.getName();
                if (!innerName.equals(ATT_ID) && !innerName.equals(ATT_LOCAL_ID)) {
                    schemaWriter.write(inner, sb);
                    attsMap.put(inner.getName(), sb.toString());
                    sb.setLength(0);
                }
            }

            RecordAtts atts;
            if (attsMap.size() > 0) {
                atts = recordsService.getAtts(ref, attsMap, true);
            } else {
                atts = new RecordAtts(ref);
            }

            innerAtts = new InnerMetaValue(atts.getAttributes().getData().asJson());

            //todo
            if (attsMap.containsKey("?assoc") && !attsMap.containsKey("?str")) {
                atts.set("?str", atts.getStringOrNull("?assoc"));
            }
        }

        @Override
        public RecordRef getId() {
            return ref;
        }

        @Override
        public String getString() {
            return innerAtts.getString();
        }

        @Override
        public String getDisplayName() {
            return innerAtts.getDisplayName();
        }

        @Override
        public Double getDouble() {
            return innerAtts.getDouble();
        }

        @Override
        public Boolean getBool() {
            return innerAtts.getBool();
        }

        @Override
        public Object getJson() {
            return innerAtts.getJson();
        }

        @Override
        public boolean has(@NotNull String name) {
            return innerAtts.has(name);
        }

        @Override
        public Object getAs(@NotNull String type) {
            return innerAtts.getAs(type);
        }

        @Override
        public Object getAtt(@NotNull String name) {
            return innerAtts.getAtt(name);
        }

        public RecordRef getRef() {
            return ref;
        }
    }
}
