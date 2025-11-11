package ru.citeck.ecos.records3.test.record.atts.value.factory.interfacelookup

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory

class InterfaceLookupFactory : AttValueFactory<MyCustomType> {

    override fun getValue(value: MyCustomType): AttValue {
        return object : AttValue, MyCustomType by value {

            override fun getAtt(name: String): Any? {
                return when (name) {
                    "foo" -> getCustom()
                    else -> null
                }
            }
        }
    }

    override fun getValueTypes(): List<Class<*>> = listOf(MyCustomType::class.java)
}
