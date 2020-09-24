package ru.citeck.ecos.records3.evaluator.evaluators;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.ServiceFactoryAware;
import ru.citeck.ecos.records3.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records3.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records3.evaluator.details.EvalDetails;
import ru.citeck.ecos.records3.evaluator.details.EvalDetailsImpl;
import ru.citeck.ecos.records3.evaluator.details.EvalResultCause;
import ru.citeck.ecos.records3.evaluator.details.RecordEvaluatorWithDetails;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public class GroupEvaluator
    implements RecordEvaluatorWithDetails<Map<String, String>, RecordMeta, GroupEvaluator.Config>,
              ServiceFactoryAware {

    public static final String TYPE = "group";

    private RecordEvaluatorService recordEvaluatorService;

    @Override
    public EvalDetails evalWithDetails(RecordMeta meta, Config config) {

        Stream<RecordEvaluatorDto> evaluators = config.getEvaluators().stream();

        List<EvalResultCause> causes = new ArrayList<>();

        Predicate<RecordEvaluatorDto> predicate = evaluator -> {
            EvalDetails evalDetails = recordEvaluatorService.evalDetailsWithMeta(evaluator, meta);
            boolean result = evalDetails != null && evalDetails.getResult();
            if (!result && evalDetails != null) {
                causes.addAll(evalDetails.getCauses());
            }
            return result;
        };

        boolean result = false;

        if (JoinType.AND.equals(config.joinBy)) {
            result = evaluators.allMatch(predicate);
        } else if (JoinType.OR.equals(config.joinBy)) {
            result = evaluators.anyMatch(predicate);
        } else {
            log.warn("Unknown join type: " + config.joinBy);
        }

        return new EvalDetailsImpl(result, causes);
    }

    @Override
    public Map<String, String> getMetaToRequest(Config config) {

        Set<String> atts = new HashSet<>();

        config.getEvaluators()
            .stream()
            .map(e -> recordEvaluatorService.getRequiredMetaAttributes(e))
            .forEach(a -> atts.addAll(a.values()));

        Map<String, String> result = new HashMap<>();
        for (String att : atts) {
            result.put(att, att);
        }

        return result;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {
        this.recordEvaluatorService = serviceFactory.getRecordEvaluatorService();
    }

    @Data
    public static class Config {

        private JoinType joinBy = JoinType.AND;
        private List<RecordEvaluatorDto> evaluators = Collections.emptyList();
    }

    public enum JoinType { AND, OR }
}
