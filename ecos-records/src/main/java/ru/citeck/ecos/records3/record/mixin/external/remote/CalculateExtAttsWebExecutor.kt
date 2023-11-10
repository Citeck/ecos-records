package ru.citeck.ecos.records3.record.mixin.external.remote

import ru.citeck.ecos.commons.promise.Promises
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.ResolveArgs
import ru.citeck.ecos.records3.record.mixin.external.ExtAttHandlerContext
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutor
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorReq
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorResp
import java.time.Duration

class CalculateExtAttsWebExecutor(
    services: RecordsServiceFactory
) : EcosWebExecutor {

    companion object {
        const val PATH = "/records/calc-ext-atts"
    }

    private val extAttMixinService = services.extAttMixinService
    private val attSchemaResolver = services.attSchemaResolver
    private val recordAttsService = services.recordsAttsService

    override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
        val requestDto = request.getBodyReader().readDto(ReqBodyDto::class.java)
        response.getBodyWriter().writeDto(execute(requestDto))
    }

    fun execute(requestDto: ReqBodyDto): RespBodyDto {

        val handlerContext = ExtAttHandlerContextImpl()

        val results = ArrayList<List<Any?>>()
        val promises = ArrayList<Promise<*>>()
        requestDto.attributes.forEachIndexed { reqAttsIdx, typeAtts ->

            val emptyAtts: List<Any?> = typeAtts.atts.map { null }
            val ctx = extAttMixinService.getNonLocalExtMixinContext(typeAtts.typeId)

            results.add(emptyAtts)

            if (ctx != null) {
                promises.add(
                    Promises.all(
                        typeAtts.atts.map { reqAttData ->
                            ctx.getAtt(handlerContext, reqAttData.reqAtts, reqAttData.toLoad).then { rawValue ->
                                RequestContext.doWithCtx {
                                    recordAttsService.doWithSchema(
                                        reqAttData.toLoad.inner,
                                        true
                                    ) { schemaAtts ->
                                        attSchemaResolver.resolveRaw(
                                            ResolveArgs.create()
                                                .withValues(listOf(rawValue))
                                                .withRawAtts(true)
                                                .withAttributes(schemaAtts)
                                                .build()
                                        ).map { it.second }[0]
                                    }
                                }
                            }
                        }
                    ).then {
                        results.set(reqAttsIdx, it)
                    }
                )
            }
        }
        Promises.all(promises).get(Duration.ofSeconds(10))

        return RespBodyDto(results)
    }

    override fun getPath(): String {
        return PATH
    }

    override fun isReadOnly(): Boolean {
        return true
    }

    data class ReqBodyDto(
        val attributes: List<ReqTypeAtts>
    )

    data class ReqTypeAtts(
        val typeId: String,
        val atts: List<ReqAttData>
    )

    data class ReqAttData(
        val reqAtts: Map<String, Any?>,
        val toLoad: SchemaAtt
    )

    class RespBodyDto(
        val results: List<List<Any?>>
    )

    private class ExtAttHandlerContextImpl : ExtAttHandlerContext {
        private val ctxData = HashMap<Any, Any?>()
        override fun <T> computeIfAbsent(key: Any, action: (Any) -> T): T {
            @Suppress("UNCHECKED_CAST")
            return ctxData.computeIfAbsent(key, action) as T
        }
    }
}
