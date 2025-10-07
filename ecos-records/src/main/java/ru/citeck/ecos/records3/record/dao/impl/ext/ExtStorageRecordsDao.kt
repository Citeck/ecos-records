package ru.citeck.ecos.records3.record.dao.impl.ext

import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

open class ExtStorageRecordsDao<T : Any>(
    config: ExtStorageRecordsDaoConfig<T>
) : AbstractRecordsDao(), RecordsQueryDao, RecordAttsDao {

    private val idField: String = config.sourceId
    private val storageField: ExtStorage<T> = config.storage
    private val workspaceScoped = config.workspaceScoped

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

    override fun getRecordAtts(recordId: String): T? {
        return storageField.getById(recordId)
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
        if (!workspaceScoped || workspaces.isEmpty()) {
            return predicate
        }
        val fixedWorkspaces = workspaces.mapTo(HashSet()) {
            if (isWorkspaceWithGlobalArtifacts(it)) "" else it
        }
        val workspacesCondition = Predicates.inVals("workspace", fixedWorkspaces)
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
        val notEmptySorting = sorting.ifEmpty {
            listOf(SortBy(RecordConstants.ATT_CREATED, false))
        }
        return predicateService.filterAndSort(values, predicate, notEmptySorting, skip, max)
    }

    override fun getId(): String {
        return idField
    }

    private fun isWorkspaceWithGlobalArtifacts(workspace: String?): Boolean {
        return workspace.isNullOrBlank() ||
            workspace == "default" ||
            workspace.startsWith("admin$")
    }
}
