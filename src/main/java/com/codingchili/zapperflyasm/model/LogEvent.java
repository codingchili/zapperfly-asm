package com.codingchili.zapperflyasm.model;

import java.io.Serializable;

/**
 * @author Robin Duda
 *
 * A log record for build outputs.
 */
public class LogEvent implements Serializable {
    private String line;
    private Long time;

    /**
     * @return the logged message.
     */
    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    /**
     * @return the time of logging in epoch seconds.
     */
    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
