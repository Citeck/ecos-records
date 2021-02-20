package ru.citeck.ecos.records3.record.atts.value.impl

import ru.citeck.ecos.records3.record.atts.value.AttValue

open class AttValueDelegate(val impl: AttValue) : AttValue by impl
