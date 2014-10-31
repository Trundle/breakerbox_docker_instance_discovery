package de.hammerhartes.andy.breakerbox;

import com.google.common.collect.ImmutableList;

import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;
import com.sun.jersey.api.client.Client;

import de.hammerhartes.andy.breakerbox.command.GetContainersCommand;
import de.hammerhartes.andy.breakerbox.model.Container;
import de.hammerhartes.andy.breakerbox.model.Port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;

public class DockerDiscovery implements InstanceDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(DockerDiscovery.class);
    private static final String CLUSTER_NAME = "prod";
    private static final Iterable<URI> DOCKER_HOSTS = ImmutableList.of(
            URI.create("http://localhost:2375"));
    private final Client client;

    public DockerDiscovery() {
        client = Client.create();
    }

    @Override
    public Collection<Instance> getInstanceList() throws Exception {
        final ImmutableList.Builder<Instance> instances = ImmutableList.builder();
        for (final URI dockerHost : DOCKER_HOSTS) {
            final List<Container> containers = new GetContainersCommand(client, dockerHost).execute();
            LOG.info("Found {} containers", containers.size());
            containers.stream()
                    .filter(container -> container.getPorts().size() == 1
                                         && container.getPorts().get(0).getPublicPort() > 0)
                    .forEach(container -> {
                        final Port port = container.getPorts().get(0);
                        final String host = format("%s:%d", dockerHost.getHost(), port.getPublicPort());
                        instances.add(new Instance(host, CLUSTER_NAME, true));
                    });
        }
        return instances.build();
    }
}
