package ru.citeck.ecos.records2.graphql.meta.value;

import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Objects;

public class CreateVariant {

    private RecordRef recordRef;
    private ObjectNode attributes = JsonNodeFactory.instance.objectNode();
    private String formKey;
    private String label;

    public CreateVariant() {
    }

    public CreateVariant(RecordRef recordRef) {
        this.recordRef = recordRef;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setRecordRef(RecordRef recordRef) {
        this.recordRef = recordRef;
    }

    public RecordRef getRecordRef() {
        return recordRef;
    }

    public ObjectNode getAttributes() {
        return attributes;
    }

    public void setAttributes(ObjectNode attributes) {
        if (attributes != null) {
            this.attributes = attributes.deepCopy();
        } else {
            this.attributes = JsonNodeFactory.instance.objectNode();
        }
    }

    public void setAttribute(String attribute, String value) {
        this.attributes.put(attribute, value);
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreateVariant variant = (CreateVariant) o;
        return Objects.equals(recordRef, variant.recordRef)
            && Objects.equals(attributes, variant.attributes)
            && Objects.equals(formKey, variant.formKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordRef, attributes, formKey);
    }
}
