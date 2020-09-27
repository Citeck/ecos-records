package ru.citeck.ecos.records3.record.operation.meta.schema.resolver;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.meta.schema.AttSchema;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.source.common.AttMixin;

import java.util.*;

@Data
@RequiredArgsConstructor
public class ResolveArgs {

    @NotNull
    private final List<Object> values;
    @NotNull
    private final List<RecordRef> valueRefs;
    @NotNull
    private final AttSchema schema;
    @NotNull
    private final Map<String, AttMixin> mixins;

    private final boolean rawAtts;

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {

        private List<Object> values = new ArrayList<>();
        private List<RecordRef> valueRefs = Collections.emptyList();
        private AttSchema schema;

        @Getter
        private boolean rawAtts;

        private Map<String, AttMixin> mixins = Collections.emptyMap();

        public Builder setRawAtts(boolean value) {
            rawAtts = value;
            return this;
        }

        @NotNull
        public Builder setValues(@Nullable List<Object> values) {
            this.values = values != null ? new ArrayList<>(values) : Collections.emptyList();
            return this;
        }

        @NotNull
        public Builder setValueRefs(@Nullable List<RecordRef> valueRefs) {
            this.valueRefs = valueRefs != null ? new ArrayList<>(valueRefs) : Collections.emptyList();
            return this;
        }

        @NotNull
        public Builder setMixins(@Nullable Collection<AttMixin> mixins) {
            if (mixins == null) {
                this.mixins = Collections.emptyMap();
            } else {
                this.mixins = new LinkedHashMap<>();
                mixins.forEach(m -> m.getAtts().forEach(a -> this.mixins.put(a, m)));
            }
            return this;
        }

        @NotNull
        public Builder setMixins(@Nullable Map<String, AttMixin> mixins) {
            if (mixins == null) {
                this.mixins = Collections.emptyMap();
            } else {
                this.mixins = new LinkedHashMap<>(mixins);
            }
            return this;
        }

        @NotNull
        public Builder setAtts(@Nullable List<SchemaRootAtt> attributes) {
            this.schema = new AttSchema(attributes != null ? new ArrayList<>(attributes) : Collections.emptyList());
            return this;
        }

        @NotNull
        public Builder setAtt(@Nullable SchemaRootAtt attribute) {
            return setAtts(attribute != null ? Collections.singletonList(attribute) : null);
        }

        public Builder setSchema(AttSchema schema) {
            this.schema = schema;
            return this;
        }

        public ResolveArgs build() {

            MandatoryParam.check("schema", schema);
            MandatoryParam.check("values", values);

            if (!valueRefs.isEmpty() && valueRefs.size() != values.size()) {
                throw new RuntimeException("valueRefs should have same size with values");
            }
            return new ResolveArgs(values, valueRefs, schema, mixins, rawAtts);
        }
    }
}
