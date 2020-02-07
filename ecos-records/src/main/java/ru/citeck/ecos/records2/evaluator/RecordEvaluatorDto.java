package ru.citeck.ecos.records2.evaluator;

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.Data;

@Data
public class RecordEvaluatorDto {

    private String id;
    private String type;
    private boolean inverse;
    private ObjectNode config;
}
