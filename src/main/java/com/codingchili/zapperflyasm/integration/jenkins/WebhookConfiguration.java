package com.codingchili.zapperflyasm.integration.jenkins;

import java.util.*;

/**
 * @author Robin Duda
 *
 * Configuration for the jenkins integration.
 */
public class WebhookConfiguration {
    private Set<String> whitelist = new HashSet<>(Arrays.asList("localhost", "127.0.0.1"));

    /**
     * @return a list of source IPs that are allowed to trigger a build.
     */
    public Set<String> getWhitelist() {
        return whitelist;
    }

    /**
     * @param whitelist a list of source IPs that are allowed to trigger a build.
     */
    public void setWhitelist(Set<String> whitelist) {
        this.whitelist = whitelist;
    }
}
