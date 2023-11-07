package com.roskart.dropwizard.jaxws.example.ws;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.util.Set;

public class WsdlFirstServiceHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger LOG = LoggerFactory.getLogger(WsdlFirstServiceHandler.class);

    @Override
    public Set<QName> getHeaders() {
        return Set.of();
    }

    @Override
    public void close(MessageContext messageContext) {
        LOG.info("Closing");
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        LOG.warn("Handling fault");
        return true;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {

        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (Boolean.TRUE.equals(outbound)) {
            LOG.info("WsdlFirstService server handler - outbound");
        } else {
            LOG.info("WsdlFirstService server handler - inbound");
        }

        return true;
    }

}
