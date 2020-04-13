package ru.citeck.ecos.records2.evaluator.details;

import java.util.List;

public interface EvalDetails {

    boolean getResult();

    List<EvalResultCause> getCauses();
}
