package org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws;

import com.codahale.metrics.annotation.Metered;
import jakarta.activation.DataHandler;
import jakarta.jws.WebService;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.soap.MTOM;
import org.apache.cxf.helpers.IOUtils;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.Hello;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.HelloResponse;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.MtomService;

import java.io.IOException;
import java.io.UncheckedIOException;

@MTOM // @MTOM annotation is not necessary if you invoke enableMtom on EndpointBuilder
@WebService(endpointInterface = "ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.mtomservice.MtomService",
        targetNamespace = "http://org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws/MtomService",
        name = "MtomService",
        wsdlLocation = "META-INF/MtomService.wsdl")
public class MtomServiceImpl implements MtomService {
    @Metered
    @Override
    public HelloResponse hello(Hello parameters) {
        try {
            byte[] bin = IOUtils.readBytesFromStream(parameters.getBinary().getInputStream());
            HelloResponse response = new HelloResponse();
            response.setTitle(parameters.getTitle());
            response.setBinary(new DataHandler(new ByteArrayDataSource(bin,
                    parameters.getBinary().getContentType())));
            return response;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
