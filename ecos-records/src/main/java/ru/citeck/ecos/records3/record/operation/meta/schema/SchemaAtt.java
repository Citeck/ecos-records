package ru.citeck.ecos.records3.record.operation.meta.schema;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records3.record.operation.meta.schema.exception.AttSchemaException;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class SchemaAtt {



    private final String alias;
    private final String name;
    private final boolean multiple;
    private final List<SchemaAtt> inner;

    public static Builder create() {
        return new Builder();
    }

    private SchemaAtt(String alias,
                      String name,
                      boolean multiple,
                      List<SchemaAtt> inner) {

        this.alias = alias;
        this.name = name;
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

    public boolean isScalar() {
        return name.length() > 0 && name.charAt(0) == '?';
    }

    public String getAliasForValue() {
        if (alias.isEmpty()) {
            return name;
        }
        return alias;
    }

    @Override
    public String toString() {
        String res = "{" +
            "\"alias\":\"" + alias + '\"' +
            ", \"name\":\"" + name + '\"' +
            ", \"multiple\":" + multiple;

        if (!inner.isEmpty()) {
            res += ", \"inner\":[" + inner.stream()
                .map(SchemaAtt::toString)
                .collect(Collectors.joining(", ")) + "]";
        }
        return res + '}';
    }

    @ToString
    public static class Builder {

        private String alias = "";
        private String name;
        private boolean multiple;
        private List<SchemaAtt> inner = Collections.emptyList();

        public Builder() {
        }

        public Builder(SchemaAtt base) {
            this.alias = base.getAlias();
            this.name = base.getName();
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

        public Builder setMultiple(boolean multiple) {
            this.multiple = multiple;
            return this;
        }

        public boolean isMultiple() {
            return this.multiple;
        }

        public List<SchemaAtt> getInner() {
            return inner;
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

            SchemaAtt att = new SchemaAtt(alias, name, multiple, Collections.unmodifiableList(inner));

            if (att.isScalar() && !inner.isEmpty()) {
                throw new AttSchemaException("Attribute can't be a scalar and has inner attributes. " + this);
            }
            if (!att.isScalar() && inner.isEmpty()) {
                throw new AttSchemaException("Attribute can't be not a scalar and has empty inner attributes. " + this);
            }
            if (att.isScalar() && att.isMultiple()) {
                throw new AttSchemaException("Scalar can't hold multiple values. " + this);
            }
            if (att.isScalar() && !ScalarType.getBySchema(att.getName()).isPresent()) {
                throw new AttSchemaException("Unknown scalar: '" + att.getName() + "'");
            }

            return att;
        }
    }
}
