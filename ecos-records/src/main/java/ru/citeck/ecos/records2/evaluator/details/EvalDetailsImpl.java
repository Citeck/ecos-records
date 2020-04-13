package ru.citeck.ecos.records2.evaluator.details;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvalDetailsImpl implements EvalDetails {

    private boolean result;
    private List<EvalResultCause> causes;

    @Override
    public boolean getResult() {
        return result;
    }

    @Override
    public List<EvalResultCause> getCauses() {
        return causes != null ? causes : Collections.emptyList();
    }
}
