package org.kiwiproject.dropwizard.jakarta.xml.ws;

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

class JakartaXmlWsBundleTest {

    Environment environment;
    Bootstrap<?> bootstrap;
    ServletEnvironment servletEnvironment;
    ServletRegistration.Dynamic servlet;
    JakartaXmlWsEnvironment jwsEnvironment;
    LifecycleEnvironment lifecycleEnvironment;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        bootstrap = mock(Bootstrap.class);
        servletEnvironment = mock(ServletEnvironment.class);
        servlet = mock(ServletRegistration.Dynamic.class);
        jwsEnvironment = mock(JakartaXmlWsEnvironment.class);
        lifecycleEnvironment = mock(LifecycleEnvironment.class);

        when(environment.servlets()).thenReturn(servletEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(bootstrap.getMetricRegistry()).thenReturn(mock(MetricRegistry.class));
        when(servletEnvironment.addServlet(anyString(), any(HttpServlet.class))).thenReturn(servlet);
        when(jwsEnvironment.buildServlet()).thenReturn(mock(HttpServlet.class));
        when(jwsEnvironment.getDefaultPath()).thenReturn("/soap");
    }

    @Test
    void constructorArgumentChecks() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JakartaXmlWsBundle<>(null, jwsEnvironment))
                .withMessage("Servlet path is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JakartaXmlWsBundle<>("soap", jwsEnvironment))
                .withMessage("soap is not an absolute path");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JakartaXmlWsBundle<>("/soap", null))
                .withMessage("jwsEnvironment is null");

        assertThatCode(() -> new JakartaXmlWsBundle<>("/soap", jwsEnvironment))
                .doesNotThrowAnyException();
    }

    @Test
    void initializeAndRun() {
        var jwsBundle = new JakartaXmlWsBundle<>("/soap", jwsEnvironment);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.run(null, null))
                .withMessage("Environment is null");

        jwsBundle.initialize(bootstrap);
        verify(jwsEnvironment).setInstrumentedInvokerBuilder(any(InstrumentedInvokerFactory.class));

        jwsBundle.run(null, environment);
        verify(servletEnvironment).addServlet(startsWith("CXF Servlet"), any(Servlet.class));
        verify(lifecycleEnvironment).addServerLifecycleListener(any(ServerLifecycleListener.class));
        verify(servlet).addMapping("/soap/*");
        verify(jwsEnvironment, never()).setPublishedEndpointUrlPrefix(anyString());
    }

    @Test
    void initializeAndRunWithPublishedEndpointUrlPrefix() {
        JakartaXmlWsBundle<?> jwsBundle = new JakartaXmlWsBundle<Configuration>("/soap", jwsEnvironment) {
            @Override
            protected String getPublishedEndpointUrlPrefix(Configuration configuration) {
                return "http://some/prefix";
            }
        };

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.run(null, null))
                .withMessage("Environment is null");

        jwsBundle.initialize(bootstrap);
        verify(jwsEnvironment).setInstrumentedInvokerBuilder(any(InstrumentedInvokerFactory.class));

        jwsBundle.run(null, environment);
        verify(servletEnvironment).addServlet(startsWith("CXF Servlet"), any(Servlet.class));
        verify(lifecycleEnvironment).addServerLifecycleListener(any(ServerLifecycleListener.class));
        verify(servlet).addMapping("/soap/*");
        verify(jwsEnvironment).setPublishedEndpointUrlPrefix("http://some/prefix");
    }

    @Test
    void publishEndpoint() {
        var jwsBundle = new JakartaXmlWsBundle<>("/soap", jwsEnvironment);
        var service = new Object();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.publishEndpoint(new EndpointBuilder("foo", null)))
                .withMessage("Service is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.publishEndpoint(new EndpointBuilder(null, service)))
                .withMessage("Path is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.publishEndpoint(new EndpointBuilder("   ", service)))
                .withMessage("Path is empty");

        var builder = mock(EndpointBuilder.class);
        jwsBundle.publishEndpoint(builder);
        verify(jwsEnvironment).publishEndpoint(builder);
    }

    @Test
    void getClient() {
        var jwsBundle = new JakartaXmlWsBundle<>("/soap", jwsEnvironment);

        Class<?> cls = Object.class;
        var url = "http://foo";

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.getClient(new ClientBuilder<>(null, null)))
                .withMessage("ServiceClass is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.getClient(new ClientBuilder<>(null, url)))
                .withMessage("ServiceClass is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.getClient(new ClientBuilder<>(cls, null)))
                .withMessage("Address is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwsBundle.getClient(new ClientBuilder<>(cls, " ")))
                .withMessage("Address is empty");

        var builder = new ClientBuilder<>(cls, url);
        jwsBundle.getClient(builder);
        verify(jwsEnvironment).getClient(builder);
    }
}
