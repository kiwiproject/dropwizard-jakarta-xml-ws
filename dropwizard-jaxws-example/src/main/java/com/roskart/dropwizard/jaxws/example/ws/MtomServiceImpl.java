package com.roskart.dropwizard.jaxws.example.ws;

import com.codahale.metrics.annotation.Metered;
import jakarta.activation.DataHandler;
import jakarta.jws.WebService;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.soap.MTOM;
import org.apache.cxf.helpers.IOUtils;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.Hello;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.HelloResponse;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.MtomService;

import java.io.IOException;
import java.io.UncheckedIOException;

@MTOM // @MTOM annotation is not necessary if you invoke enableMtom on EndopointBuilder
@WebService(endpointInterface = "ws.example.jaxws.dropwizard.roskart.com.mtomservice.MtomService",
        targetNamespace = "http://com.roskart.dropwizard.jaxws.example.ws/MtomService",
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
