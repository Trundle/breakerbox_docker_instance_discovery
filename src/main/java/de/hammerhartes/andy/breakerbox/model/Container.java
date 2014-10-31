package de.hammerhartes.andy.breakerbox.model;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

import javax.annotation.concurrent.Immutable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Immutable
public class Container {

    private final String id;
    private final DateTime created;
    private final String command;
    private final String image;
    private final List<Port> ports;

    @JsonCreator
    public Container(@JsonProperty("Id") final String id,
                     // XXX is there a better way?
                     @JsonProperty("Created") final long created,
                     @JsonProperty("Command") final String command,
                     @JsonProperty("Image") final String image,
                     @JsonProperty("Ports") final List<Port> ports) {
        this.id = id;
        this.created = new DateTime(created * 1000, DateTimeZone.UTC);
        this.command = command;
        this.image = image;
        this.ports = ports;
    }

    public String getId() {
        return id;
    }

    public DateTime getCreated() {
        return created;
    }

    public String getCommand() {
        return command;
    }

    public String getImage() {
        return image;
    }

    public List<Port> getPorts() {
        return ports;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("created", created)
                .add("command", command)
                .add("image", image)
                .add("ports", ports)
                .toString();
    }
}
