package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static com.google.common.base.Preconditions.checkArgument;

import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.apache.cxf.jaxws.EndpointImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A Dropwizard bundle that enables Dropwizard applications to publish SOAP web services using
 * Jakarta XML Web Services and to create web service clients.
 */
public class JAXWSBundle<C> implements ConfiguredBundle<C> {

    protected static final String DEFAULT_PATH = "/soap";
    protected final JAXWSEnvironment jwsEnvironment;
    protected final String servletPath;

    /**
     * Create a new bundle instance. Service endpoints are published relative to '/soap'.
     */
    public JAXWSBundle() {
        this(DEFAULT_PATH);
    }

    /**
     * Create a new bundle instance. Service endpoints are published relative to the provided servletPath.
     *
     * @param servletPath Root path for service endpoints. Leading slash is required.
     */
    public JAXWSBundle(String servletPath) {
        this(servletPath, new JAXWSEnvironment(servletPath));
    }

    /**
     * Create a new bundle instance using the provided JAXWSEnvironment. Service endpoints are
     * published relative to the provided servletPath.
     *
     * @param servletPath    Root path for service endpoints. Leading slash is required.
     * @param jwsEnvironment Valid JAXWSEnvironment.
     */
    public JAXWSBundle(String servletPath, JAXWSEnvironment jwsEnvironment) {
        checkArgument(servletPath != null, "Servlet path is null");
        checkArgument(servletPath.startsWith("/"), "%s is not an absolute path", servletPath);
        checkArgument(jwsEnvironment != null, "jwsEnvironment is null");
        this.servletPath = servletPath.endsWith("/") ? servletPath + "*" : servletPath + "/*";
        this.jwsEnvironment = jwsEnvironment;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        this.jwsEnvironment.setInstrumentedInvokerBuilder(
                new InstrumentedInvokerFactory(bootstrap.getMetricRegistry()));
    }

    @Override
    public void run(C configuration, Environment environment) {
        checkArgument(environment != null, "Environment is null");
        environment.servlets().addServlet("CXF Servlet " + jwsEnvironment.getDefaultPath(),
                jwsEnvironment.buildServlet()).addMapping(servletPath);

        environment.lifecycle().addServerLifecycleListener(
                server -> jwsEnvironment.logEndpoints());

        String publishedEndpointUrlPrefix = getPublishedEndpointUrlPrefix(configuration);
        if (publishedEndpointUrlPrefix != null) {
            jwsEnvironment.setPublishedEndpointUrlPrefix(publishedEndpointUrlPrefix);
        }
    }

    /**
     * Publish Jakarta XML Web Services endpoint. Endpoint will be published relative to the CXF servlet path.
     *
     * @param endpointBuilder EndpointBuilder.
     * @return javax.xml.ws.Endpoint
     */
    public EndpointImpl publishEndpoint(EndpointBuilder endpointBuilder) {
        checkArgument(endpointBuilder != null, "EndpointBuilder is null");
        return this.jwsEnvironment.publishEndpoint(endpointBuilder);
    }

    /**
     * Factory method for creating Jakarta XML Web Services clients.
     *
     * @param clientBuilder ClientBuilder.
     * @param <T>           Service interface type.
     * @return Jakarta XML Web Services client proxy.
     */
    public <T> T getClient(ClientBuilder<T> clientBuilder) {
        checkArgument(clientBuilder != null, "ClientBuilder is null");
        return jwsEnvironment.getClient(clientBuilder);
    }

    /**
     * Extract the published endpoint URL prefix from the application configuration and return it to use the returned
     * value as the location of services in the published WSDLs.
     * <p>
     * Override this method to configure the bundle.
     *
     * @param configuration Application configuration.
     * @return Published endpoint URL prefix, or null if there is no prefix
     */
    @Nullable
    protected String getPublishedEndpointUrlPrefix(C configuration) {
        return null;
    }
}
