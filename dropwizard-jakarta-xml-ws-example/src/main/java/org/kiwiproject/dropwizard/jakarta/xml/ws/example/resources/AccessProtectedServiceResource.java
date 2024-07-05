package org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.ws.BindingProvider;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws.JavaFirstService;

/**
 * A Dropwizard resource that invokes JavaFirstService SOAP web service using basic authentication.
 */
@Path("/javafirstclient")
@Produces(MediaType.APPLICATION_JSON)

public class AccessProtectedServiceResource {

    final JavaFirstService javaFirstService;

    public AccessProtectedServiceResource(JavaFirstService javaFirstService) {
        this.javaFirstService = javaFirstService;
    }

    @GET
    public String getEcho() {
        try {
            var bindingProvider = (BindingProvider) javaFirstService;
            bindingProvider.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "johndoe");
            bindingProvider.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "secret");

            return this.javaFirstService.echo("Hello from the protected service!");
        } catch (JavaFirstService.JavaFirstServiceException jfse) {
            throw new WebApplicationException(jfse);
        }
    }

}
