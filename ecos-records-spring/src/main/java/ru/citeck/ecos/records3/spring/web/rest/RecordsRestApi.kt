package ru.citeck.ecos.records3.spring.web.rest

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordsServiceFactory
import ru.citeck.ecos.records2.request.result.RecordsResult
import ru.citeck.ecos.records2.utils.SecurityUtils
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.rest.RestHandlerAdapter
import ru.citeck.ecos.records3.rest.v1.RequestResp
import ru.citeck.ecos.records3.spring.utils.web.exception.RequestHandlingException
import ru.citeck.ecos.records3.spring.utils.web.exception.ResponseHandlingException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

@Api(
    description =
    "Service for universal querying an arbitrary data set (record) " +
    "from any available data source",
    tags = ["Records API"])
@RestController
@RequestMapping("/api/records")
class RecordsRestApi @Autowired constructor(private val services: RecordsServiceFactory) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val restHandlerAdapter: RestHandlerAdapter = services.restHandlerAdapter
    private var isProdProfile = true
    private var environment: Environment? = null
    private val ctxAttsSuppliers: MutableList<ContextAttributesSupplier> = CopyOnWriteArrayList()

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent?) {
        isProdProfile = environment != null && environment!!.acceptsProfiles("prod")
    }

    @PostMapping(value = ["/query"], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun recordsQuery(@ApiParam(value = "query") @RequestBody body: ByteArray): ByteArray? {
        val bodyData = convertRequest(body, ObjectData::class.java)
        val ctxAtts: MutableMap<String, Any?> = HashMap()
        ctxAttsSuppliers.forEach { s->
            val atts = s.attributes
            if (atts.isNotEmpty()) {
                ctxAtts.putAll(atts)
            }
        }
        return RequestContext.doWithCtx(services,
            {
                it.locale = LocaleContextHolder.getLocale()
                it.ctxAtts = ctxAtts
            }
        ) {
            encodeResponse(restHandlerAdapter.queryRecords(bodyData))
        }
    }

    @PostMapping(value = ["/mutate"], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun recordsMutate(@ApiParam(value = "change query text") @RequestBody body: ByteArray): ByteArray? {
        val mutationBody = convertRequest(body, ObjectData::class.java)
        val mutatedRecords = restHandlerAdapter.mutateRecords(mutationBody)
        return encodeResponse(mutatedRecords)
    }

    @PostMapping(value = ["/delete"], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun recordsDelete(@ApiParam(value = "query text") @RequestBody body: ByteArray): ByteArray? {
        val deletionBody = convertRequest(body, ObjectData::class.java)
        val deletedRecords = restHandlerAdapter.deleteRecords(deletionBody)
        return encodeResponse(deletedRecords)
    }

    private fun <T : Any> convertRequest(body: ByteArray, valueType: Class<T>): T {
        return try {
            Json.mapper.read(body, valueType)!!
        } catch (ioe: Exception) {
            log.error("Jackson cannot parse request body", ioe)
            throw RequestHandlingException(ioe)
        }
    }

    private fun encodeResponse(response: Any): ByteArray? {
        return try {
            if (isProdProfile) {
                if (response is RecordsResult<*>) {
                    SecurityUtils.encodeResult(response)
                } else if (response is RequestResp) {
                    SecurityUtils.encodeResult(response)
                }
            }
            Json.mapper.toBytes(response)
        } catch (jpe: Exception) {
            log.error("Jackson cannot write response body as bytes", jpe)
            throw ResponseHandlingException(jpe)
        }
    }

    fun registerContextAttsSupplier(supplier: ContextAttributesSupplier) {
        ctxAttsSuppliers.add(supplier)
    }

    @Autowired(required = false)
    fun setEnvironment(environment: Environment?) {
        this.environment = environment
    }
}
