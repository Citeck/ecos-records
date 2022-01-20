package ru.citeck.ecos.records3.test.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.cache.CacheConfig

class CacheTest {

    @Test
    fun test() {

        var loadCounter = 0L

        val services = RecordsServiceFactory()
        val cache = services.cacheManager.create(
            CacheConfig(
                "test-key",
                expireAfterWrite = 1000,
                maxItems = 10
            ),
            ""
        ) { key: String -> key + "-" + (++loadCounter).toString() }

        assertThat(cache.get("test")).isEqualTo("test-1")
        assertThat(cache.get("test")).isEqualTo("test-1")
        assertThat(cache.get("test")).isEqualTo("test-1")
        assertThat(cache.get("test")).isEqualTo("test-1")
        assertThat(cache.get("test")).isEqualTo("test-1")

        cache.update()

        assertThat(cache.get("test")).isEqualTo("test-1")

        Thread.sleep(1100)

        cache.update()

        assertThat(cache.get("test")).isEqualTo("test-2")
        assertThat(cache.get("test")).isEqualTo("test-2")

        assertThat(cache.size()).isEqualTo(1)
    }
}
