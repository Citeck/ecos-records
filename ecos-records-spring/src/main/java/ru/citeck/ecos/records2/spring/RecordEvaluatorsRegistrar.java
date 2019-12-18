package ru.citeck.ecos.records2.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorsService;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordEvaluatorsRegistrar {

    private List<RecordEvaluator<?, ?, ?>> evaluators;
    private final RecordEvaluatorsService recordEvaluatorsService;

    @PostConstruct
    public void register() {
        log.info("========================== RecordEvaluatorsRegistrar ==========================");
        if (evaluators != null) {
            evaluators.forEach(this::register);
        }
        log.info("========================= /RecordEvaluatorsRegistrar ==========================");
    }

    private void register(RecordEvaluator<?, ?, ?> evaluator) {
        log.info("Register: \"" + evaluator.getType() + "\" with class " + evaluator.getClass().getName());
        recordEvaluatorsService.register(evaluator);
    }

    @Autowired(required = false)
    public void setEvaluators(List<RecordEvaluator<?, ?, ?>> evaluators) {
        this.evaluators = evaluators;
    }
}
