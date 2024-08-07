package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentials;
import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.TokenType;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A CXF interceptor that manages HTTP Basic Authentication. Implementation is based on the combination of
 * CXF JAASLoginInterceptor code and the following GitHub Gist:
 * <a href="https://gist.github.com/palesz/3438143">Basic HTTP Authentication Interceptor for Apache CXF</a>.
 * <p>
 * Dropwizard authenticator is used for credential authentication. Authenticated principal is stored in message
 * exchange and is available in the service implementation through a Jakarta XML Web Services
 * {@link jakarta.xml.ws.WebServiceContext WebServiceContext}.
 */
public class BasicAuthenticationInterceptor<P extends Principal> extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LoggerFactory.getLogger(BasicAuthenticationInterceptor.class);

    public static final String PRINCIPAL_KEY = "dropwizard.jakarta.xml.ws.principal";

    private BasicAuthentication<P> authentication;

    public BasicAuthenticationInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void setAuthenticator(BasicAuthentication<P> authentication) {
        this.authentication = authentication;
    }

    @Override
    public void handleMessage(final Message message) throws Fault {

        final var exchange = message.getExchange();

        BasicCredentials credentials = null;

        try {
            var policy = message.get(AuthorizationPolicy.class);
            if (nonNull(policy) && nonNull(policy.getUserName()) && nonNull(policy.getPassword())) {
                credentials = new BasicCredentials(policy.getUserName(), policy.getPassword());
            } else {
                // try the WS-Security UsernameToken
                var token = message.get(SecurityToken.class);
                if (nonNull(token) && token.getTokenType() == TokenType.UsernameToken) {
                    var usernameToken = (UsernameToken) token;
                    credentials = new BasicCredentials(usernameToken.getName(), usernameToken.getPassword());
                }
            }

            if (isNull(credentials)) {
                sendErrorResponse(message, HttpURLConnection.HTTP_UNAUTHORIZED);
                return;
            }

            Optional<P> principal = authentication.getAuthenticator().authenticate(
                    new BasicCredentials(credentials.getUsername(), credentials.getPassword()));

            if (principal.isEmpty()) {
                sendErrorResponse(message, HttpURLConnection.HTTP_UNAUTHORIZED);
                return;
            }

            // principal will be available through Jakarta XML Web Services WebServiceContext
            exchange.getInMessage().put(PRINCIPAL_KEY, principal.get());
        } catch (AuthenticationException ae) {
            sendErrorResponse(message, HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    private void sendErrorResponse(Message message, int responseCode) {
        var outMessage = getOutMessage(message);
        outMessage.put(Message.RESPONSE_CODE, responseCode);
        // Set the response headers
        @SuppressWarnings("unchecked")
        var responseHeaders = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (nonNull(responseHeaders)) {
            responseHeaders.put("WWW-Authenticate", singletonList("Basic realm=" + authentication.getRealm()));
            responseHeaders.put("Content-length", singletonList("0"));
        }
        message.getInterceptorChain().abort();
        try {
            getConduit(message).prepare(outMessage);
            close(outMessage);
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private Message getOutMessage(Message inMessage) {
        var exchange = inMessage.getExchange();
        var outMessage = exchange.getOutMessage();
        if (isNull(outMessage)) {
            var endpoint = exchange.get(Endpoint.class);
            outMessage = endpoint.getBinding().createMessage();
            exchange.setOutMessage(outMessage);
        }
        outMessage.putAll(inMessage);
        return outMessage;
    }

    private Conduit getConduit(Message inMessage) throws IOException {
        var exchange = inMessage.getExchange();
        var conduit = exchange.getDestination().getBackChannel(inMessage);
        exchange.setConduit(conduit);
        return conduit;
    }

    private void close(Message outMessage) throws IOException {
        var outputStream = outMessage.getContent(OutputStream.class);
        outputStream.flush();
        outputStream.close();
    }
}
