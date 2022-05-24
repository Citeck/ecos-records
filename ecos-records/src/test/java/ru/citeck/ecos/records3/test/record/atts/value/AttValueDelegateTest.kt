package ru.citeck.ecos.records3.test.record.atts.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.impl.AttValueDelegate
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredFunctions

class AttValueDelegateTest {

    @Test
    fun methodsTest() {
        // delegate should implement all methods of AttValue
        assertThat(getFunctions(AttValueDelegate::class))
            .containsExactlyInAnyOrderElementsOf(getFunctions(AttValue::class))
    }

    private fun getFunctions(clazz: KClass<*>): Set<Func> {
        return clazz.declaredFunctions.map {
            Func(it.name, it.parameters.filter {
                p -> p.kind == KParameter.Kind.VALUE
            }.map {
                p -> p.type.classifier
            })
        }.toSet()
    }

    data class Func(
        val name: String,
        val params: List<KClassifier?>
    )
}
