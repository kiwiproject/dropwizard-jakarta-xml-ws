package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.cxf.interceptor.Interceptor;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class EndpointBuilderTest {

    @Test
    void buildEndpoint() {
        Object service = new Object();
        String path = "/foo";
        String publishedUrl = "http://external/url";
        BasicAuthentication basicAuth = mock(BasicAuthentication.class);
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Interceptor<?> inInterceptor = mock(Interceptor.class);
        Interceptor<?> inFaultInterceptor = mock(Interceptor.class);
        Interceptor<?> outInterceptor = mock(Interceptor.class);
        Interceptor<?> outFaultInterceptor = mock(Interceptor.class);
        Map<String, Object> props = new HashMap<>();
        props.put("key", "value");

        EndpointBuilder builder = new EndpointBuilder(path, service)
                .publishedEndpointUrl(publishedUrl)
                .authentication(basicAuth)
                .sessionFactory(sessionFactory)
                .cxfInInterceptors(inInterceptor, inInterceptor)
                .cxfInFaultInterceptors(inFaultInterceptor, inFaultInterceptor)
                .cxfOutInterceptors(outInterceptor, outInterceptor)
                .cxfOutFaultInterceptors(outFaultInterceptor, outFaultInterceptor)
                .properties(props);

        assertThat(builder.getPath()).isEqualTo(path);
        assertThat(builder.getService()).isEqualTo(service);
        assertThat(builder.publishedEndpointUrl()).isEqualTo(publishedUrl);
        assertThat(builder.getAuthentication()).isEqualTo(basicAuth);
        assertThat(builder.getSessionFactory()).isEqualTo(sessionFactory);
        assertThat(builder.getCxfInInterceptors()).contains(inInterceptor, inInterceptor);
        assertThat(builder.getCxfInFaultInterceptors()).contains(inFaultInterceptor, inFaultInterceptor);
        assertThat(builder.getCxfOutInterceptors()).contains(outInterceptor, outInterceptor);
        assertThat(builder.getCxfOutFaultInterceptors()).contains(outFaultInterceptor, outFaultInterceptor);
        assertThat(builder.getProperties()).containsEntry("key", "value");
    }
}
