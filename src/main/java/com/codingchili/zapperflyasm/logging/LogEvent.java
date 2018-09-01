package com.codingchili.zapperflyasm.logging;

import io.vertx.core.shareddata.Shareable;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * @author Robin Duda
 * <p>
 * A log record for build outputs.
 */
public class LogEvent implements Serializable, Shareable {
    private Long time = ZonedDateTime.now().toInstant().toEpochMilli();
    private String line;

    public LogEvent() {}

    /**
     * @param line the text representing the event that occured.
     */
    public LogEvent(String line) {
        this.line = line;
    }

    /**
     * @return the logged message.
     */
    public String getLine() {
        return line;
    }

    public LogEvent setLine(String line) {
        this.line = line;
        return this;
    }

    /**
     * @return the time of logging in epoch seconds.
     */
    public Long getTime() {
        return time;
    }

    public LogEvent setTime(Long time) {
        this.time = time;
        return this;
    }
}
