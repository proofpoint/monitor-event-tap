package com.proofpoint.event.monitor;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Map;

@Immutable
public class Event
{
    private final String type;
    private final String uuid;
    private final String host;
    private final DateTime timestamp;
    private final Map<String, ?> data;

    @JsonCreator
    public Event(@JsonProperty("type") String type,
            @JsonProperty("uuid") String uuid,
            @JsonProperty("host") String host,
            @JsonProperty("timestamp") DateTime timestamp,
            @JsonProperty("data") Map<String, ?> data)
    {
        this.type = type;
        this.uuid = uuid;
        this.host = host;
        this.timestamp = timestamp;
        this.data = data;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9]*", message = "must be alphanumeric")
    public String getType()
    {
        return type;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getUuid()
    {
        return uuid;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getHost()
    {
        return host;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public DateTime getTimestamp()
    {
        return timestamp;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public Map<String, ?> getData()
    {
        return data;
    }
}
