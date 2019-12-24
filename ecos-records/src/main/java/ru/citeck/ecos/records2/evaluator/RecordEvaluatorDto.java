package ru.citeck.ecos.records2.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

@Data
public class RecordEvaluatorDto {

    private String id;
    private String type;
    private boolean inverse;
    private ObjectNode config;
}
