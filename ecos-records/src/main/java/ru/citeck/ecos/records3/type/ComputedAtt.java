package ru.citeck.ecos.records3.type;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@NoArgsConstructor
public class ComputedAtt {

    private String name;
    private String type;
    private ObjectData config;

    public ComputedAtt(ComputedAtt other) {
        this.type = other.type;
        this.name = other.name;
        this.config = ObjectData.deepCopy(other.config);
    }
}
