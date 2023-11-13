package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.commons.data.entity.EntityMeta
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter
import ru.citeck.ecos.records3.record.atts.value.impl.AttValueDelegate
import ru.citeck.ecos.webapp.api.entity.EntityRef

class EntityWithMetaValueFactory : AttValueFactory<EntityWithMeta<*>> {

    private lateinit var valuesConverter: AttValuesConverter

    override fun init(attValuesConverter: AttValuesConverter) {
        this.valuesConverter = attValuesConverter
    }

    override fun getValue(value: EntityWithMeta<*>): AttValue? {
        return valuesConverter.toAttValue(value.entity)?.let {
            EntityValue(it, value.meta)
        }
    }

    override fun getValueTypes() = listOf(EntityWithMeta::class.java)

    class EntityValue(
        entity: AttValue,
        private val meta: EntityMeta
    ) : AttValueDelegate(entity) {

        override fun getAtt(name: String): Any? {
            return when (name) {
                RecordConstants.ATT_MODIFIED -> meta.modified
                RecordConstants.ATT_MODIFIER -> EntityRef.create("emodel", "person", meta.modifier)
                RecordConstants.ATT_CREATED -> meta.created
                RecordConstants.ATT_CREATOR -> EntityRef.create("emodel", "person", meta.creator)
                else -> super.getAtt(name)
            }
        }
    }
}
