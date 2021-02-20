package ru.citeck.ecos.records3.record.atts.value.impl

import ru.citeck.ecos.records3.record.atts.value.AttValue

class AttFuncValue(val impl: (String) -> Any?) : AttValue {

    override fun getAtt(name: String): Any? {
        return impl.invoke(name)
    }
}
