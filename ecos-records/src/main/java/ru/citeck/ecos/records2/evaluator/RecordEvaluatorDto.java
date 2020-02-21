package ru.citeck.ecos.records2.evaluator;

import lombok.Data;
import ru.citeck.ecos.records2.objdata.ObjectData;

@Data
public class RecordEvaluatorDto {

    private String id;
    private String type;
    private boolean inverse;
    private ObjectData config;
}
