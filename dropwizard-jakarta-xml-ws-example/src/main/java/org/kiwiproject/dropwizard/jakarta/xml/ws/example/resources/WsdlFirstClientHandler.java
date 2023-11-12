package org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources;

import org.kiwiproject.dropwizard.jakarta.xml.ws.example.util.SimpleLoggingSoapHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jakarta XML Web Services client handler used when WsdlFirstService is invoked.
 *
 * @see AccessWsdlFirstServiceResource
 */
public class WsdlFirstClientHandler extends SimpleLoggingSoapHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WsdlFirstClientHandler.class);

    public WsdlFirstClientHandler() {
        super("WsdlFirstService client handler", LOG);
    }
}
