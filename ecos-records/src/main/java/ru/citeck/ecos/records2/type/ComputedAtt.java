package ru.citeck.ecos.records2.type;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@NoArgsConstructor
public class ComputedAtt {

    private String id;
    private String type;
    private boolean persistent;
    private ObjectData config;

    public ComputedAtt(ComputedAtt other) {
        this.id = other.id;
        this.type = other.type;
        this.persistent = other.persistent;
        this.config = ObjectData.deepCopy(other.config);
    }
}
