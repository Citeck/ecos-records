package ru.citeck.ecos.records2.evaluator;

import lombok.Data;
import ru.citeck.ecos.records2.attributes.Attributes;

@Data
public class RecordEvaluatorDto {

    private String id;
    private String type;
    private boolean inverse;
    private Attributes config;
}
