package ru.citeck.ecos.records3.record.request.msg

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class MsgType(
    val value: String
)
