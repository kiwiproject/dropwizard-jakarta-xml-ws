package com.roskart.dropwizard.jaxws;

import ch.qos.logback.classic.Level;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxy;
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

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import javax.wsdl.WSDLException;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.soap.SOAPBinding;

import java.lang.reflect.Proxy;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JAXWSEnvironmentTest {

    private JAXWSEnvironment jaxwsEnvironment;
    private Invoker mockInvoker = mock(Invoker.class);
    private TestUtilities testutils = new TestUtilities(JAXWSEnvironmentTest.class);
    private DummyService service = new DummyService();
    InstrumentedInvokerFactory mockInvokerBuilder = mock(InstrumentedInvokerFactory.class);
    UnitOfWorkInvokerFactory mockUnitOfWorkInvokerBuilder = mock(UnitOfWorkInvokerFactory.class);
    private int mockBasicAuthInterceptorInvoked;

    private String soapRequest = "test-soap-request.xml";

    // DummyInterface is used by getClient test
    @WebService
    public interface DummyInterface {
        @WebMethod
        @SuppressWarnings("unused")
        void foo();
    }

    // TestInterceptor is used for testing CXF interceptors
    class TestInterceptor extends AbstractPhaseInterceptor<Message> {
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

        ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("org.apache.cxf")).setLevel(Level.INFO);

        jaxwsEnvironment = new JAXWSEnvironment("soap") {
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
        jaxwsEnvironment.setInstrumentedInvokerBuilder(mockInvokerBuilder);

        when(mockUnitOfWorkInvokerBuilder
                .create(any(), any(Invoker.class), any(SessionFactory.class)))
                .thenReturn(mockInvoker);
        jaxwsEnvironment.setUnitOfWorkInvokerBuilder(mockUnitOfWorkInvokerBuilder);

        mockBasicAuthInterceptorInvoked = 0;

        testutils.setBus(jaxwsEnvironment.bus);
        testutils.addNamespace("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        testutils.addNamespace("a", "http://jaxws.dropwizard.roskart.com/");
    }

    @AfterEach
    void teardown() {
        jaxwsEnvironment.bus.shutdown(false);
    }

    @Test
    void buildServlet() {
        Object result = jaxwsEnvironment.buildServlet();
        assertThat(result).isInstanceOf(CXFNonSpringServlet.class);
        assertThat(((CXFNonSpringServlet) result).getBus()).isInstanceOf(Bus.class);
    }

    @Test
    void publishEndpoint() throws Exception {

        Endpoint e = jaxwsEnvironment.publishEndpoint(new EndpointBuilder("local://path", service));
        assertThat(e).isNotNull();

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    void publishEndpointWithAnotherEnvironment() throws Exception {

        // creating new runtime environment simulates using separate bundles
        JAXWSEnvironment anotherJaxwsEnvironment = new JAXWSEnvironment("soap2");
        anotherJaxwsEnvironment.setInstrumentedInvokerBuilder(mockInvokerBuilder);
        anotherJaxwsEnvironment.setUnitOfWorkInvokerBuilder(mockUnitOfWorkInvokerBuilder);

        testutils.setBus(anotherJaxwsEnvironment.bus);

        anotherJaxwsEnvironment.publishEndpoint(new EndpointBuilder("local://path", service));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    void publishEndpointWithAuthentication() throws Exception {

        jaxwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                    .authentication(mock(BasicAuthentication.class)));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);

        assertThat(mockBasicAuthInterceptorInvoked).isEqualTo(1);
    }

    @Test
    void publishEndpointWithHibernateInvoker() throws Exception {

        jaxwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                    .sessionFactory(mock(SessionFactory.class)));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verify(mockUnitOfWorkInvokerBuilder).create(any(), any(Invoker.class), any(SessionFactory.class));

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    void publishEndpointWithCxfInterceptors() throws Exception {

        TestInterceptor inInterceptor = new TestInterceptor(Phase.UNMARSHAL);
        TestInterceptor inInterceptor2 = new TestInterceptor(Phase.PRE_INVOKE);
        TestInterceptor outInterceptor = new TestInterceptor(Phase.MARSHAL);

        jaxwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .cxfInInterceptors(inInterceptor, inInterceptor2)
                        .cxfOutInterceptors(outInterceptor));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest);

        verify(mockInvoker).invoke(any(Exchange.class), any());
        assertThat(inInterceptor.getInvocationCount()).isEqualTo(1);
        assertThat(inInterceptor2.getInvocationCount()).isEqualTo(1);
        assertThat(outInterceptor.getInvocationCount()).isEqualTo(1);

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);

        soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest);

        verify(mockInvoker, times(2)).invoke(any(Exchange.class), any());
        assertThat(inInterceptor.getInvocationCount()).isEqualTo(2);
        assertThat(inInterceptor2.getInvocationCount()).isEqualTo(2);
        assertThat(outInterceptor.getInvocationCount()).isEqualTo(2);

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }


    @Test
    void publishEndpointWithMtom() throws Exception {

        jaxwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .enableMtom());

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));

        byte[] response = testutils.invokeBytes("local://path", LocalTransportFactory.TRANSPORT_ID, soapRequest);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        MimeMultipart mimeMultipart = new MimeMultipart(new ByteArrayDataSource(response,
                "application/xop+xml; charset=UTF-8; type=\"text/xml\""));
        assertThat(mimeMultipart.getCount()).isEqualTo(1);
        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse",
                StaxUtils.read(mimeMultipart.getBodyPart(0).getInputStream()));
    }

    @Test
    void publishEndpointWithCustomPublishedUrl() throws Exception {

        jaxwsEnvironment.publishEndpoint(
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

        Endpoint e = jaxwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                    .properties(props));

        assertThat(e).isNotNull();
        assertThat(e.getProperties().get("key")).isEqualTo("value");

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyNoInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest);

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    void publishEndpointWithPublishedUrlPrefix() throws WSDLException {

        jaxwsEnvironment.setPublishedEndpointUrlPrefix("http://external/prefix");

        jaxwsEnvironment.publishEndpoint(
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
    void publishEndpointWithInvalidArguments() throws Exception {

        try {
            jaxwsEnvironment.publishEndpoint(new EndpointBuilder("foo", null));
        }
        catch (IllegalArgumentException e) {
        }

        try {
            jaxwsEnvironment.publishEndpoint(new EndpointBuilder(null, service));
        }
        catch (IllegalArgumentException e) {
        }

        try {
            jaxwsEnvironment.publishEndpoint(new EndpointBuilder("   ", service));
        }
        catch (IllegalArgumentException e) {
        }
    }

    @Test
    void getClient() {

        String address = "http://address";
        Handler handler = mock(Handler.class);

        // simple
        DummyInterface clientProxy = jaxwsEnvironment.getClient(
                new ClientBuilder<>(DummyInterface.class, address)
        );
        assertThat(clientProxy).isInstanceOf(Proxy.class);

        Client c = ClientProxy.getClient(clientProxy);
        assertThat(c.getEndpoint().getEndpointInfo().getAddress()).isEqualTo(address);
        assertThat(c.getEndpoint().getService().get("endpoint.class").equals(DummyInterface.class)).isEqualTo(true);
        assertThat(((BindingProvider)clientProxy).getBinding() .getHandlerChain().size()).isEqualTo(0);

        HTTPClientPolicy httpclient = ((HTTPConduit)c.getConduit()).getClient();
        assertThat(httpclient.getConnectionTimeout()).isEqualTo(500L);
        assertThat(httpclient.getReceiveTimeout()).isEqualTo(2000L);

        // with timeouts, handlers, interceptors, properties and MTOM

        TestInterceptor inInterceptor = new TestInterceptor(Phase.UNMARSHAL);
        TestInterceptor inInterceptor2 = new TestInterceptor(Phase.PRE_INVOKE);
        TestInterceptor outInterceptor = new TestInterceptor(Phase.MARSHAL);

        clientProxy = jaxwsEnvironment.getClient(
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
        assertThat(c.getEndpoint().getService().get("endpoint.class").equals(DummyInterface.class)).isEqualTo(true);

        httpclient = ((HTTPConduit)c.getConduit()).getClient();
        assertThat(httpclient.getConnectionTimeout()).isEqualTo(123L);
        assertThat(httpclient.getReceiveTimeout()).isEqualTo(456L);

        assertThat(((BindingProvider)clientProxy).getBinding().getHandlerChain()).contains(handler);

        BindingProvider bp = (BindingProvider)clientProxy;
        SOAPBinding binding = (SOAPBinding)bp.getBinding();
        assertThat(binding.isMTOMEnabled()).isEqualTo(true);
    }
}
