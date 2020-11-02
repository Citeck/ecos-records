package ru.citeck.ecos.records3.spring.utils.registrar

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.evaluator.RecordEvaluator
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService
import javax.annotation.PostConstruct

@Component
class RecordEvaluatorsRegistrar(
    private val recordEvaluatorService: RecordEvaluatorService
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private var evaluators: List<RecordEvaluator<*, *, *>>? = null

    @PostConstruct
    fun register() {
        log.info("========================== RecordEvaluatorsRegistrar ==========================")
        if (evaluators != null) {
            evaluators!!.forEach { this.register(it) }
        }
        log.info("========================= /RecordEvaluatorsRegistrar ==========================")
    }

    private fun register(evaluator: RecordEvaluator<*, *, *>) {
        log.info("Register: \"" + evaluator.type + "\" with class " + evaluator.javaClass.name)
        recordEvaluatorService.register(evaluator)
    }

    @Autowired(required = false)
    fun setEvaluators(evaluators: List<RecordEvaluator<*, *, *>>) {
        this.evaluators = evaluators
    }
}
