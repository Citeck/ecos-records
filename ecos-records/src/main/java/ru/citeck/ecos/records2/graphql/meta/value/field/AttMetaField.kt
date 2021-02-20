package ru.citeck.ecos.records2.graphql.meta.value.field

import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext

object AttMetaField : MetaField {

    override fun getInnerSchema(): String {
        return AttContext.getCurrentSchemaAttAsStr()
    }

    override fun getAlias(): String {
        return AttContext.getCurrentSchemaAtt().alias
    }

    override fun getName(): String {
        return AttContext.getCurrentSchemaAtt().name
    }

    override fun getAttributeSchema(field: String): String {
        return AttContext.getInnerAttsMap()[field] ?: ".str"
    }

    override fun getInnerAttributes(): List<String> {
        return ArrayList(AttContext.getInnerAttsMap().values)
    }

    override fun getInnerAttributesMap(): Map<String, String> {
        return AttContext.getInnerAttsMap()
    }

    override fun getInnerAttributesMap(withAliases: Boolean): Map<String, String> {
        return AttContext.getInnerAttsMap()
    }
}
