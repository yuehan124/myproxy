package com.myproxy.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Proxy configuration class for MyProxy application.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class ProxyConfig {

    public static final String DEFAULT_SERVER_URL = "https://124.156.206.40/myproxy";

    @JsonProperty("port")
    private int port = 6666;

    @JsonProperty("whitelistEnabled")
    private boolean whitelistEnabled = false;

    @JsonProperty("allowedIps")
    private Set<String> allowedIps = new LinkedHashSet<>();

    @JsonProperty("reverseProxyPort")
    private int reverseProxyPort = 6688;

    @JsonProperty("reverseProxyEnabled")
    private boolean reverseProxyEnabled = false;

    @JsonProperty("reverseProxyRules")
    private List<ReverseProxyRule> reverseProxyRules = new ArrayList<>();

    @JsonProperty("language")
    private String language = "auto";

    @JsonProperty("serverUrl")
    private String serverUrl = DEFAULT_SERVER_URL;

    @JsonProperty("autoUpdate")
    private boolean autoUpdate = true;

    public ProxyConfig() {
        allowedIps.add("127.0.0.1");
        allowedIps.add("0:0:0:0:0:0:0:1");

        // Default reverse proxy rule: local IP -> google.com
        reverseProxyRules.add(new ReverseProxyRule(NetUtils.getLocalIp(), "https://www.google.com"));
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public void setWhitelistEnabled(boolean whitelistEnabled) {
        this.whitelistEnabled = whitelistEnabled;
    }

    public Set<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(Set<String> allowedIps) {
        this.allowedIps = allowedIps;
    }

    public void addAllowedIp(String ip) {
        allowedIps.add(ip);
    }

    public void removeAllowedIp(String ip) {
        allowedIps.remove(ip);
    }

    public boolean isIpAllowed(String ip) {
        if (!whitelistEnabled) {
            return true;
        }
        return allowedIps.contains(ip);
    }

    public int getReverseProxyPort() {
        return reverseProxyPort;
    }

    public void setReverseProxyPort(int reverseProxyPort) {
        this.reverseProxyPort = reverseProxyPort;
    }

    public boolean isReverseProxyEnabled() {
        return reverseProxyEnabled;
    }

    public void setReverseProxyEnabled(boolean reverseProxyEnabled) {
        this.reverseProxyEnabled = reverseProxyEnabled;
    }

    public List<ReverseProxyRule> getReverseProxyRules() {
        return reverseProxyRules;
    }

    public void setReverseProxyRules(List<ReverseProxyRule> reverseProxyRules) {
        this.reverseProxyRules = reverseProxyRules;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Get the update URL, derived from {@code serverUrl + "/update"}.
     *
     * @return update URL
     */
    public String getUpdateUrl() {
        return serverUrl + "/update";
    }

    /**
     * Get the tracking URL, derived from {@code serverUrl + "/track"}.
     *
     * @return tracking URL
     */
    public String getTrackingUrl() {
        return serverUrl + "/track";
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public static class ReverseProxyRule {
        @JsonProperty("domain")
        private String domain;

        @JsonProperty("target")
        private String target;

        public ReverseProxyRule() {
        }

        public ReverseProxyRule(String domain, String target) {
            this.domain = domain;
            this.target = target;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }
}
