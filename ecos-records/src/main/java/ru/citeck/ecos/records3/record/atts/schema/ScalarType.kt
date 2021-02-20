package ru.citeck.ecos.records3.record.atts.schema

import java.util.*

enum class ScalarType(val schema: String) {

    ID("?id"),
    STR("?str"),
    DISP("?disp"),
    NUM("?num"),
    ASSOC("?assoc"),
    LOCAL_ID("?localId"),
    BOOL("?bool"),
    JSON("?json");

    companion object {
        @JvmStatic
        fun getBySchema(schema: String): Optional<ScalarType> {
            return Arrays.stream(values())
                .filter { it.schema == schema }
                .findFirst()
        }
    }
}
