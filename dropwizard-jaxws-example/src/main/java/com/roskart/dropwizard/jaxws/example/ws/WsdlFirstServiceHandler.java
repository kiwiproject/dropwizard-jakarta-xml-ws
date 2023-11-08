package com.roskart.dropwizard.jaxws.example.ws;

import com.roskart.dropwizard.jaxws.example.util.SimpleLoggingSoapHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jakarta XML Web Services server handler when WsdlFirstService is invoked.
 * <p>
 * See the {@code wsdlfirstservice-handlerchain.xml} configuration file, which
 * is where this class is defined to be a handler.
 */
public class WsdlFirstServiceHandler extends SimpleLoggingSoapHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WsdlFirstServiceHandler.class);

    public WsdlFirstServiceHandler() {
        super("WsdlFirstService server handler", LOG);
    }
}
