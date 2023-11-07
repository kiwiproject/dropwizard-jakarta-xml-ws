package com.roskart.dropwizard.jaxws;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JAXWSBundleTest {

    Environment environment = mock(Environment.class);
    Bootstrap<?> bootstrap = mock(Bootstrap.class);
    ServletEnvironment servletEnvironment = mock(ServletEnvironment.class);
    ServletRegistration.Dynamic servlet = mock(ServletRegistration.Dynamic.class);
    JAXWSEnvironment jaxwsEnvironment = mock(JAXWSEnvironment.class);
    LifecycleEnvironment lifecycleEnvironment = mock(LifecycleEnvironment.class);

    @BeforeEach
    void setUp() {
        when(environment.servlets()).thenReturn(servletEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(bootstrap.getMetricRegistry()).thenReturn(mock(MetricRegistry.class));
        when(servletEnvironment.addServlet(anyString(), any(HttpServlet.class))).thenReturn(servlet);
        when(jaxwsEnvironment.buildServlet()).thenReturn(mock(HttpServlet.class));
        when(jaxwsEnvironment.getDefaultPath()).thenReturn("/soap");
    }

    @Test
    void constructorArgumentChecks() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JAXWSBundle<>(null, jaxwsEnvironment))
                .withMessage("Servlet path is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JAXWSBundle<>("soap", jaxwsEnvironment))
                .withMessage("soap is not an absolute path");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JAXWSBundle<>("/soap", null))
                .withMessage("jaxwsEnvironment is null");

        assertThatCode(() -> new JAXWSBundle<>("/soap", jaxwsEnvironment))
                .doesNotThrowAnyException();
    }

    @Test
    void initializeAndRun() {
        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<>("/soap", jaxwsEnvironment);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.run(null, null))
                .withMessage("Environment is null");

        jaxwsBundle.initialize(bootstrap);
        verify(jaxwsEnvironment).setInstrumentedInvokerBuilder(any(InstrumentedInvokerFactory.class));

        jaxwsBundle.run(null, environment);
        verify(servletEnvironment).addServlet(startsWith("CXF Servlet"), any(Servlet.class));
        verify(lifecycleEnvironment).addServerLifecycleListener(any(ServerLifecycleListener.class));
        verify(servlet).addMapping("/soap/*");
        verify(jaxwsEnvironment, never()).setPublishedEndpointUrlPrefix(anyString());
    }

    @Test
    void initializeAndRunWithPublishedEndpointUrlPrefix() {
        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<Configuration>("/soap", jaxwsEnvironment) {
            @Override
            protected String getPublishedEndpointUrlPrefix(Configuration configuration) {
                return "http://some/prefix";
            }
        };

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.run(null, null))
                .withMessage("Environment is null");

        jaxwsBundle.initialize(bootstrap);
        verify(jaxwsEnvironment).setInstrumentedInvokerBuilder(any(InstrumentedInvokerFactory.class));

        jaxwsBundle.run(null, environment);
        verify(servletEnvironment).addServlet(startsWith("CXF Servlet"), any(Servlet.class));
        verify(lifecycleEnvironment).addServerLifecycleListener(any(ServerLifecycleListener.class));
        verify(servlet).addMapping("/soap/*");
        verify(jaxwsEnvironment).setPublishedEndpointUrlPrefix("http://some/prefix");
    }

    @Test
    void publishEndpoint() {

        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<>("/soap", jaxwsEnvironment);
        Object service = new Object();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.publishEndpoint(new EndpointBuilder("foo", null)))
                .withMessage("Service is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.publishEndpoint(new EndpointBuilder(null, service)))
                .withMessage("Path is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.publishEndpoint(new EndpointBuilder("   ", service)))
                .withMessage("Path is empty");

        EndpointBuilder builder = mock(EndpointBuilder.class);
        jaxwsBundle.publishEndpoint(builder);
        verify(jaxwsEnvironment).publishEndpoint(builder);
    }

    @Test
    void getClient() {

        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<>("/soap", jaxwsEnvironment);

        Class<?> cls = Object.class;
        String url = "http://foo";

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.getClient(new ClientBuilder<>(null, null)))
                .withMessage("ServiceClass is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.getClient(new ClientBuilder<>(null, url)))
                .withMessage("ServiceClass is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.getClient(new ClientBuilder<>(cls, null)))
                .withMessage("Address is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jaxwsBundle.getClient(new ClientBuilder<>(cls, " ")))
                .withMessage("Address is empty");

        ClientBuilder<?> builder = new ClientBuilder<>(cls, url);
        jaxwsBundle.getClient(builder);
        verify(jaxwsEnvironment).getClient(builder);
    }
}
