package ru.citeck.ecos.records2.evaluator.details;

import ru.citeck.ecos.commons.data.ObjectData;

public interface EvalResultCause {

    String getMessage();

    String getLocalizedMessage();

    String getType();

    ObjectData getData();
}
