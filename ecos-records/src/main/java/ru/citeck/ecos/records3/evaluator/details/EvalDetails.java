package ru.citeck.ecos.records3.evaluator.details;

import java.util.List;

public interface EvalDetails {

    boolean getResult();

    List<EvalResultCause> getCauses();
}
