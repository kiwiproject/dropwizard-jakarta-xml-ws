package com.roskart.dropwizard.jaxws;

import static com.google.common.base.Preconditions.checkArgument;

import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.xml.ws.handler.Handler;
import org.apache.cxf.jaxws.EndpointImpl;
import org.hibernate.SessionFactory;

/**
 * A Dropwizard bundle that enables Dropwizard applications to publish SOAP web services using
 * Jakarta XML Web Services and to create web service clients.
 */
public class JAXWSBundle<C> implements ConfiguredBundle<C> {

    protected static final String DEFAULT_PATH = "/soap";
    protected JAXWSEnvironment jaxwsEnvironment;
    protected String servletPath;

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
     * @param servletPath      Root path for service endpoints. Leading slash is required.
     * @param jaxwsEnvironment Valid JAXWSEnvironment.
     */
    public JAXWSBundle(String servletPath, JAXWSEnvironment jaxwsEnvironment) {
        checkArgument(servletPath != null, "Servlet path is null");
        checkArgument(servletPath.startsWith("/"), "%s is not an absolute path", servletPath);
        checkArgument(jaxwsEnvironment != null, "jaxwsEnvironment is null");
        this.servletPath = servletPath.endsWith("/") ? servletPath + "*" : servletPath + "/*";
        this.jaxwsEnvironment = jaxwsEnvironment;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        this.jaxwsEnvironment.setInstrumentedInvokerBuilder(
                new InstrumentedInvokerFactory(bootstrap.getMetricRegistry()));
    }

    @Override
    public void run(C configuration, Environment environment) {
        checkArgument(environment != null, "Environment is null");
        environment.servlets().addServlet("CXF Servlet " + jaxwsEnvironment.getDefaultPath(),
                jaxwsEnvironment.buildServlet()).addMapping(servletPath);

        environment.lifecycle().addServerLifecycleListener(
                server -> jaxwsEnvironment.logEndpoints());

        String publishedEndpointUrlPrefix = getPublishedEndpointUrlPrefix(configuration);
        if (publishedEndpointUrlPrefix != null) {
            jaxwsEnvironment.setPublishedEndpointUrlPrefix(publishedEndpointUrlPrefix);
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
        return this.jaxwsEnvironment.publishEndpoint(endpointBuilder);
    }

    /**
     * Publish Jakarta XML Web Services endpoint. Endpoint is published relative to the CXF servlet path.
     *
     * @param path    Relative endpoint path.
     * @param service Service implementation.
     * @return javax.xml.ws.Endpoint
     * @deprecated Use the {@link #publishEndpoint(EndpointBuilder)} publishEndpoint} method instead.
     */
    @Deprecated(since = "0.5.0")
    public EndpointImpl publishEndpoint(String path, Object service) {
        return this.publishEndpoint(path, service, null, null);
    }

    /**
     * Publish Jakarta XML Web Services endpoint with Dropwizard Hibernate Bundle integration. Service is
     * scanned for @UnitOfWork annotations. EndpointBuilder is published relative to the CXF servlet path.
     *
     * @param path           Relative endpoint path.
     * @param service        Service implementation.
     * @param sessionFactory Hibernate session factory.
     * @return javax.xml.ws.Endpoint
     * @deprecated Use the {@link #publishEndpoint(EndpointBuilder)} publishEndpoint} method instead.
     */
    @Deprecated(since = "0.5.0")
    public EndpointImpl publishEndpoint(String path, Object service, SessionFactory sessionFactory) {
        return this.publishEndpoint(path, service, null, sessionFactory);
    }

    /**
     * Publish Jakarta XML Web Services protected endpoint using Dropwizard BasicAuthentication.
     * EndpointBuilder is published relative to the CXF servlet path.
     *
     * @param path           Relative endpoint path.
     * @param service        Service implementation.
     * @param authentication BasicAuthentication implementation.
     * @return javax.xml.ws.Endpoint
     * @deprecated Use the {@link #publishEndpoint(EndpointBuilder)} publishEndpoint} method instead.
     */
    @Deprecated(since = "0.5.0")
    public EndpointImpl publishEndpoint(String path, Object service, BasicAuthentication authentication) {
        return this.publishEndpoint(path, service, authentication, null);
    }

    /**
     * Publish Jakarta XML Web Services protected endpoint using Dropwizard BasicAuthentication with
     * Dropwizard Hibernate Bundle integration. Service is scanned for @UnitOfWork annotations. EndpointBuilder
     * is published relative to the CXF servlet path.
     *
     * @param path           Relative endpoint path.
     * @param service        Service implementation.
     * @param auth           BasicAuthentication implementation.
     * @param sessionFactory Hibernate session factory.
     * @return javax.xml.ws.Endpoint
     * @deprecated Use the {@link #publishEndpoint(EndpointBuilder)} publishEndpoint} method instead.
     */
    @Deprecated(since = "0.5.0")
    public EndpointImpl publishEndpoint(String path, Object service, BasicAuthentication auth,
                                        SessionFactory sessionFactory) {
        checkArgument(service != null, "Service is null");
        checkArgument(path != null, "Path is null");
        checkArgument((path).trim().length() > 0, "Path is empty");
        return this.publishEndpoint(new EndpointBuilder(path, service)
                .authentication(auth)
                .sessionFactory(sessionFactory)
        );
    }

    /**
     * Factory method for creating Jakarta XML Web Services clients.
     *
     * @param serviceClass Service interface class.
     * @param address      Endpoint URL address.
     * @param handlers     Client side Jakarta XML Web Services handlers. Optional.
     * @param <T>          Service interface type.
     * @return Jakarta XML Web Services client proxy.
     * @deprecated Use the {@link #getClient(ClientBuilder)} getClient} method instead.
     */
    @Deprecated(since = "0.5.0")
    public <T> T getClient(Class<T> serviceClass, String address, Handler... handlers) {
        checkArgument(serviceClass != null, "ServiceClass is null");
        checkArgument(address != null, "Address is null");
        checkArgument((address).trim().length() > 0, "Address is empty");
        return jaxwsEnvironment.getClient(
                new ClientBuilder<>(serviceClass, address).handlers(handlers));
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
        return jaxwsEnvironment.getClient(clientBuilder);
    }

    /**
     * Extract the published endpoint URL prefix from the application configuration and return it to use the returned
     * value as the location of services in the published WSDLs.
     * <p>
     * Override this method to configure the bundle.
     *
     * @param configuration Application configuration.
     * @return Published endpoint URL prefix.
     */
    protected String getPublishedEndpointUrlPrefix(C configuration) {
        return null;
    }
}
