package com.roskart.dropwizard.jaxws;

import org.apache.cxf.interceptor.Interceptor;
import org.junit.jupiter.api.Test;

import jakarta.xml.ws.handler.Handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClientBuilderTest {

    @Test
    void buildClient() {

        Handler<?> handler = mock(Handler.class);

        Interceptor<?> inInterceptor = mock(Interceptor.class);
        Interceptor<?> inFaultInterceptor = mock(Interceptor.class);
        Interceptor<?> outInterceptor = mock(Interceptor.class);
        Interceptor<?> outFaultInterceptor = mock(Interceptor.class);

        ClientBuilder<Object> builder = new ClientBuilder<>(Object.class, "address")
                .connectTimeout(1234)
                .receiveTimeout(5678)
                .handlers(handler, handler)
                .bindingId("binding id")
                .cxfInInterceptors(inInterceptor, inInterceptor)
                .cxfInFaultInterceptors(inFaultInterceptor, inFaultInterceptor)
                .cxfOutInterceptors(outInterceptor, outInterceptor)
                .cxfOutFaultInterceptors(outFaultInterceptor, outFaultInterceptor);

        assertThat(builder.getAddress()).isEqualTo("address");
        assertThat(builder.getServiceClass()).isEqualTo(Object.class);
        assertThat(builder.getConnectTimeout()).isEqualTo(1234);
        assertThat(builder.getReceiveTimeout()).isEqualTo(5678);
        assertThat(builder.getBindingId()).isEqualTo("binding id");
        assertThat(builder.getCxfInInterceptors()).contains(new Interceptor<?>[]{ inInterceptor, inInterceptor });
        assertThat(builder.getCxfInFaultInterceptors()).contains(new Interceptor<?>[]{ inFaultInterceptor, inFaultInterceptor });
        assertThat(builder.getCxfOutInterceptors()).contains(new Interceptor<?>[]{ outInterceptor, outInterceptor });
        assertThat(builder.getCxfOutFaultInterceptors()).contains(new Interceptor<?>[]{ outFaultInterceptor, outFaultInterceptor });
    }
}
