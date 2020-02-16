package ru.citeck.ecos.records2.graphql.meta.value;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.objdata.ObjectData;

import java.util.Objects;

public class CreateVariant {

    private RecordRef recordRef;
    private String formKey;
    private String label;
    private ObjectData attributes = new ObjectData();

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

    public ObjectData getAttributes() {
        return attributes;
    }

    public void setAttributes(ObjectData attributes) {
        this.attributes = new ObjectData(attributes);
    }

    public void setAttribute(String attribute, String value) {
        this.attributes.set(attribute, value);
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
