package de.hammerhartes.andy.breakerbox.command;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import de.hammerhartes.andy.breakerbox.model.Container;

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Gets the list of containers from a docker host.
 */
public class GetContainersCommand extends HystrixCommand<List<Container>> {

    private final Client client;
    private final URI dockerHost;

    public GetContainersCommand(final Client client, final URI dockerHost) {
        super(HystrixCommandGroupKey.Factory.asKey("GetContainersCommand"));
        this.client = checkNotNull(client);
        this.dockerHost = checkNotNull(dockerHost);
    }

    @Override
    protected List<Container> run() {
        return client.target(dockerHost.resolve("/containers/json"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<Container>>() {});
    }
}
