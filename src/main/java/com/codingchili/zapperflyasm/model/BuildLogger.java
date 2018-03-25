package com.codingchili.zapperflyasm.model;

/**
 * @author Robin Duda
 */
public interface BuildLogger {

    /**
     * Logs a message to a build log.
     *
     * @param line text message to write to the build log.
     *             The current date and time will be stored.
     */
    void log(String line);

}
