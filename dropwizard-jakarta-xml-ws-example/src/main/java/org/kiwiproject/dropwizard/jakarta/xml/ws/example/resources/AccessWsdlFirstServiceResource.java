package org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources;

import com.codahale.metrics.annotation.Timed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.ObjectFactory;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.WsdlFirstService;

import java.time.LocalDateTime;

/**
 * A Dropwizard resource that invokes WsdlFirstService SOAP web service.
 *
 * @see WsdlFirstClientHandler
 */
@Path("/wsdlfirstclient")
@Produces(MediaType.APPLICATION_JSON)
public class AccessWsdlFirstServiceResource {

    final WsdlFirstService wsdlFirstServiceClient;

    public AccessWsdlFirstServiceResource(WsdlFirstService wsdlFirstServiceClient) {
        this.wsdlFirstServiceClient = wsdlFirstServiceClient;
    }

    @GET
    @Timed
    public String getFoo() {
        var objectFactory = new ObjectFactory();
        var echo = objectFactory.createEcho();
        echo.setValue("echo value");

        var echoResponse = wsdlFirstServiceClient.echo(echo);

        return "Echo response: " + echoResponse.getValue() + " at " + LocalDateTime.now();
    }
}
