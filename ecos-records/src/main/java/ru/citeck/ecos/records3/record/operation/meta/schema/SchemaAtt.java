package ru.citeck.ecos.records3.record.operation.meta.schema;

import lombok.Data;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records3.record.operation.meta.schema.exception.AttSchemaException;

import java.util.*;

@Data
public class SchemaAtt {

    private final String alias;
    private final String name;
    private final boolean scalar;
    private final boolean multiple;
    private final List<SchemaAtt> inner;

    public static Builder create() {
        return new Builder();
    }

    private SchemaAtt(String alias,
                      String name,
                      boolean scalar,
                      boolean multiple,
                      List<SchemaAtt> inner) {

        this.alias = alias;
        this.name = name;
        this.scalar = scalar;
        this.multiple = multiple;
        this.inner = inner;
    }

    public Builder modify() {
        return new Builder(this);
    }

    public String getScalarName() {
        if (isScalar()) {
            return getName();
        } else {
            if (inner.size() != 1) {
                return null;
            }
            return inner.get(0).getScalarName();
        }
    }

    public String getAliasForValue() {
        if (alias.isEmpty()) {
            return name;
        }
        return alias;
    }

    public static class Builder {

        private String alias = "";
        private String name;
        private boolean scalar;
        private boolean multiple;
        private List<SchemaAtt> inner = Collections.emptyList();

        public Builder() {
        }

        public Builder(SchemaAtt base) {
            this.alias = base.getAlias();
            this.name = base.getName();
            this.scalar = base.isScalar();
            this.multiple = base.isMultiple();
            this.inner = base.getInner();
        }

        public Builder setAlias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setScalar(boolean scalar) {
            this.scalar = scalar;
            return this;
        }

        public Builder setMultiple(boolean multiple) {
            this.multiple = multiple;
            return this;
        }

        public Builder setInner(List<SchemaAtt> inner) {
            this.inner = inner;
            return this;
        }

        public Builder setInner(SchemaAtt inner) {
            return setInner(Collections.singletonList(inner));
        }

        public Builder setInner(SchemaAtt.Builder inner) {
            return setInner(inner.build());
        }

        public SchemaAtt build() {

            MandatoryParam.check("name", name);

            if (scalar && !inner.isEmpty()) {
                throw new AttSchemaException("Attribute can't be a scalar and has inner attributes");
            }
            if (!scalar && inner.isEmpty()) {
                throw new AttSchemaException("Attribute can't be not a scalar and has empty inner attributes");
            }

            return new SchemaAtt(alias, name, scalar, multiple, Collections.unmodifiableList(inner));
        }
    }
}
