package ru.citeck.ecos.records3.record.mixin.impl.mutmeta

import java.time.Instant

data class MutMeta(
    val creator: String,
    val created: Instant,
    val modifier: String,
    val modified: Instant
)
