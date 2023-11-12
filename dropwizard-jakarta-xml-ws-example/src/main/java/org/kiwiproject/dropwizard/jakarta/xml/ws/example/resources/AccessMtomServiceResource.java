package org.kiwiproject.dropwizard.jakarta.xml.ws.example.resources;

import com.codahale.metrics.annotation.Timed;
import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.helpers.IOUtils;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.Hello;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.HelloResponse;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.MtomService;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.ObjectFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;

@Path("/mtomclient")
@Produces(MediaType.APPLICATION_JSON)
public class AccessMtomServiceResource {

    final MtomService mtomServiceClient;

    public AccessMtomServiceResource(MtomService mtomServiceClient) {
        this.mtomServiceClient = mtomServiceClient;
    }

    @GET
    @Timed
    public String getFoo() {

        ObjectFactory of = new ObjectFactory();
        Hello h = of.createHello();
        h.setTitle("Hello");
        h.setBinary(new DataHandler(new ByteArrayDataSource("test".getBytes(), "text/plain")));

        HelloResponse hr = mtomServiceClient.hello(h);

        try {
            return "Hello response: " + hr.getTitle() + ", " +
                    IOUtils.readStringFromStream(hr.getBinary().getInputStream()) +
                    " at " + LocalDateTime.now();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
