package ru.citeck.ecos.records3.test.record.dao.impl.proxy

import org.junit.jupiter.api.Test
import kotlin.reflect.full.functions

class ProxyJavaReflectionTest {

    @Test
    fun test() {
        print(ProxyJavaDescendant::class.functions)
    }
}
