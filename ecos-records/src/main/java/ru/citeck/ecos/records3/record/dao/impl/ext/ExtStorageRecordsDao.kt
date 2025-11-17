package ru.citeck.ecos.records3.record.dao.impl.ext

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter
import ru.citeck.ecos.records3.record.atts.value.impl.AttValueDelegate
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.records3.workspace.RecordsWorkspaceService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

open class ExtStorageRecordsDao<T : Any>(
    config: ExtStorageRecordsDaoConfig<T>
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        private const val ATT_WORKSPACE = "workspace"
    }

    private val idField: String = config.sourceId
    private val storageField: ExtStorage<T> = config.storage
    private val workspaceScoped = config.workspaceScoped
    private lateinit var workspaceService: RecordsWorkspaceService

    private lateinit var attValuesConverter: AttValuesConverter

    private val getRefByIdImpl: (String) -> EntityRef = if (idField.contains("/")) {
        val appNameAndSrcId = this.idField.split("/");
        { EntityRef.create(appNameAndSrcId[0], appNameAndSrcId[1], it) }
    } else {
        { EntityRef.create(this.idField, it) }
    }

    init {
        val ecosType = config.ecosType
        if (ecosType.isNotBlank()) {
            val mixinAtts = setOf(RecordConstants.ATT_TYPE)
            val recordsType = EntityRef.create(AppName.EMODEL, "type", ecosType)
            addAttributesMixin(object : AttMixin {
                override fun getAtt(path: String, value: AttValueCtx): Any {
                    return recordsType
                }
                override fun getProvidedAtts(): Collection<String> {
                    return mixinAtts
                }
            })
        }
    }

    override fun getRecordsAtts(recordIds: List<String>): List<*> {
        val records = storageField.getByIds(recordIds)
        if (!workspaceScoped) {
            return records
        }
        val attValues = records.map { toWsScopedRecord(it) }
        if (AuthContext.isRunAsSystem()) {
            return attValues
        }
        val userWorkspaces = workspaceService.getUserOrWsSystemUserWorkspaces(
            AuthContext.getCurrentRunAsAuth()
        ) ?: return emptyList<Any>()
        val workspaces = recordsService.getAtts(attValues, listOf(ATT_WORKSPACE))
        return records.mapIndexed { index, record ->
            val recWs = workspaces[index].getAtt(ATT_WORKSPACE).asText()
            if (recWs.isBlank() || userWorkspaces.contains(recWs)) {
                record
            } else {
                null
            }
        }
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val page = recsQuery.page

        if (recsQuery.language.isEmpty()) {
            return getSortedValues(
                getAllRefs(),
                getPredicateWithWorkspacesFilter(VoidPredicate.INSTANCE, recsQuery.workspaces),
                recsQuery.sortBy,
                page.skipCount,
                page.maxItems
            )
        }
        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            return null
        }

        val predicate = recsQuery.getQuery(Predicate::class.java)

        val allRecords = getSortedValues(
            getAllRefs(),
            getPredicateWithWorkspacesFilter(predicate, recsQuery.workspaces),
            recsQuery.sortBy,
            0,
            -1
        )
        val result = RecsQueryRes<Any>()
        result.setTotalCount((allRecords.size).toLong())
        result.setHasMore(allRecords.size > (page.maxItems + page.skipCount))

        var resRecords = allRecords
        if (recsQuery.page.skipCount > 0) {
            resRecords = resRecords.drop(recsQuery.page.skipCount)
        }
        if (recsQuery.page.maxItems >= 0) {
            resRecords = resRecords.take(recsQuery.page.maxItems)
        }
        result.setRecords(resRecords)
        return result
    }

    private fun getPredicateWithWorkspacesFilter(predicate: Predicate, workspaces: List<String>): Predicate {
        if (!workspaceScoped) {
            return predicate
        }
        val workspacesCondition = workspaceService.buildAvailableWorkspacesPredicate(
            AuthContext.getCurrentRunAsAuth(),
            workspaces
        )
        if (PredicateUtils.isAlwaysFalse(workspacesCondition)) {
            return workspacesCondition
        }
        if (PredicateUtils.isAlwaysTrue(workspacesCondition)) {
            return predicate
        }
        return if (PredicateUtils.isAlwaysTrue(predicate)) {
            workspacesCondition
        } else {
            Predicates.and(predicate, workspacesCondition)
        }
    }

    fun getAllRefs(): List<EntityRef> {
        return storageField.getIds().map { getRefById(it) }
    }

    fun getRefById(id: String): EntityRef {
        return getRefByIdImpl(id)
    }

    fun getSortedValues(
        values: List<EntityRef>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int
    ): List<EntityRef> {
        if (PredicateUtils.isAlwaysFalse(predicate)) {
            return emptyList()
        }
        val notEmptySorting = sorting.ifEmpty {
            listOf(SortBy(RecordConstants.ATT_CREATED, false))
        }
        return predicateService.filterAndSort(values, predicate, notEmptySorting, skip, max)
    }

    override fun getId(): String {
        return idField
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        super.setRecordsServiceFactory(serviceFactory)
        workspaceService = serviceFactory.recordsWorkspaceService
        attValuesConverter = serviceFactory.attValuesConverter
    }

    private fun toWsScopedRecord(value: T?): Any {
        value ?: return EmptyAttValue.INSTANCE
        if (!workspaceScoped) {
            return value
        }
        return WsScopedRecord(attValuesConverter.toAttValue(value) ?: EmptyAttValue.INSTANCE)
    }

    class WsScopedRecord(value: AttValue) : AttValueDelegate(value) {
        override fun getAtt(name: String): Any? {
            if (name == RecordConstants.ATT_WORKSPACE) {
                var wsIdRaw = super.getAtt(ATT_WORKSPACE)
                if (wsIdRaw is DataValue) {
                    wsIdRaw = wsIdRaw.asJavaObj()
                }
                return if (wsIdRaw !is String || wsIdRaw.isBlank()) {
                    null
                } else {
                    EntityRef.create(AppName.EMODEL, "workspace", wsIdRaw)
                }
            }
            return super.getAtt(name)
        }
    }
}
