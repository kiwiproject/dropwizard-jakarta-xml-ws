package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static org.kiwiproject.dropwizard.jakarta.xml.ws.BasicAuthenticationInterceptor.PRINCIPAL_KEY;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kiwiproject.dropwizard.jakarta.xml.ws.auth.BasicAuthenticator;
import org.kiwiproject.dropwizard.jakarta.xml.ws.auth.User;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.Principal;

class BasicAuthenticationInterceptorTest {

    // Suppress warning about "hard-coded" password
    @SuppressWarnings("java:S2068")
    private static final String CORRECT_PASSWORD = "secret";
    private static final String USERNAME = "username";

    @Mock
    private InterceptorChain interceptorChainMock;
    @Mock
    private Destination destinationMock;
    @Mock
    private Conduit conduitMock;
    @Mock
    private Message inMessageMock;
    @Mock
    private Message outMessageMock;
    @Mock
    private OutputStream outputStreamMock;

    private BasicAuthentication<User> basicAuthentication;

    @BeforeEach
    void setup() throws IOException {
        basicAuthentication = new BasicAuthentication<>(new BasicAuthenticator(), "TOP_SECRET");

        MockitoAnnotations.openMocks(this);
        when(destinationMock.getBackChannel(any())).thenReturn(conduitMock);
        when(outMessageMock.getContent(OutputStream.class)).thenReturn(outputStreamMock);
    }

    @Test
    void shouldAuthenticateValidUser() {
        var interceptor = new BasicAuthenticationInterceptor();
        interceptor.setAuthenticator(basicAuthentication);
        var message = createMessageWithUsernameAndPassword(USERNAME, CORRECT_PASSWORD);

        interceptor.handleMessage(message);

        verify(inMessageMock).put(eq(PRINCIPAL_KEY), any(Principal.class));
    }

    @Test
    void shouldReturnUnauthorizedCodeForInvalidCredentials() {
        var interceptor = new BasicAuthenticationInterceptor();
        interceptor.setAuthenticator(basicAuthentication);
        var message = createMessageWithUsernameAndPassword(USERNAME, "foo");

        interceptor.handleMessage(message);

        verify(outMessageMock).put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    void shouldNotCrashOnNullPassword() {
        var interceptor = new BasicAuthenticationInterceptor();
        interceptor.setAuthenticator(basicAuthentication);
        var message = createMessageWithUsernameAndPassword(USERNAME, null);

        interceptor.handleMessage(message);

        verify(outMessageMock).put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    void shouldNotCrashOnNullUser() {
        var interceptor = new BasicAuthenticationInterceptor();
        interceptor.setAuthenticator(basicAuthentication);
        var message = createMessageWithUsernameAndPassword(null, CORRECT_PASSWORD);

        interceptor.handleMessage(message);

        verify(outMessageMock).put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    private Message createMessageWithUsernameAndPassword(String username, String password) {
        var message = createEmptyMessage();

        var policy = new AuthorizationPolicy();
        policy.setUserName(username);
        policy.setPassword(password);
        message.put(AuthorizationPolicy.class, policy);
        return message;
    }

    private Message createEmptyMessage() {
        var exchange = new ExchangeImpl();
        exchange.setInMessage(inMessageMock);
        exchange.setOutMessage(outMessageMock);
        exchange.setDestination(destinationMock);

        var message = new MessageImpl();
        message.setExchange(exchange);
        message.setInterceptorChain(interceptorChainMock);
        return message;
    }
}
