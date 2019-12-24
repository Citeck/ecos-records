package ru.citeck.ecos.records2.evaluator.evaluators;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorServiceAware;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public class GroupEvaluator implements RecordEvaluator<Map<String, String>, ObjectNode, GroupEvaluator.Config>,
                                       RecordEvaluatorServiceAware {

    public static final String TYPE = "group";

    @Setter
    private RecordEvaluatorService recordEvaluatorService;

    @Override
    public boolean evaluate(ObjectNode meta, Config config) {

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
    public Map<String, String> getRequiredMeta(Config config) {

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
    public Class<ObjectNode> getEvalMetaType() {
        return ObjectNode.class;
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Data
    public static class Config {

        private JoinType joinBy = JoinType.AND;
        private List<RecordEvaluatorDto> evaluators = Collections.emptyList();
    }

    public enum JoinType { AND, OR }
}
