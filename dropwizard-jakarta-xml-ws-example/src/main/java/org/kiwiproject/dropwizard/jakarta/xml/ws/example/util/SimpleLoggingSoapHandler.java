package org.kiwiproject.dropwizard.jakarta.xml.ws.example.util;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.Logger;

import javax.xml.namespace.QName;
import java.util.Set;

public abstract class SimpleLoggingSoapHandler implements SOAPHandler<SOAPMessageContext> {

    private final String name;
    private final Logger logger;

    protected SimpleLoggingSoapHandler(String name, Logger logger) {
        this.name = name;
        this.logger = logger;
    }

    @Override
    public Set<QName> getHeaders() {
        return Set.of();
    }

    @Override
    public void close(MessageContext messageContext) {
        logger.info("Closing");
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        logger.warn("Handling fault");
        return true;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        var outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (Boolean.TRUE.equals(outbound)) {
            logger.info("{} - outbound", name);
        } else {
            logger.info("{} - inbound", name);
        }

        return true;
    }

}
