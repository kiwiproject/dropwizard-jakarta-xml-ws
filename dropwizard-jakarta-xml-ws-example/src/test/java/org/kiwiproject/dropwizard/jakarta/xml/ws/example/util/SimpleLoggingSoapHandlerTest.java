package org.kiwiproject.dropwizard.jakarta.xml.ws.example.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimpleLoggingSoapHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleLoggingSoapHandlerTest.class);

    private SimpleLoggingSoapHandler handler;

    private static class TestHandler extends SimpleLoggingSoapHandler {
        TestHandler() {
            super("test", LOG);
        }
    }

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
    }

    @Test
    void shouldGetHeaders() {
        assertThat(handler.getHeaders()).isEmpty();
    }

    @Test
    void shouldClose() {
        assertThatCode(() -> handler.close(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleFault() {
        assertThat(handler.handleFault(null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldHandleMessage(boolean outbound) {
        var context = mock(SOAPMessageContext.class);
        when(context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).thenReturn(outbound);

        assertThat(handler.handleMessage(context)).isTrue();
    }
}
