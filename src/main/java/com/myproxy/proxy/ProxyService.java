package com.myproxy.proxy;

import com.myproxy.config.ConfigManager;
import com.myproxy.config.ProxyConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Forward proxy service for MyProxy application.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class ProxyService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConfigManager configManager;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private HttpProxyServer proxyServer;
    private Consumer<String> logConsumer;
    private Consumer<Boolean> statusConsumer;

    public ProxyService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public synchronized void start() {
        if (running.get()) {
            appendLog("Proxy already running");
            return;
        }

        ProxyConfig config = configManager.getConfig();
        int port = config.getPort();

        try {
            proxyServer = DefaultHttpProxyServer.bootstrap()
                    .withAddress(new InetSocketAddress("0.0.0.0", port))
                    .withAllowLocalOnly(false)
                    .withFiltersSource(new HttpFiltersSourceAdapter() {
                        @Override
                        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                            String clientIp = extractClientIp(ctx);
                            String method = originalRequest.method().name();
                            String uri = originalRequest.uri();
                            String time = LocalDateTime.now().format(TIME_FMT);

                            // IP whitelist check
                            ProxyConfig cfg = configManager.getConfig();
                            if (cfg.isWhitelistEnabled() && !cfg.isIpAllowed(clientIp)) {
                                appendLog(String.format("[%s] DENY %s %s %s", time, clientIp, method, uri));
                                return new HttpFiltersAdapter(originalRequest) {
                                    @Override
                                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                        return new DefaultFullHttpResponse(
                                                HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
                                    }
                                };
                            }

                            appendLog(String.format("[%s] %s %s %s", time, clientIp, method, uri));
                            return new HttpFiltersAdapter(originalRequest);
                        }

                        @Override
                        public int getMaximumRequestBufferSizeInBytes() {
                            return 0; // No buffering
                        }

                        @Override
                        public int getMaximumResponseBufferSizeInBytes() {
                            return 0;
                        }
                    })
                    .start();

            running.set(true);
            appendLog("Proxy server started on port: " + port);
            notifyStatus(true);
        } catch (Exception e) {
            logger.error("Failed to start proxy", e);
            appendLog("Failed to start: " + e.getMessage());
            running.set(false);
            notifyStatus(false);
        }
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        try {
            if (proxyServer != null) {
                proxyServer.stop();
                proxyServer = null;
            }
            running.set(false);
            appendLog("Proxy server stopped");
            notifyStatus(false);
        } catch (Exception e) {
            logger.error("Failed to stop proxy", e);
            appendLog("Failed to stop: " + e.getMessage());
        }
    }

    public synchronized void restart() {
        appendLog("Restarting...");
        stop();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        start();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setLogConsumer(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    public void setStatusConsumer(Consumer<Boolean> statusConsumer) {
        this.statusConsumer = statusConsumer;
    }

    private void appendLog(String message) {
        logger.info(message);
        if (logConsumer != null) {
            SwingUtilities.invokeLater(() -> logConsumer.accept(message));
        }
    }

    private void notifyStatus(boolean isRunning) {
        if (statusConsumer != null) {
            SwingUtilities.invokeLater(() -> statusConsumer.accept(isRunning));
        }
    }

    private String extractClientIp(ChannelHandlerContext ctx) {
        if (ctx != null && ctx.channel() != null && ctx.channel().remoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
            if (addr.getAddress() != null) {
                return addr.getAddress().getHostAddress();
            }
        }
        return "unknown";
    }
}
