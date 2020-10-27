package ru.citeck.ecos.records3.spring.web

import org.springframework.http.client.SimpleClientHttpRequestFactory
import ru.citeck.ecos.commons.utils.ExceptionUtils.throwException
import java.io.IOException
import java.net.HttpURLConnection
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

// Basically copied from
// org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet.SkipSslVerificationHttpRequestFactory
class SkipSslVerificationHttpRequestFactory : SimpleClientHttpRequestFactory() {

    @Throws(IOException::class)
    override fun prepareConnection(connection: HttpURLConnection, httpMethod: String) {
        if (connection is HttpsURLConnection) {
            try {
                connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                connection.sslSocketFactory = createSslSocketFactory()
            } catch (e: Exception) {
                throwException(e)
                throw RuntimeException(e)
            }
        }
        super.prepareConnection(connection, httpMethod)
    }

    @Throws(Exception::class)
    private fun createSslSocketFactory(): SSLSocketFactory {
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(SkipX509TrustManager()), SecureRandom())
        return context.socketFactory
    }

    private class SkipX509TrustManager : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    }
}
