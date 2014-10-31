package de.hammerhartes.andy.breakerbox.model;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Port {

    public enum Type {
        TCP,
        UDP;

        @JsonCreator
        public static Type fromValue(final String value) {
            return valueOf(value.toUpperCase());
        }
    }

    private final String ip;
    private final int privatePort;
    private final int publicPort;
    private final Type type;

    @JsonCreator
    public Port(@JsonProperty("IP") final String ip,
                @JsonProperty("PrivatePort") final int privatePort,
                @JsonProperty("PublicPort") final int publicPort,
                @JsonProperty("Type") final Type type) {
        this.ip = ip;
        this.privatePort = privatePort;
        this.publicPort = publicPort;
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public int getPrivatePort() {
        return privatePort;
    }

    public int getPublicPort() {
        return publicPort;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("ip", ip)
                .add("privatePort", privatePort)
                .add("publicPort", publicPort)
                .add("type", type)
                .toString();
    }
}
