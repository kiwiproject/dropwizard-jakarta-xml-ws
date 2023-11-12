package org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WsdlFirstClientHandlerTest {

    private SOAPMessageContext messageContext;

    @BeforeEach
    void setUp() {
        messageContext = mock(SOAPMessageContext.class);
    }

    @Test
    void shouldCreateWithName() {
        var handler = new WsdlFirstClientHandler();
        assertThat(handler.handleMessage(messageContext)).isTrue();
    }
}
