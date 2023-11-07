package com.roskart.dropwizard.jaxws.example;

import com.roskart.dropwizard.jaxws.BasicAuthentication;
import com.roskart.dropwizard.jaxws.ClientBuilder;
import com.roskart.dropwizard.jaxws.EndpointBuilder;
import com.roskart.dropwizard.jaxws.JAXWSBundle;
import com.roskart.dropwizard.jaxws.example.auth.BasicAuthenticator;
import com.roskart.dropwizard.jaxws.example.core.Person;
import com.roskart.dropwizard.jaxws.example.db.PersonDAO;
import com.roskart.dropwizard.jaxws.example.resources.AccessMtomServiceResource;
import com.roskart.dropwizard.jaxws.example.resources.AccessProtectedServiceResource;
import com.roskart.dropwizard.jaxws.example.resources.AccessWsdlFirstServiceResource;
import com.roskart.dropwizard.jaxws.example.resources.WsdlFirstClientHandler;
import com.roskart.dropwizard.jaxws.example.ws.HibernateExampleService;
import com.roskart.dropwizard.jaxws.example.ws.JavaFirstService;
import com.roskart.dropwizard.jaxws.example.ws.JavaFirstServiceImpl;
import com.roskart.dropwizard.jaxws.example.ws.MtomServiceImpl;
import com.roskart.dropwizard.jaxws.example.ws.SimpleService;
import com.roskart.dropwizard.jaxws.example.ws.WsdlFirstServiceImpl;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.MtomService;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.WsdlFirstService;

public class JaxWsExampleApplication extends Application<JaxWsExampleApplicationConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(JaxWsExampleApplication.class);

    // HibernateBundle is used by HibernateExampleService
    private final HibernateBundle<JaxWsExampleApplicationConfiguration> hibernate = new HibernateBundle<JaxWsExampleApplicationConfiguration>(Person.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(JaxWsExampleApplicationConfiguration configuration) {
            return configuration.getDatabaseConfiguration();
        }
    };

    // JAX-WS Bundle
    private JAXWSBundle<Object> jaxWsBundle = new JAXWSBundle<>();
    private JAXWSBundle<Object> anotherJaxWsBundle = new JAXWSBundle<>("/api2");

    public static void main(String[] args) throws Exception {
        new JaxWsExampleApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<JaxWsExampleApplicationConfiguration> bootstrap) {
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(jaxWsBundle);
        bootstrap.addBundle(anotherJaxWsBundle);
    }

    @SuppressWarnings({ "UnusedAssignment", "java:S1854", "java:S125" })
    @Override
    public void run(JaxWsExampleApplicationConfiguration jaxWsExampleApplicationConfiguration, Environment environment) {

        // Hello world service
        @SuppressWarnings("unused")
        EndpointImpl e = jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/simple", new SimpleService()));

        // publishEndpoint returns javax.xml.ws.Endpoint to enable further customization.
        // e.getProperties().put(...);

        // Publish Hello world service again using different JAXWSBundle instance
        e = anotherJaxWsBundle.publishEndpoint(
                new EndpointBuilder("/simple", new SimpleService()));

        // Java first service protected with basic authentication
        e = jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/javafirst", new JavaFirstServiceImpl())
                        .authentication(new BasicAuthentication(new BasicAuthenticator(), "TOP_SECRET")));

        // WSDL first service using server side JAX-WS handler and CXF logging interceptors
        e = jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/wsdlfirst", new WsdlFirstServiceImpl())
                        .cxfInInterceptors(new LoggingInInterceptor())
                        .cxfOutInterceptors(new LoggingOutInterceptor()));

        // Service using Hibernate
        PersonDAO personDAO = new PersonDAO(hibernate.getSessionFactory());
        e = jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/hibernate",
                        new HibernateExampleService(personDAO))
                        .sessionFactory(hibernate.getSessionFactory()));

        // Publish the same service again using different JAXWSBundle instance
        e = anotherJaxWsBundle.publishEndpoint(
                new EndpointBuilder("/hibernate",
                        new HibernateExampleService(personDAO))
                        .sessionFactory(hibernate.getSessionFactory()));

        // WSDL first service using MTOM. Invoking enableMTOM on EndpointBuilder is not necessary
        // if you use @MTOM JAX-WS annotation on your service implementation class.
        e = jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/mtom", new MtomServiceImpl())
                        .enableMtom()
        );

        // RESTful resource that invokes WsdlFirstService on localhost and uses client side JAX-WS handler.
        environment.jersey().register(new AccessWsdlFirstServiceResource(
                jaxWsBundle.getClient(
                        new ClientBuilder<>(
                                WsdlFirstService.class,
                                "http://localhost:8080/soap/wsdlfirst")
                                .handlers(new WsdlFirstClientHandler()))));

        // RESTful resource that invokes MtomService on localhost
        environment.jersey().register(new AccessMtomServiceResource(
                jaxWsBundle.getClient(
                        new ClientBuilder<>(
                                MtomService.class,
                                "http://localhost:8080/soap/mtom")
                                .enableMtom())));

        // RESTful resource that invokes JavaFirstService on localhost and uses basic authentication and
        // client side CXF interceptors.
        environment.jersey().register(new AccessProtectedServiceResource(
                jaxWsBundle.getClient(
                        new ClientBuilder<>(
                                JavaFirstService.class,
                                "http://localhost:8080/soap/javafirst")
                                .cxfInInterceptors(new LoggingInInterceptor())
                                .cxfOutInterceptors(new LoggingOutInterceptor()))));

        environment.lifecycle().addServerLifecycleListener(server ->
                LOG.info("Jakarta XML Web Services Example is ready!"));
    }
}
