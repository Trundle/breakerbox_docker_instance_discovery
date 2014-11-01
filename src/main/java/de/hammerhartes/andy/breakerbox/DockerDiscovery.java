package de.hammerhartes.andy.breakerbox;

import com.google.common.collect.ImmutableList;

import com.netflix.config.ConfigurationManager;
import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;
import com.sun.jersey.api.client.Client;

import de.hammerhartes.andy.breakerbox.command.GetContainersCommand;
import de.hammerhartes.andy.breakerbox.model.Container;
import de.hammerhartes.andy.breakerbox.model.Port;

import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class DockerDiscovery implements InstanceDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(DockerDiscovery.class);
    private static final String CLUSTER_NAME = "prod";
    private static final String DOCKER_HOSTS_KEY = "docker.hosts";
    private final Client client;
    private final List<URI> dockerHosts;

    public DockerDiscovery() {
        client = Client.create();
        dockerHosts = getDockerHosts();
    }

    @Override
    public Collection<Instance> getInstanceList() throws Exception {
        final ImmutableList.Builder<Instance> instances = ImmutableList.builder();
        for (final URI dockerHost : dockerHosts) {
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

    private static List<URI> getDockerHosts() {
        final AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        return Stream.of(config.getStringArray(DOCKER_HOSTS_KEY))
                .map(URI::create)
                .collect(toList());
    }
}
