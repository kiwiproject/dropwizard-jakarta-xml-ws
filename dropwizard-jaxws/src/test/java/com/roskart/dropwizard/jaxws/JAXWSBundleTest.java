package com.roskart.dropwizard.jaxws;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        try {
            new JAXWSBundle<>(null, null);
            fail("expected IllegalArgumentException but no exception thrown");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

        try {
            new JAXWSBundle<>("soap", null);
            fail("expected IllegalArgumentException but no exception thrown");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void initializeAndRun() {
        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<>("/soap", jaxwsEnvironment);

        try {
            jaxwsBundle.run(null, null);
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

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

        try {
            jaxwsBundle.run(null, null);
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

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
        try {
            jaxwsBundle.publishEndpoint(new EndpointBuilder("foo", null));
            fail("expected IllegalArgumentException but no exception thrown");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

        try {
            jaxwsBundle.publishEndpoint(new EndpointBuilder(null, service));
            fail("expected IllegalArgumentException but no exception thrown");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

        try {
            jaxwsBundle.publishEndpoint(new EndpointBuilder("   ", service));
            fail("expected IllegalArgumentException but no exception thrown");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

        EndpointBuilder builder = mock(EndpointBuilder.class);
        jaxwsBundle.publishEndpoint(builder);
        verify(jaxwsEnvironment).publishEndpoint(builder);
    }

    @Test
    void getClient() {

        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<>("/soap", jaxwsEnvironment);

        Class<?> cls = Object.class;
        String url = "http://foo";

        try {
            jaxwsBundle.getClient(new ClientBuilder<>(null, null));
            fail("expected IllegalArgumentException but no exception thrown");
        }

        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

        try {
            jaxwsBundle.getClient(new ClientBuilder<>(null, url));
            fail("expected IllegalArgumentException but no exception thrown");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

        try {
            jaxwsBundle.getClient(new ClientBuilder<>(cls, "   "));
            fail("expected IllegalArgumentException but no exception thrown");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

        ClientBuilder<?> builder = new ClientBuilder<>(cls, url);
        jaxwsBundle.getClient(builder);
        verify(jaxwsEnvironment).getClient(builder);
    }
}
