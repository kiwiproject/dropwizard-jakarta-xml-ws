package com.roskart.dropwizard.jaxws.example.resources;

import com.roskart.dropwizard.jaxws.example.util.SimpleLoggingSoapHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-WS client handler used when WsdlFirstService is invoked.
 *
 * @see AccessWsdlFirstServiceResource
 */
public class WsdlFirstClientHandler extends SimpleLoggingSoapHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WsdlFirstClientHandler.class);

    public WsdlFirstClientHandler() {
        super("WsdlFirstService client handler", LOG);
    }
}
