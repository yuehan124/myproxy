package com.myproxy.update;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Factory for HttpClient instances that trust all SSL certificates.
 *
 * <p>Used by {@link UpdateService} and {@link TrackingService} to connect to
 * HTTPS servers with self-signed certificates or IP addresses without
 * hostname verification issues.</p>
 *
 * @author yuehan124@gmail.com
 * @since 2026-07-12
 */
public final class HttpClientFactory {

    private HttpClientFactory() {
    }

    /**
     * Create an HttpClient that trusts all SSL certificates and skips
     * hostname verification.
     *
     * @param connectTimeoutSeconds connection timeout in seconds
     * @return a trust-all HttpClient
     */
    public static HttpClient createTrustAllClient(int connectTimeoutSeconds) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, null);
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                    .build();
        }
    }
}
