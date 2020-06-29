package ru.citeck.ecos.records2.spring.web;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import ru.citeck.ecos.commons.utils.ExceptionUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

// Basically copied from
// org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet.SkipSslVerificationHttpRequestFactory
public class SkipSslVerificationHttpRequestFactory extends SimpleClientHttpRequestFactory {

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        if (connection instanceof HttpsURLConnection) {
            try {
                ((HttpsURLConnection) connection).setHostnameVerifier(
                    (String s, SSLSession sslSession) -> true);
                ((HttpsURLConnection) connection).setSSLSocketFactory(
                    this.createSslSocketFactory());
            } catch (Exception e) {
                ExceptionUtils.throwException(e);
                throw new RuntimeException(e);
            }
        }
        super.prepareConnection(connection, httpMethod);
    }

    private SSLSocketFactory createSslSocketFactory() throws Exception {
        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new SkipX509TrustManager()}, new SecureRandom());
        return context.getSocketFactory();
    }

    private static class SkipX509TrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
    }
}
