package ru.citeck.ecos.records2.meta.attproc;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;

import java.util.List;

@Data
@AllArgsConstructor
public class AttProcessorDef {
    private String type;
    private List<DataValue> arguments;
}
