package ru.citeck.ecos.records2.type;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.Map;

@Data
@NoArgsConstructor
public class ComputedAttribute {

    private String id;
    private Map<String, String> model;

    private String type;
    private ObjectData config;

    public ComputedAttribute(ComputedAttribute other) {
        this.id = other.id;
        this.model = DataValue.create(other.model).asMap(String.class, String.class);
        this.type = other.type;
        this.config = ObjectData.deepCopy(other.config);
    }
}
