package de.hammerhartes.andy.breakerbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicStringSetProperty;
import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;

import de.hammerhartes.andy.breakerbox.command.CheckForTenacityProperties;
import de.hammerhartes.andy.breakerbox.command.GetContainersCommand;
import de.hammerhartes.andy.breakerbox.model.Container;
import de.hammerhartes.andy.breakerbox.model.Port;

import org.apache.commons.configuration.AbstractConfiguration;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;

import static de.hammerhartes.andy.breakerbox.stream.Collectors.toImmutableList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.joda.time.Seconds.secondsBetween;

public class DockerDiscovery implements InstanceDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(DockerDiscovery.class);
    private static final String IMAGES_BLACKLIST_KEY = "blacklist.images";
    private static final String DOCKER_HOSTS_KEY = "docker.hosts";
    private static final String TENACITY_CHECK_CACHE_SIZE_KEY = "cache.tenacitycheck.size";
    private static final String TENACITY_CHECK_EXPIRY_KEY = "cache.tenacitycheck.expiry";
    private static final Pattern IMAGE_NAME = Pattern.compile("([^/]+?/)?([^:]++):.+");
    private final Client client;
    private final List<URI> dockerHosts;
    private final String clusterName;
    private final LoadingCache<HostAndPort, Boolean> tenacityCheckCache;
    private final List<Instance> staticInstances;
    private final DynamicStringSetProperty blacklistedImages;

    public DockerDiscovery() {
        client = new JerseyClientBuilder().build();
        dockerHosts = getDockerHosts();
        clusterName = getClusterName();
        tenacityCheckCache = createCache(client);
        staticInstances = getStaticInstances(clusterName);
        blacklistedImages = new DynamicStringSetProperty(IMAGES_BLACKLIST_KEY, Collections.emptySet());
    }

    @Override
    public Collection<Instance> getInstanceList() throws Exception {
        final ImmutableList.Builder<Instance> instances = ImmutableList.builder();
        instances.addAll(staticInstances);
        for (final URI dockerHost : dockerHosts) {
            final List<Container> containers = new GetContainersCommand(client, dockerHost).execute();
            LOG.info("Found {} containers", containers.size());
            final List<Instance> dockerInstances = containers.stream()
                    .filter(this::isNotBlacklisted)
                    .filter(this::exposesExactlyOnePublicPort)
                    .filter(this::serviceAlreadyStarted)
                    .map(container -> {
                        final Port port = container.getPorts().get(0);
                        final HostAndPort hostAndPort =
                                HostAndPort.fromParts(dockerHost.getHost(), port.getPublicPort());
                        LOG.debug("Container {} is reachable via {}", container, hostAndPort);
                        return hostAndPort;
                    })
                    .filter(this::isTenacityService)
                    .map((hostAndPort) -> new Instance(hostAndPort.toString(), clusterName, true))
                    .collect(toImmutableList());
            instances.addAll(dockerInstances);
        }
        return instances.build();
    }

    private boolean exposesExactlyOnePublicPort(final Container container) {
        return container.getPorts().size() == 1 && container.getPorts().get(0).getPublicPort() > 0;
    }

    private boolean serviceAlreadyStarted(final Container container) {
        // Simply give the service 60 seconds to start
        return secondsBetween(container.getCreated(), DateTime.now()).getSeconds() > 60;
    }

    private boolean isNotBlacklisted(final Container container) {
        final String imageName = stripHubAndVersion(container.getImage());
        return !blacklistedImages.get().contains(imageName);
    }

    private boolean isTenacityService(final HostAndPort hostAndPort) {
        try {
            final boolean providesMetricsStream = tenacityCheckCache.get(hostAndPort);
            LOG.debug("{} seems like a tenacity service: {}", hostAndPort, providesMetricsStream);
            return providesMetricsStream;
        } catch (ExecutionException e) {
            final String exceptionName = e.getCause().getClass().getSimpleName();
            LOG.warn(exceptionName + " while trying to check for tenacity properties at "
                     + hostAndPort.toString(),
                     e.getCause());
            return false;
        }
    }

    private static List<URI> getDockerHosts() {
        final AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        return Stream.of(config.getStringArray(DOCKER_HOSTS_KEY))
                .map(URI::create)
                .collect(toList());
    }

    private static LoadingCache<HostAndPort, Boolean> createCache(final Client client) {
        final AbstractConfiguration configuration = ConfigurationManager.getConfigInstance();
        return CacheBuilder.newBuilder()
                .expireAfterWrite(configuration.getInt(TENACITY_CHECK_EXPIRY_KEY, 60), TimeUnit.MINUTES)
                .maximumSize(configuration.getInt(TENACITY_CHECK_CACHE_SIZE_KEY, 500))
                .build(new CacheLoader<HostAndPort, Boolean>() {
                    @Override
                    public Boolean load(final HostAndPort key) throws Exception {
                        return new CheckForTenacityProperties(client, key).execute();
                    }
                });
    }

    private static String getClusterName() {
        final AbstractConfiguration configuration = ConfigurationManager.getConfigInstance();
        return configuration.getString("turbine.aggregator.clusterConfig", "prod");
    }

    private static List<Instance> getStaticInstances(final String clusterName) {
        final ImmutableList.Builder<Instance> instances = ImmutableList.builder();
        final AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        final String propertyName = format("turbine.ConfigPropertyBasedDiscovery.%s.instances", clusterName);
        for (String instance : config.getStringArray(propertyName)) {
            instances.add(new Instance(instance, clusterName, true));
        }
        return instances.build();
    }

    /**
     * Given some Docker image name in the form {@code hub.example.com/image:version}, strip the hub and the version and
     * simply return the image name.
     *
     * @param image the image name with version and possibly also a hub
     * @return the plain image name
     */
    @VisibleForTesting
    static String stripHubAndVersion(final String image) {
        final Matcher matcher = IMAGE_NAME.matcher(image);
        return matcher.matches()
               ? matcher.group(2)
               : image;
    }
}
