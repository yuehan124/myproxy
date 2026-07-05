package com.myproxy.proxy;

import com.myproxy.config.ConfigManager;
import com.myproxy.config.ProxyConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Reverse proxy service for MyProxy application.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class ReverseProxyService {

    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConfigManager configManager;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private Consumer<String> logConsumer;
    private Consumer<Boolean> statusConsumer;

    public ReverseProxyService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public synchronized void start() {
        if (running.get()) {
            appendLog("[Reverse] Already running");
            return;
        }

        ProxyConfig config = configManager.getConfig();
        int port = config.getReverseProxyPort();
        List<ProxyConfig.ReverseProxyRule> rules = config.getReverseProxyRules();

        if (rules.isEmpty()) {
            appendLog("[Reverse] No rules configured, cannot start");
            return;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new ReverseProxyHandler());
                        }
                    });

            serverChannel = b.bind(new InetSocketAddress("0.0.0.0", port)).sync().channel();
            running.set(true);
            appendLog("[Reverse] Started on port: " + port + ", rules: " + rules.size());
            notifyStatus(true);
        } catch (Exception e) {
            logger.error("Failed to start reverse proxy", e);
            appendLog("[Reverse] Failed to start: " + e.getMessage());
            shutdown();
        }
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        shutdown();
        running.set(false);
        appendLog("[Reverse] Stopped");
        notifyStatus(false);
    }

    public synchronized void restart() {
        appendLog("[Reverse] Restarting...");
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

    private void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
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

    private ProxyConfig.ReverseProxyRule findRule(String host) {
        if (host == null) {
            return null;
        }
        // Strip port from host header
        int colonIdx = host.indexOf(':');
        String domain = colonIdx > 0 ? host.substring(0, colonIdx) : host;

        List<ProxyConfig.ReverseProxyRule> rules = configManager.getConfig().getReverseProxyRules();
        for (ProxyConfig.ReverseProxyRule rule : rules) {
            if (rule.getDomain().equalsIgnoreCase(domain)) {
                return rule;
            }
        }
        return null;
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

    private class ReverseProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String host = request.headers().get(HttpHeaderNames.HOST);
            String time = LocalDateTime.now().format(TIME_FMT);
            String clientIp = extractClientIp(ctx);

            // IP whitelist check
            ProxyConfig cfg = configManager.getConfig();
            if (cfg.isWhitelistEnabled() && !cfg.isIpAllowed(clientIp)) {
                appendLog(String.format("[%s] [Reverse] DENY %s %s %s", time, clientIp, host, request.uri()));
                FullHttpResponse resp = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN,
                        Unpooled.copiedBuffer("Forbidden: IP not allowed",
                                io.netty.util.CharsetUtil.UTF_8));
                resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
                resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
                ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
                return;
            }

            ProxyConfig.ReverseProxyRule rule = findRule(host);
            if (rule == null) {
                appendLog(String.format("[%s] [Reverse] No matching rule: %s %s", time, host, request.uri()));
                FullHttpResponse resp = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY,
                        Unpooled.copiedBuffer("No backend configured for: " + host,
                                io.netty.util.CharsetUtil.UTF_8));
                resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
                resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
                ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
                return;
            }

            appendLog(String.format("[%s] [Reverse] %s %s%s -> %s",
                    time, host, request.method(), request.uri(), rule.getTarget()));

            // Parse target
            String target = rule.getTarget();
            URI targetUri;
            try {
                targetUri = URI.create(target);
            } catch (Exception e) {
                sendError(ctx, "Invalid target: " + target);
                return;
            }

            String targetHost = targetUri.getHost();
            int targetPort = targetUri.getPort();
            if (targetPort <= 0) {
                targetPort = "https".equalsIgnoreCase(targetUri.getScheme()) ? 443 : 80;
            }
            String targetPath = targetUri.getPath();
            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = "";
            }

            // Build backend request
            String backendUri = targetPath + request.uri();
            FullHttpRequest backendReq = new DefaultFullHttpRequest(
                    request.protocolVersion(), request.method(), backendUri,
                    request.content().retain());
            backendReq.headers().set(request.headers());
            backendReq.headers().set(HttpHeaderNames.HOST, targetHost);

            // Connect to backend
            Bootstrap bootstrap = new Bootstrap();
            final int fTargetPort = targetPort;
            bootstrap.group(ctx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                            ch.pipeline().addLast(new BackendHandler(ctx));
                        }
                    });

            bootstrap.connect(targetHost, fTargetPort).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(backendReq);
                } else {
                    backendReq.release();
                    appendLog("[Reverse] Failed to connect backend: " + targetHost + ":" + fTargetPort);
                    sendError(ctx, "Backend unreachable: " + targetHost + ":" + fTargetPort);
                }
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.debug("Reverse proxy handler exception", cause);
            ctx.close();
        }

        private void sendError(ChannelHandlerContext ctx, String msg) {
            FullHttpResponse resp = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY,
                    Unpooled.copiedBuffer(msg, io.netty.util.CharsetUtil.UTF_8));
            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
            resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static class BackendHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final ChannelHandlerContext clientCtx;

        BackendHandler(ChannelHandlerContext clientCtx) {
            this.clientCtx = clientCtx;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
            FullHttpResponse clientResp = new DefaultFullHttpResponse(
                    response.protocolVersion(), response.status(),
                    response.content().retain());
            clientResp.headers().set(response.headers());
            clientResp.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
            clientResp.headers().set(HttpHeaderNames.CONTENT_LENGTH, clientResp.content().readableBytes());
            clientCtx.writeAndFlush(clientResp).addListener(ChannelFutureListener.CLOSE);
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
            clientCtx.close();
        }
    }
}
