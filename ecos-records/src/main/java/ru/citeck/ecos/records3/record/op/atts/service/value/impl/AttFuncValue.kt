package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class AttFuncValue(val impl: (String) -> Any?) : AttValue {

    override fun getAtt(name: String): Any? {
        return impl.invoke(name)
    }
}
