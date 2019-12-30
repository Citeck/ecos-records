package ru.citeck.ecos.records2.evaluator.evaluators;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public class GroupEvaluator implements RecordEvaluator<Map<String, String>, RecordMeta, GroupEvaluator.Config>,
                                       ServiceFactoryAware {

    public static final String TYPE = "group";

    private RecordEvaluatorService recordEvaluatorService;

    @Override
    public boolean evaluate(RecordMeta meta, Config config) {

        Stream<RecordEvaluatorDto> evaluators = config.getEvaluators().stream();
        Predicate<RecordEvaluatorDto> predicate = evaluator -> recordEvaluatorService.evaluateWithMeta(evaluator, meta);

        if (JoinType.AND.equals(config.joinBy)) {
            return evaluators.allMatch(predicate);
        } else if (JoinType.OR.equals(config.joinBy)) {
            return evaluators.anyMatch(predicate);
        } else {
            log.warn("Unknown join type: " + config.joinBy);
        }
        return false;
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
