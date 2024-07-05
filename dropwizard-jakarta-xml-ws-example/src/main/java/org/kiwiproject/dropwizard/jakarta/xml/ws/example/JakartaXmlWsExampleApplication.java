package org.kiwiproject.dropwizard.jakarta.xml.ws.example;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.kiwiproject.dropwizard.jakarta.xml.ws.BasicAuthentication;
import org.kiwiproject.dropwizard.jakarta.xml.ws.ClientBuilder;
import org.kiwiproject.dropwizard.jakarta.xml.ws.EndpointBuilder;
import org.kiwiproject.dropwizard.jakarta.xml.ws.JakartaXmlWsBundle;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.auth.BasicAuthenticator;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.core.Person;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.db.PersonDAO;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources.AccessMtomServiceResource;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources.AccessProtectedServiceResource;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources.AccessWsdlFirstServiceResource;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources.WsdlFirstClientHandler;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws.HibernateExampleService;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws.JavaFirstService;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws.JavaFirstServiceImpl;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws.MtomServiceImpl;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws.SimpleService;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws.WsdlFirstServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.MtomService;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.WsdlFirstService;

public class JakartaXmlWsExampleApplication extends Application<JakartaXmlWsExampleConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(JakartaXmlWsExampleApplication.class);

    // HibernateBundle is used by HibernateExampleService
    private final HibernateBundle<JakartaXmlWsExampleConfiguration> hibernate = new HibernateBundle<>(Person.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(JakartaXmlWsExampleConfiguration configuration) {
            return configuration.getDatabaseConfiguration();
        }
    };

    // Jakarta XML Web Services Bundle
    private final JakartaXmlWsBundle<JakartaXmlWsExampleConfiguration> jwsBundle = new JakartaXmlWsBundle<>();
    private final JakartaXmlWsBundle<JakartaXmlWsExampleConfiguration> anotherJwsBundle = new JakartaXmlWsBundle<>("/api2");

    public static void main(String[] args) throws Exception {
        new JakartaXmlWsExampleApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<JakartaXmlWsExampleConfiguration> bootstrap) {
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(jwsBundle);
        bootstrap.addBundle(anotherJwsBundle);
    }

    @SuppressWarnings({ "UnusedAssignment", "java:S1854", "java:S125" })
    @Override
    public void run(JakartaXmlWsExampleConfiguration configuration, Environment environment) {

        // Hello world service
        @SuppressWarnings("unused")
        var endpoint = jwsBundle.publishEndpoint(
                new EndpointBuilder("/simple", new SimpleService()));

        // publishEndpoint returns javax.xml.ws.Endpoint to enable further customization.
        // e.getProperties().put(...);

        // Publish Hello world service again using different JakartaXmlWsBundle instance
        endpoint = anotherJwsBundle.publishEndpoint(
                new EndpointBuilder("/simple", new SimpleService()));

        // Java-first service protected with basic authentication
        endpoint = jwsBundle.publishEndpoint(
                new EndpointBuilder("/javafirst", new JavaFirstServiceImpl())
                        .authentication(new BasicAuthentication(new BasicAuthenticator(), "TOP_SECRET")));

        // WSDL first service using server side Jakarta XML Web Services handler and CXF logging interceptors.
        // The server handler is defined in the wsdlfirstservice-handlerchain.xml file, via the
        // HandlerChain annotation on WsdlFirstServiceImpl
        endpoint = jwsBundle.publishEndpoint(
                new EndpointBuilder("/wsdlfirst", new WsdlFirstServiceImpl())
                        .cxfInInterceptors(new LoggingInInterceptor())
                        .cxfOutInterceptors(new LoggingOutInterceptor()));

        // Service using Hibernate
        var personDAO = new PersonDAO(hibernate.getSessionFactory());
        endpoint = jwsBundle.publishEndpoint(
                new EndpointBuilder("/hibernate",
                        new HibernateExampleService(personDAO))
                        .sessionFactory(hibernate.getSessionFactory()));

        // Publish the same service again using different JakartaXmlWsBundle instance
        endpoint = anotherJwsBundle.publishEndpoint(
                new EndpointBuilder("/hibernate",
                        new HibernateExampleService(personDAO))
                        .sessionFactory(hibernate.getSessionFactory()));

        // WSDL first service using MTOM. Invoking enableMTOM on EndpointBuilder is not necessary
        // if you use @MTOM Jakarta XML Web Services annotation on your service implementation class.
        endpoint = jwsBundle.publishEndpoint(
                new EndpointBuilder("/mtom", new MtomServiceImpl())
                        .enableMtom()
        );

        // A RESTful resource that invokes WsdlFirstService on localhost and uses client side Jakarta XML Web Services handler.
        environment.jersey().register(new AccessWsdlFirstServiceResource(
                jwsBundle.getClient(
                        new ClientBuilder<>(
                                WsdlFirstService.class,
                                "http://localhost:8080/soap/wsdlfirst")
                                .handlers(new WsdlFirstClientHandler()))));

        // A RESTful resource that invokes MtomService on localhost
        environment.jersey().register(new AccessMtomServiceResource(
                jwsBundle.getClient(
                        new ClientBuilder<>(
                                MtomService.class,
                                "http://localhost:8080/soap/mtom")
                                .enableMtom())));

        // A RESTful resource that invokes JavaFirstService on localhost and uses basic authentication and
        // client side CXF interceptors.
        environment.jersey().register(new AccessProtectedServiceResource(
                jwsBundle.getClient(
                        new ClientBuilder<>(
                                JavaFirstService.class,
                                "http://localhost:8080/soap/javafirst")
                                .cxfInInterceptors(new LoggingInInterceptor())
                                .cxfOutInterceptors(new LoggingOutInterceptor()))));

        environment.lifecycle().addServerLifecycleListener(server ->
                LOG.info("Jakarta XML Web Services Example is ready!"));
    }
}
