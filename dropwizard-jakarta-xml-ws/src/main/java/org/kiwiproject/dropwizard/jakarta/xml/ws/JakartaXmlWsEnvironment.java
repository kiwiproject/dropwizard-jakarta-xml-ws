package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

import jakarta.servlet.http.HttpServlet;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs CXF Bus setup and provides methods for publishing Jakarta XML Web Services endpoints and creating
 * Jakarta XML Web Services clients.
 */
public class JakartaXmlWsEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger(JakartaXmlWsEnvironment.class);

    protected final Bus bus;
    protected final String defaultPath;
    private InstrumentedInvokerFactory instrumentedInvokerBuilder;
    private UnitOfWorkInvokerFactory unitOfWorkInvokerBuilder = new UnitOfWorkInvokerFactory();
    private String publishedEndpointUrlPrefix;

    public String getDefaultPath() {
        return this.defaultPath;
    }

    public JakartaXmlWsEnvironment(String defaultPath) {

        System.setProperty("org.apache.cxf.Logger", "org.apache.cxf.common.logging.Slf4jLogger");
        /*
        Instruct CXF to use CXFBusFactory instead of SpringBusFactory. CXFBusFactory provides ExtensionManagerBus
        which loads extension based on contents of META-INF/cxf/bus-extensions.txt file. Many CXF modules contain
        such file. When building shaded jar for dropwizard service, these files have to be merged into single
        bus-extension.txt file by using AppendingTransformer with Maven shade plugin.
        */
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, "org.apache.cxf.bus.CXFBusFactory");
        this.bus = BusFactory.newInstance().createBus();
        this.defaultPath = defaultPath.replace("/*", "");
    }

    public HttpServlet buildServlet() {
        var cxf = new CXFNonSpringServlet();
        cxf.setBus(bus);
        return cxf;
    }

    public void setPublishedEndpointUrlPrefix(String publishedEndpointUrlPrefix) {
        this.publishedEndpointUrlPrefix = publishedEndpointUrlPrefix;
    }

    public void setInstrumentedInvokerBuilder(InstrumentedInvokerFactory instrumentedInvokerBuilder) {
        this.instrumentedInvokerBuilder = instrumentedInvokerBuilder;
    }

    public void setUnitOfWorkInvokerBuilder(UnitOfWorkInvokerFactory unitOfWorkInvokerBuilder) {
        this.unitOfWorkInvokerBuilder = unitOfWorkInvokerBuilder;
    }

    protected BasicAuthenticationInterceptor createBasicAuthenticationInterceptor() {
        return new BasicAuthenticationInterceptor();
    }

    protected ValidatingInvoker createValidatingInvoker(Invoker invoker, Validator validator) {
        return new ValidatingInvoker(invoker, validator);
    }

    public void logEndpoints() {
        var serverRegistry = bus.getExtension(org.apache.cxf.endpoint.ServerRegistry.class);
        if (!serverRegistry.getServers().isEmpty()) {
            var endpoints = new StringBuilder();
            for (var server : serverRegistry.getServers()) {
                endpoints.append("    ")
                        .append(this.defaultPath)
                        .append(server.getEndpoint().getEndpointInfo().getAddress())
                        .append(" (")
                        .append(server.getEndpoint().getEndpointInfo().getInterface().getName())
                        .append(")\n");
            }
            LOG.info("Jakarta XML Web Services service endpoints [{}]:\n\n{}", this.defaultPath, endpoints);
        } else {
            LOG.info("Jakarta XML Web Services service endpoints were registered.");
        }
    }

    /**
     * Publish Jakarta XML Web Services server side endpoint. Returns the native CXF Endpoint
     * to allow further customization.
     */
    public EndpointImpl publishEndpoint(EndpointBuilder endpointBuilder) {
        checkArgument(nonNull(endpointBuilder), "EndpointBuilder is null");

        var cxfEndpoint = new EndpointImpl(bus, endpointBuilder.getService());
        if (nonNull(endpointBuilder.publishedEndpointUrl())) {
            cxfEndpoint.setPublishedEndpointUrl(endpointBuilder.publishedEndpointUrl());
        } else if (nonNull(publishedEndpointUrlPrefix)) {
            cxfEndpoint.setPublishedEndpointUrl(publishedEndpointUrlPrefix + endpointBuilder.getPath());
        }
        cxfEndpoint.publish(endpointBuilder.getPath());

        // MTOM support
        if (endpointBuilder.isMtomEnabled()) {
            ((SOAPBinding) cxfEndpoint.getBinding()).setMTOMEnabled(true);
        }

        var invoker = cxfEndpoint.getService().getInvoker();

        // validating invoker
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        invoker = this.createValidatingInvoker(invoker, validatorFactory.getValidator());

        if (nonNull(endpointBuilder.getSessionFactory())) {
            // Add invoker to handle UnitOfWork annotations. Note that this invoker is set up before
            // instrumented invoker(s) in order for instrumented invoker(s) to wrap "unit of work" invoker.
            invoker = unitOfWorkInvokerBuilder.create(
                    endpointBuilder.getService(), invoker, endpointBuilder.getSessionFactory());
            cxfEndpoint.getService().setInvoker(invoker);
        }

        // Replace CXF service invoker with instrumented invoker(s)
        invoker = instrumentedInvokerBuilder.create(endpointBuilder.getService(), invoker);
        cxfEndpoint.getService().setInvoker(invoker);

        if (nonNull(endpointBuilder.getAuthentication())) {
            // Configure CXF in interceptor to handle basic authentication
            var basicAuthInterceptor = this.createBasicAuthenticationInterceptor();
            basicAuthInterceptor.setAuthenticator(endpointBuilder.getAuthentication());
            cxfEndpoint.getInInterceptors().add(basicAuthInterceptor);
        }

        // CXF interceptors

        if (nonNull(endpointBuilder.getCxfInInterceptors())) {
            cxfEndpoint.getInInterceptors().addAll(endpointBuilder.getCxfInInterceptors());
        }

        if (nonNull(endpointBuilder.getCxfInFaultInterceptors())) {
            cxfEndpoint.getInFaultInterceptors().addAll(endpointBuilder.getCxfInFaultInterceptors());
        }

        if (nonNull(endpointBuilder.getCxfOutInterceptors())) {
            cxfEndpoint.getOutInterceptors().addAll(endpointBuilder.getCxfOutInterceptors());
        }

        if (nonNull(endpointBuilder.getCxfOutFaultInterceptors())) {
            cxfEndpoint.getOutFaultInterceptors().addAll(endpointBuilder.getCxfOutFaultInterceptors());
        }

        if (nonNull(endpointBuilder.getProperties())) {
            cxfEndpoint.getProperties().putAll(
                    endpointBuilder.getProperties());
        }

        return cxfEndpoint;
    }

    /**
     * Jakarta XML Web Services client factory
     *
     * @param clientBuilder ClientBuilder.
     * @param <T>           Service interface type.
     * @return Jakarta XML Web Services client proxy.
     */
    public <T> T getClient(ClientBuilder<T> clientBuilder) {

        var proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setServiceClass(clientBuilder.getServiceClass());
        proxyFactory.setAddress(clientBuilder.getAddress());

        // Jakarta XML Web Services handlers
        if (nonNull(clientBuilder.getHandlers())) {
            for (var h : clientBuilder.getHandlers()) {
                proxyFactory.getHandlers().add(h);
            }
        }

        // ClientProxyFactoryBean bindingId
        if (nonNull(clientBuilder.getBindingId())) {
            proxyFactory.setBindingId(clientBuilder.getBindingId());
        }

        // CXF interceptors
        if (nonNull(clientBuilder.getCxfInInterceptors())) {
            proxyFactory.getInInterceptors().addAll(clientBuilder.getCxfInInterceptors());
        }
        if (nonNull(clientBuilder.getCxfInFaultInterceptors())) {
            proxyFactory.getInFaultInterceptors().addAll(clientBuilder.getCxfInFaultInterceptors());
        }
        if (nonNull(clientBuilder.getCxfOutInterceptors())) {
            proxyFactory.getOutInterceptors().addAll(clientBuilder.getCxfOutInterceptors());
        }
        if (nonNull(clientBuilder.getCxfOutFaultInterceptors())) {
            proxyFactory.getOutFaultInterceptors().addAll(clientBuilder.getCxfOutFaultInterceptors());
        }

        var proxy = clientBuilder.getServiceClass().cast(proxyFactory.create());

        // MTOM support
        if (clientBuilder.isMtomEnabled()) {
            var bp = (BindingProvider) proxy;
            var binding = (SOAPBinding) bp.getBinding();
            binding.setMTOMEnabled(true);
        }

        var httpConduit = (HTTPConduit) ClientProxy.getClient(proxy).getConduit();
        var client = httpConduit.getClient();
        client.setConnectionTimeout(clientBuilder.getConnectTimeout());
        client.setReceiveTimeout(clientBuilder.getReceiveTimeout());

        return proxy;
    }
}
