package com.codingchili.zapperflyasm.model;

import com.hazelcast.core.PartitionAware;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.codingchili.core.storage.Storable;

import static com.codingchili.zapperflyasm.model.ApiRequest.ID_TIME;

/**
 * @author Robin Duda
 *
 * A log record for build outputs.
 */
public class LogEvent implements Storable, PartitionAware<String> {
    private String id = UUID.randomUUID().toString();
    private Long time = ZonedDateTime.now().toInstant().toEpochMilli();
    private String build;
    private String line;


    @Override
    public String getId() {
        return id;
    }

    public LogEvent setId(String id) {
        this.id = id;
        return this;
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

    /**
     * @return primary index key for the log event.
     */
    public String getBuild() {
        return build;
    }

    public LogEvent setBuild(String build) {
        this.build = build;
        return this;
    }

    @Override
    public int compareToAttribute(Storable other, String attribute) {
        if (attribute.equals(ID_TIME)) {
            return time.compareTo(((LogEvent) other).time);
        } else {
            return 0;
        }
    }

    @Override
    public String getPartitionKey() {
        return build;
    }
}
