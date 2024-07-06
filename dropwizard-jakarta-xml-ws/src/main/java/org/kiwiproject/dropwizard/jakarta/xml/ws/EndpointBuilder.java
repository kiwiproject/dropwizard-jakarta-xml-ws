package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.hibernate.SessionFactory;

import java.security.Principal;
import java.util.Map;

/**
 * Jakarta XML Web Services/CXF server endpoint builder.
 */
public class EndpointBuilder extends AbstractBuilder {

    private final String path;
    private final Object service;
    private String publishedEndpointUrl;
    SessionFactory sessionFactory;
    BasicAuthentication<? extends Principal> authentication;
    Map<String, Object> properties;

    public String getPath() {
        return path;
    }

    public Object getService() {
        return service;
    }

    public String publishedEndpointUrl() {
        return publishedEndpointUrl;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public BasicAuthentication<? extends Principal> getAuthentication() {
        return authentication;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Create new EndpointBuilder. Endpoint will be published relative to the CXF servlet path.
     *
     * @param path    Relative endpoint path.
     * @param service Service implementation.
     */
    public EndpointBuilder(String path, Object service) {
        checkArgument(nonNull(service), "Service is null");
        checkArgument(nonNull(path), "Path is null");
        checkArgument(!path.isBlank(), "Path is empty");
        if (!path.startsWith("local:")) { // local transport is used in tests
            path = (path.startsWith("/")) ? path : "/" + path;
        }
        this.path = path;
        this.service = service;
    }

    /**
     * Publish Jakarta XML Web Services endpoint with Dropwizard Hibernate Bundle integration.
     * The service will be scanned for @UnitOfWork annotations.
     *
     * @param sessionFactory Hibernate session factory.
     */
    public EndpointBuilder sessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        return this;
    }

    /**
     * Publish Jakarta XML Web Services protected endpoint using Dropwizard BasicAuthentication.
     *
     * @param authentication BasicAuthentication implementation.
     */
    public EndpointBuilder authentication(BasicAuthentication<? extends Principal> authentication) {
        this.authentication = authentication;
        return this;
    }

    @Override
    @SafeVarargs
    public final EndpointBuilder cxfInInterceptors(Interceptor<? extends Message>... interceptors) {
        return (EndpointBuilder) super.cxfInInterceptors(interceptors);
    }

    @Override
    @SafeVarargs
    public final EndpointBuilder cxfInFaultInterceptors(Interceptor<? extends Message>... interceptors) {
        return (EndpointBuilder) super.cxfInFaultInterceptors(interceptors);
    }

    @Override
    @SafeVarargs

    public final EndpointBuilder cxfOutInterceptors(Interceptor<? extends Message>... interceptors) {
        return (EndpointBuilder) super.cxfOutInterceptors(interceptors);
    }

    @Override
    @SafeVarargs
    public final EndpointBuilder cxfOutFaultInterceptors(Interceptor<? extends Message>... interceptors) {
        return (EndpointBuilder) super.cxfOutFaultInterceptors(interceptors);
    }

    /**
     * Invoking enableMTOM is not necessary if you use the {@link jakarta.xml.ws.soap.MTOM MTOM} Jakarta XML Web
     * Services annotation on your service implementation class.
     */
    @Override
    public EndpointBuilder enableMtom() {
        return (EndpointBuilder) super.enableMtom();
    }

    public EndpointBuilder publishedEndpointUrl(String publishedEndpointUrl) {
        this.publishedEndpointUrl = publishedEndpointUrl;
        return this;
    }

    /**
     * Provide a property bag to be supplied to the Jakarta XML Web Services endpoint.
     */
    public EndpointBuilder properties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }
}
