package ru.citeck.ecos.records2.graphql.meta.value;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

/**
 * @deprecated actual version of create variant model located in ecos-model-lib
 */
@Data
@Deprecated
public class CreateVariant {

    private MLText label;
    private String formKey;
    private RecordRef formRef;
    private RecordRef recordRef;
    private ObjectData attributes = ObjectData.create();

    public CreateVariant() {
    }

    public CreateVariant(RecordRef recordRef) {
        this.recordRef = recordRef;
    }

    public void setAttributes(ObjectData attributes) {
        this.attributes = ObjectData.create(attributes);
    }

    public void setAttribute(String attribute, String value) {
        this.attributes.set(attribute, value);
    }
}
