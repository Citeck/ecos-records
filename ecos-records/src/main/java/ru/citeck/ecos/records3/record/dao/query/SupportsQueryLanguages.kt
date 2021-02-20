package ru.citeck.ecos.records3.record.dao.query

interface SupportsQueryLanguages : RecordsQueryDao {

    /**
     * Get query languages which can be used to query records in this DAO.
     * First languages in the result list are more preferred than last
     *
     * @return list of languages
     */
    fun getSupportedLanguages(): List<String>
}
