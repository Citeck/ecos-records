package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

open class AttValueDelegate(val impl: AttValue) : AttValue by impl
