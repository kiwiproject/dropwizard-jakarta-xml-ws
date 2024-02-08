package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
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
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.wsdl.WSDLException;
import java.lang.reflect.Proxy;
import java.util.HashMap;

class JakartaXmlWsEnvironmentTest {

    private static final String SOAP_REQUEST_FILE_NAME = "test-soap-request.xml";

    private JakartaXmlWsEnvironment jwsEnvironment;
    private Invoker mockInvoker;
    private TestUtilities testutils;
    private DummyService service;
    InstrumentedInvokerFactory mockInvokerBuilder;
    UnitOfWorkInvokerFactory mockUnitOfWorkInvokerBuilder;
    private int mockBasicAuthInterceptorInvoked;


    // DummyInterface is used by getClient test
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
            protected BasicAuthenticationInterceptor createBasicAuthenticationInterceptor() {
                return new BasicAuthenticationInterceptor() {
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
        Object result = jwsEnvironment.buildServlet();
        assertThat(result).isInstanceOf(CXFNonSpringServlet.class);
        assertThat(((CXFNonSpringServlet) result).getBus()).isInstanceOf(Bus.class);
    }

    @Test
    void publishEndpoint() throws Exception {

        Endpoint e = jwsEnvironment.publishEndpoint(new EndpointBuilder("local://path", service));
        assertThat(e).isNotNull();

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
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

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    void publishEndpointWithAuthentication() throws Exception {

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .authentication(mock(BasicAuthentication.class)));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);

        assertThat(mockBasicAuthInterceptorInvoked).isEqualTo(1);
    }

    @Test
    void publishEndpointWithHibernateInvoker() throws Exception {

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .sessionFactory(mock(SessionFactory.class)));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verify(mockUnitOfWorkInvokerBuilder).create(any(), any(Invoker.class), any(SessionFactory.class));

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    void publishEndpointWithCxfInterceptors() throws Exception {

        TestInterceptor inInterceptor = new TestInterceptor(Phase.UNMARSHAL);
        TestInterceptor inInterceptor2 = new TestInterceptor(Phase.PRE_INVOKE);
        TestInterceptor outInterceptor = new TestInterceptor(Phase.MARSHAL);

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .cxfInInterceptors(inInterceptor, inInterceptor2)
                        .cxfOutInterceptors(outInterceptor));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());
        assertThat(inInterceptor.getInvocationCount()).isEqualTo(1);
        assertThat(inInterceptor2.getInvocationCount()).isEqualTo(1);
        assertThat(outInterceptor.getInvocationCount()).isEqualTo(1);

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);

        soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker, times(2)).invoke(any(Exchange.class), any());
        assertThat(inInterceptor.getInvocationCount()).isEqualTo(2);
        assertThat(inInterceptor2.getInvocationCount()).isEqualTo(2);
        assertThat(outInterceptor.getInvocationCount()).isEqualTo(2);

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }


    @Test
    void publishEndpointWithMtom() throws Exception {

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .enableMtom());

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));

        byte[] response = testutils.invokeBytes("local://path", LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        MimeMultipart mimeMultipart = new MimeMultipart(new ByteArrayDataSource(response,
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

        Server server = testutils.getServerForAddress("local://path");
        AbstractDestination destination = (AbstractDestination) server.getDestination();
        String publishedEndpointUrl = destination.getEndpointInfo().getProperty(WSDLGetUtils.PUBLISHED_ENDPOINT_URL, String.class);

        assertThat(publishedEndpointUrl).isEqualTo("http://external.server/external/path");
    }

    @Test
    void publishEndpointWithProperties() throws Exception {

        HashMap<String, Object> props = new HashMap<>();
        props.put("key", "value");

        Endpoint e = jwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .properties(props));

        assertThat(e).isNotNull();
        assertThat(e.getProperties()).containsEntry("key", "value");

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, SOAP_REQUEST_FILE_NAME);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    void publishEndpointWithPublishedUrlPrefix() throws WSDLException {

        jwsEnvironment.setPublishedEndpointUrlPrefix("http://external/prefix");

        jwsEnvironment.publishEndpoint(
                new EndpointBuilder("/path", service)
        );

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        Server server = testutils.getServerForAddress("/path");
        AbstractDestination destination = (AbstractDestination) server.getDestination();
        String publishedEndpointUrl = destination.getEndpointInfo().getProperty(WSDLGetUtils.PUBLISHED_ENDPOINT_URL, String.class);

        assertThat(publishedEndpointUrl).isEqualTo("http://external/prefix/path");
    }

    @Test
    void publishEndpointWithInvalidArguments() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EndpointBuilder("foo", null))
                .withMessage("Service is null");

        //noinspection DataFlowIssue
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EndpointBuilder(null, service))
                .withMessage("Path is null");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EndpointBuilder("   ", service))
                .withMessage("Path is empty");
    }

    @Test
    void getClient() {

        var address = "http://address";
        var handler = mock(Handler.class);

        // simple
        DummyInterface clientProxy = jwsEnvironment.getClient(
                new ClientBuilder<>(DummyInterface.class, address)
        );
        assertThat(clientProxy).isInstanceOf(Proxy.class);

        Client c = ClientProxy.getClient(clientProxy);
        assertThat(c.getEndpoint().getEndpointInfo().getAddress()).isEqualTo(address);
        assertThat(c.getEndpoint().getService()).containsEntry("endpoint.class", DummyInterface.class);
        assertThat(((BindingProvider) clientProxy).getBinding().getHandlerChain()).isEmpty();

        HTTPClientPolicy httpclient = ((HTTPConduit) c.getConduit()).getClient();
        assertThat(httpclient.getConnectionTimeout()).isEqualTo(500L);
        assertThat(httpclient.getReceiveTimeout()).isEqualTo(2000L);

        // with timeouts, handlers, interceptors, properties and MTOM

        TestInterceptor inInterceptor = new TestInterceptor(Phase.UNMARSHAL);
        TestInterceptor inInterceptor2 = new TestInterceptor(Phase.PRE_INVOKE);
        TestInterceptor outInterceptor = new TestInterceptor(Phase.MARSHAL);

        clientProxy = jwsEnvironment.getClient(
                new ClientBuilder<>(DummyInterface.class, address)
                        .connectTimeout(123)
                        .receiveTimeout(456)
                        .handlers(handler)
                        .bindingId(SoapBindingFactory.SOAP_12_BINDING)
                        .cxfInInterceptors(inInterceptor, inInterceptor2)
                        .cxfOutInterceptors(outInterceptor)
                        .enableMtom());
        c = ClientProxy.getClient(clientProxy);
        assertThat(((BindingProvider) clientProxy).getBinding().getBindingID()).isEqualTo("http://www.w3.org/2003/05/soap/bindings/HTTP/");
        assertThat(c.getEndpoint().getEndpointInfo().getAddress()).isEqualTo(address);
        assertThat(c.getEndpoint().getService()).containsEntry("endpoint.class", DummyInterface.class);

        httpclient = ((HTTPConduit) c.getConduit()).getClient();
        assertThat(httpclient.getConnectionTimeout()).isEqualTo(123L);
        assertThat(httpclient.getReceiveTimeout()).isEqualTo(456L);

        assertThat(((BindingProvider) clientProxy).getBinding().getHandlerChain()).contains(handler);

        BindingProvider bp = (BindingProvider) clientProxy;
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        assertThat(binding.isMTOMEnabled()).isTrue();
    }
}
