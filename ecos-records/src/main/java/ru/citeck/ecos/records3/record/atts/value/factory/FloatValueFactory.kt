package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter

class FloatValueFactory : AttValueFactory<Float> {

    private lateinit var doubleValueFactory: DoubleValueFactory

    override fun init(attValuesConverter: AttValuesConverter) {
        this.doubleValueFactory = attValuesConverter.getFactory(DoubleValueFactory::class.java)
    }

    override fun getValue(value: Float): AttValue {
        return doubleValueFactory.getValue(value.toDouble())
    }

    override fun getValueTypes() = listOf(java.lang.Float::class.java)
}
