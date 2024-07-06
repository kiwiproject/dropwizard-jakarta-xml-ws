package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.HashMap;

import javax.wsdl.WSDLException;

class JakartaXmlWsEnvironmentTest {

    private static final String SOAP_REQUEST_FILE_NAME = "test-soap-request.xml";

    private JakartaXmlWsEnvironment jwsEnvironment;
    private Invoker mockInvoker;
    private TestUtilities testutils;
    private DummyService service;
    InstrumentedInvokerFactory mockInvokerBuilder;
    UnitOfWorkInvokerFactory mockUnitOfWorkInvokerBuilder;
    private int mockBasicAuthInterceptorInvoked;


    // DummyInterface is used by getClient tests
    @WebService
    public interface DummyInterface {
        @WebMethod
        @SuppressWarnings("unused")
        void foo();
    }

    // TestInterceptor is used for testing CXF interceptors
    static class TestInterceptor extends AbstractPhaseInterceptor<Message> {
        private int invocationCount = 0;

        public TestInterceptor(String phase) {
            super(phase);
        }

        public int getInvocationCount() {
            return this.invocationCount;
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            invocationCount++;
        }
    }

    @BeforeEach
    void setup() {

        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.apache.cxf")).setLevel(Level.INFO);

        mockInvoker = mock(Invoker.class);
        testutils = new TestUtilities(JakartaXmlWsEnvironmentTest.class);
        service = new DummyService();
        mockInvokerBuilder = mock(InstrumentedInvokerFactory.class);
        mockUnitOfWorkInvokerBuilder = mock(UnitOfWorkInvokerFactory.class);

        jwsEnvironment = new JakartaXmlWsEnvironment("soap") {
            /*
            We create BasicAuthenticationInterceptor mock manually, because Mockito provided mock
            does not get invoked by CXF
            */
            @Override
            protected <P extends Principal> BasicAuthenticationInterceptor<P> createBasicAuthenticationInterceptor() {
                return new BasicAuthenticationInterceptor<>() {
                    @Override
                    public void handleMessage(Message message) throws Fault {
                        mockBasicAuthInterceptorInvoked++;
                    }
                };
            }
        };

        when(mockInvokerBuilder.create(any(), any(Invoker.class))).thenReturn(mockInvoker);
        jwsEnvironment.setInstrumentedInvokerBuilder(mockInvokerBuilder);

        when(mockUnitOfWorkInvokerBuilder
                .create(any(), any(Invoker.class), any(SessionFactory.class)))
                .thenReturn(mockInvoker);
        jwsEnvironment.setUnitOfWorkInvokerBuilder(mockUnitOfWorkInvokerBuilder);

        mockBasicAuthInterceptorInvoked = 0;

        testutils.setBus(jwsEnvironment.bus);
        testutils.addNamespace("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        testutils.addNamespace("a", "http://ws.xml.jakarta.dropwizard.kiwiproject.org/");
    }

    @AfterEach
    void teardown() {
        jwsEnvironment.bus.shutdown(false);
    }

    @Test
    void buildServlet() {
        var result = jwsEnvironment.buildServlet();

        assertAll(
                () -> assertThat(result).isInstanceOf(CXFNonSpringServlet.class),
                () -> assertThat(((CXFNonSpringServlet) result).getBus()).isInstanceOf(Bus.class)
        );
    }

    @Test
    void publishEndpoint() throws Exception {
        var endpoint = jwsEnvironment.publishEndpoint(new EndpointBuilder("local://path", service));
        assertThat(endpoint).isNotNull();

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        var soapResponseNode = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponseNode);
    }

