package ru.citeck.ecos.records3.record.dao.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ColumnsSourceId(val value: String = "")
