package com.myproxy.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lightweight usage tracking service that sends events to an nginx endpoint.
 *
 * <p>Events are sent as HTTP GET requests to
 * {@code {trackingUrl}?app=myproxy&version=x.y.z&event=xxx}. Nginx logs these
 * requests into a dedicated log file using a custom {@code log_format}. No
 * backend application is needed — nginx access logs serve as the data store.</p>
 *
 * <p>All tracking calls are fire-and-forget: failures are silently ignored
 * so they never affect application functionality.</p>
 *
 * @author yuehan124@gmail.com
 * @since 2026-07-12
 */
public class TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingService.class);

    private static final String APP_NAME = "myproxy";

    private final String trackingUrl;
    private final HttpClient httpClient;

    /**
     * @param trackingUrl base URL for tracking (e.g.
     *                    {@code https://124.156.206.40/track}), or
     *                    {@code null}/{@code empty} to disable tracking
     */
    public TrackingService(String trackingUrl) {
        this.trackingUrl = (trackingUrl != null && !trackingUrl.isBlank())
                ? trackingUrl.replaceAll("/+$", "")
                : null;
        this.httpClient = (this.trackingUrl != null)
                ? HttpClientFactory.createTrustAllClient(5)
                : null;
    }

    /**
     * Send a tracking event. Fire-and-forget; errors are logged at debug level.
     *
     * @param event the event name (e.g. {@code start}, {@code stop})
     */
    public void send(String event) {
        if (trackingUrl == null || httpClient == null) {
            return;
        }
        new Thread(() -> {
            try {
                String params = "app=" + URLEncoder.encode(APP_NAME, StandardCharsets.UTF_8)
                        + "&version=" + URLEncoder.encode(UpdateService.CURRENT_VERSION, StandardCharsets.UTF_8)
                        + "&event=" + URLEncoder.encode(event, StandardCharsets.UTF_8);
                URI uri = URI.create(trackingUrl + "?" + params);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                logger.debug("Tracking event sent: {}", event);
            } catch (Exception e) {
                logger.debug("Tracking event '{}' failed: {}", event, e.getMessage());
            }
        }, "tracking-" + event).start();
    }
}