    @Test
    void publishEndpointWithAnotherEnvironment() throws Exception {

        // creating new runtime environment simulates using separate bundles
        var anotherJwsEnvironment = new JakartaXmlWsEnvironment("soap2");
        anotherJwsEnvironment.setInstrumentedInvokerBuilder(mockInvokerBuilder);
        anotherJwsEnvironment.setUnitOfWorkInvokerBuilder(mockUnitOfWorkInvokerBuilder);

        testutils.setBus(anotherJwsEnvironment.bus);

        anotherJwsEnvironment.publishEndpoint(new EndpointBuilder("local://path", service));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        var soapResponseNode = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponseNode);
    }

    @Test
    void publishEndpointWithAuthentication() throws Exception {
        BasicAuthentication<? extends Principal> authentication = mock();

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .authentication(authentication));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        var soapResponseNode = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponseNode);

        assertThat(mockBasicAuthInterceptorInvoked).isEqualTo(1);
    }

    @Test
    void publishEndpointWithHibernateInvoker() throws Exception {
        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .sessionFactory(mock(SessionFactory.class)));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verify(mockUnitOfWorkInvokerBuilder).create(any(), any(Invoker.class), any(SessionFactory.class));

        var soapResponseNode = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponseNode);
    }

    @Test
    void publishEndpointWithCxfInterceptors() throws Exception {
        var inInterceptor = new TestInterceptor(Phase.UNMARSHAL);
        var inInterceptor2 = new TestInterceptor(Phase.PRE_INVOKE);
        var outInterceptor = new TestInterceptor(Phase.MARSHAL);

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .cxfInInterceptors(inInterceptor, inInterceptor2)
                        .cxfOutInterceptors(outInterceptor));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));

        var soapResponseNode = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());
        assertAll(
                () -> assertThat(inInterceptor.getInvocationCount()).isEqualTo(1),
                () -> assertThat(inInterceptor2.getInvocationCount()).isEqualTo(1),
                () -> assertThat(outInterceptor.getInvocationCount()).isEqualTo(1)
        );

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponseNode);

        soapResponseNode = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker, times(2)).invoke(any(Exchange.class), any());
        assertAll(
                () -> assertThat(inInterceptor.getInvocationCount()).isEqualTo(2),
                () -> assertThat(inInterceptor2.getInvocationCount()).isEqualTo(2),
                () -> assertThat(outInterceptor.getInvocationCount()).isEqualTo(2)
        );

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponseNode);
    }


    @Test
    void publishEndpointWithMtom() throws Exception {
        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .enableMtom());

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));

        var bytes = testutils.invokeBytes("local://path", LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        var mimeMultipart = new MimeMultipart(new ByteArrayDataSource(bytes,
                "application/xop+xml; charset=UTF-8; type=\"text/xml\""));
        assertThat(mimeMultipart.getCount()).isEqualTo(1);
        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse",
                StaxUtils.read(mimeMultipart.getBodyPart(0).getInputStream()));
    }

    @Test
    void publishEndpointWithCustomPublishedUrl() throws Exception {

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .publishedEndpointUrl("http://external.server/external/path")
        );

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        var server = testutils.getServerForAddress("local://path");
        var destination = (AbstractDestination) server.getDestination();
        var publishedEndpointUrl = destination.getEndpointInfo()
                .getProperty(WSDLGetUtils.PUBLISHED_ENDPOINT_URL, String.class);

        assertThat(publishedEndpointUrl).isEqualTo("http://external.server/external/path");
    }

    @Test
    void publishEndpointWithProperties() throws Exception {
        var props = new HashMap<String, Object>();
        props.put("key", "value");

        var endpoint = jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .properties(props));

        assertThat(endpoint).isNotNull();
        assertThat(endpoint.getProperties()).containsEntry("key", "value");

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        var soapResponseNode = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponseNode);
    }

    @Test
    void publishEndpointWithPublishedUrlPrefix() throws WSDLException {
        jwsEnvironment.setPublishedEndpointUrlPrefix("http://external/prefix");

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("/path", service)
        );

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        var server = testutils.getServerForAddress("/path");
        var destination = (AbstractDestination) server.getDestination();
        var publishedEndpointUrl = destination.getEndpointInfo()
                .getProperty(WSDLGetUtils.PUBLISHED_ENDPOINT_URL, String.class);

        assertThat(publishedEndpointUrl).isEqualTo("http://external/prefix/path");
    }

    @Test
    void publishEndpointWithInvalidArguments() {
        assertAll(
                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> new EndpointBuilder("foo", null))
                        .withMessage("Service is null"),

                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> new EndpointBuilder(null, service))
                        .withMessage("Path is null"),

                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> new EndpointBuilder("   ", service))
                        .withMessage("Path is empty")
        );
    }

    @Test
    void getClientSimple() {
        var address = "http://address";

        // simple
        var clientProxy = jwsEnvironment.getClient(
                new ClientBuilder<>(DummyInterface.class, address)
        );
        assertThat(clientProxy).isInstanceOf(Proxy.class);

        var client = ClientProxy.getClient(clientProxy);
        assertAll(
                () -> assertThat(client.getEndpoint().getEndpointInfo().getAddress()).isEqualTo(address),
                () -> assertThat(client.getEndpoint().getService()).containsEntry("endpoint.class", DummyInterface.class),
                () -> assertThat(((BindingProvider) clientProxy).getBinding().getHandlerChain()).isEmpty()
        );

        var httpClientPolicy = ((HTTPConduit) client.getConduit()).getClient();
        assertAll(
                () -> assertThat(httpClientPolicy.getConnectionTimeout()).isEqualTo(500L),
                () -> assertThat(httpClientPolicy.getReceiveTimeout()).isEqualTo(2000L)
        );
    }

    @Test
    void getClientComplex() {
        var address = "http://address";

        // with timeouts, handlers, interceptors, properties and MTOM

        var handler = mock(Handler.class);
        var inInterceptor = new TestInterceptor(Phase.UNMARSHAL);
        var inInterceptor2 = new TestInterceptor(Phase.PRE_INVOKE);
        var outInterceptor = new TestInterceptor(Phase.MARSHAL);

        var clientProxy = jwsEnvironment.getClient(
                new ClientBuilder<>(DummyInterface.class, address)
                        .connectTimeout(123)
                        .receiveTimeout(456)
                        .handlers(handler)
                        .bindingId(SoapBindingFactory.SOAP_12_BINDING)
                        .cxfInInterceptors(inInterceptor, inInterceptor2)
                        .cxfOutInterceptors(outInterceptor)
                        .enableMtom());

        var client = ClientProxy.getClient(clientProxy);
        var bindingProvider = (BindingProvider) clientProxy;
        assertAll(
                () -> assertThat(bindingProvider.getBinding().getBindingID()).isEqualTo("http://www.w3.org/2003/05/soap/bindings/HTTP/"),
                () -> assertThat(bindingProvider.getBinding().getHandlerChain()).contains(handler),
                () -> {
                    var soapBinding = (SOAPBinding) bindingProvider.getBinding();
                    assertThat(soapBinding.isMTOMEnabled()).isTrue();
                },
                () -> assertThat(client.getEndpoint().getEndpointInfo().getAddress()).isEqualTo(address),
                () -> assertThat(client.getEndpoint().getService()).containsEntry("endpoint.class", DummyInterface.class)
        );

        var httpClientPolicy = ((HTTPConduit) client.getConduit()).getClient();
        assertAll(
                () -> assertThat(httpClientPolicy.getConnectionTimeout()).isEqualTo(123L),
                () -> assertThat(httpClientPolicy.getReceiveTimeout()).isEqualTo(456L)
        );
    }
}
