package ru.citeck.ecos.records2.graphql.meta.value.factory;

import com.fasterxml.jackson.databind.JsonNode;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceAware;
import ru.citeck.ecos.records2.graphql.GqlContext;
import ru.citeck.ecos.records2.graphql.meta.value.InnerMetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.Collections;
import java.util.List;

public class RecordRefValueFactory implements MetaValueFactory<RecordRef>, RecordsServiceAware {

    private RecordsService recordsService;

    @Override
    public MetaValue getValue(RecordRef value) {
        return new RecordRefValue(value);
    }

    @Override
    public List<Class<? extends RecordRef>> getValueTypes() {
        return Collections.singletonList(RecordRef.class);
    }

    @Override
    public void setRecordsService(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    public class RecordRefValue implements MetaValue {

        private RecordRef ref;
        private RecordMeta meta;

        RecordRefValue(RecordRef recordRef) {
            ref = recordRef;
        }

        @Override
        public <T extends GqlContext> void init(T context, MetaField field) {
            meta = recordsService.getRawAttributes(ref, field.getInnerAttributesMap());
        }

        @Override
        public String getString() {
            return meta.getStringOrNull(".str");
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
        public String getId() {
            return ref.toString();
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            JsonNode result = meta.get(name);
            return result != null ? new InnerMetaValue(result) : null;
        }
    }
}
