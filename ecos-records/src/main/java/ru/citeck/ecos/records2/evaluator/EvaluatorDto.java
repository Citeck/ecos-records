package ru.citeck.ecos.records2.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

@Data
public class EvaluatorDto {

    private String id;
    private boolean inverse;
    private ObjectNode config;
}
