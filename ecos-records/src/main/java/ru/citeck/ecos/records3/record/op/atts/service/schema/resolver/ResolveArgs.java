package ru.citeck.ecos.records3.record.op.atts.service.schema.resolver;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin;

import java.util.*;

@Data
@RequiredArgsConstructor
public class ResolveArgs {

    @NotNull
    private final List<Object> values;
    @NotNull
    private final String sourceId;
    @NotNull
    private final List<RecordRef> valueRefs;
    @NotNull
    private final List<SchemaRootAtt> attributes;
    @NotNull
    private final List<AttMixin> mixins;

    private final boolean rawAtts;

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {

        private String sourceId = "";
        private List<Object> values = new ArrayList<>();
        private List<RecordRef> valueRefs = Collections.emptyList();
        private List<SchemaRootAtt> attributes;

        @Getter
        private boolean rawAtts;

        private List<AttMixin> mixins = new ArrayList<>();

        public Builder setSourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder setRawAtts(boolean value) {
            rawAtts = value;
            return this;
        }

        @NotNull
        public Builder setValues(@Nullable List<?> values) {
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
                this.mixins = new ArrayList<>();
            } else {
                this.mixins = new ArrayList<>(mixins);
            }
            return this;
        }

        @NotNull
        public Builder setAtts(@Nullable List<SchemaRootAtt> attributes) {
            this.attributes = attributes != null ? new ArrayList<>(attributes) : Collections.emptyList();
            return this;
        }

        @NotNull
        public Builder setAtt(@Nullable SchemaRootAtt attribute) {
            return setAtts(attribute != null ? Collections.singletonList(attribute) : null);
        }

        public ResolveArgs build() {

            MandatoryParam.check("schema", attributes);
            MandatoryParam.check("values", values);

            if (!valueRefs.isEmpty() && valueRefs.size() != values.size()) {
                throw new RuntimeException("valueRefs should have same size with values");
            }
            return new ResolveArgs(values, sourceId, valueRefs, attributes, mixins, rawAtts);
        }
    }
}
