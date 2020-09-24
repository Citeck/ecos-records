package ru.citeck.ecos.records3.evaluator;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class RecordEvaluatorDto {

    private String id;
    private String type;
    private boolean inverse;
    private ObjectData config;
}
