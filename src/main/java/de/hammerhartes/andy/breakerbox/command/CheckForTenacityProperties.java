package de.hammerhartes.andy.breakerbox.command;

import com.google.common.net.HostAndPort;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Checks whether the given host (and port) exposes a metrics stream.
 */
public class CheckForTenacityProperties extends HystrixCommand<Boolean> {

    private static Logger LOG = LoggerFactory.getLogger(CheckForTenacityProperties.class);

    private final Client client;
    private HostAndPort hostAndPort;
    private String metricsPath;

    public CheckForTenacityProperties(final Client client, final HostAndPort hostAndPort) {
        this(client, hostAndPort, "/tenacity/propertykeys");
    }

    public CheckForTenacityProperties(final Client client, final HostAndPort hostAndPort, final String metricsPath) {
        super(HystrixCommandGroupKey.Factory.asKey("CheckForTenacityProperties"));
        this.client = checkNotNull(client);
        this.hostAndPort = checkNotNull(hostAndPort);
        this.metricsPath = checkNotNull(metricsPath);
    }

    @Override
    protected Boolean run() throws Exception {
        final URI metricsStream = new URI("http", null, hostAndPort.getHostText(), hostAndPort.getPort(),
                                          metricsPath, null, null);
        try {
            final Response response = client.target(metricsStream).request().head();
            response.close();
            return response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
        } catch (ProcessingException e) {
            LOG.warn("ProcessingException while trying to check for tenacity properties at "
                     + hostAndPort.toString(),
                     e);
            return false;
        }
    }

    @Override
    protected Boolean getFallback() {
        if (isResponseTimedOut()) {
            LOG.warn("Timeout while trying to check for tenacity properties at " + hostAndPort.toString());
            return false;
        }
        return super.getFallback();
    }
}
