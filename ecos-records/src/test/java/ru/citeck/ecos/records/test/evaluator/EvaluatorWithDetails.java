package ru.citeck.ecos.records.test.evaluator;

import kotlin.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.records2.evaluator.details.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EvaluatorWithDetails implements RecordEvaluatorWithDetails<Object,
                                                                        Unit,
                                                                        EvaluatorWithDetails.Config> {

    public static final String TYPE = "with-details";

    @Override
    public EvalDetails evalWithDetails(Unit meta, Config config) {

        List<EvalResultCause> causes = Arrays.stream(config.cause)
            .map(EvalResultCauseImpl::new)
            .collect(Collectors.toList());

        return new EvalDetailsImpl(config.result, causes);
    }

    @Override
    public Object getMetaToRequest(Config config) {
        return null;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Config {
        private boolean result;
        private String[] cause;
    }
}
