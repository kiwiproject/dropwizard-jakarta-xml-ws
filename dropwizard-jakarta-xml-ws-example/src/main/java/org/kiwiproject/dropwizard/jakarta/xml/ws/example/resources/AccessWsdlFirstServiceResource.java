package org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources;

import com.codahale.metrics.annotation.Timed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.Echo;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.EchoResponse;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.ObjectFactory;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.WsdlFirstService;

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

        ObjectFactory of = new ObjectFactory();
        Echo e = of.createEcho();
        e.setValue("echo value");

        EchoResponse er = wsdlFirstServiceClient.echo(e);

        return "Echo response: " + er.getValue();
    }
}
