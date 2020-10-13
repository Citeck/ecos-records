package ru.citeck.ecos.records2.evaluator.details;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvalResultCauseImpl implements EvalResultCause {

    private String message;
    private String localizedMessage;
    private String type = "";
    private ObjectData data = ObjectData.create();

    public EvalResultCauseImpl(String cause) {
        this.message = cause;
        this.localizedMessage = cause;
    }
}
