package org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws;

import com.codahale.metrics.annotation.Metered;
import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import org.apache.cxf.annotations.UseAsyncMethod;
import org.apache.cxf.jaxws.ServerAsyncResponse;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.Echo;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.EchoResponse;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.NonBlockingEcho;
import ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.WsdlFirstService;

import java.util.concurrent.Future;

@WebService(endpointInterface = "ws.example.ws.xml.jakarta.dropwizard.kiwiproject.org.wsdlfirstservice.WsdlFirstService",
        targetNamespace = "http://org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws/WsdlFirstService",
        name = "WsdlFirstService",
        wsdlLocation = "META-INF/WsdlFirstService.wsdl")
@HandlerChain(file = "wsdlfirstservice-handlerchain.xml")
public class WsdlFirstServiceImpl implements WsdlFirstService {
    @Override
    @Metered
    public EchoResponse echo(Echo parameters) {
        var response = new EchoResponse();
        response.setValue(parameters.getValue());
        return response;
    }

    @Override
    @UseAsyncMethod
    @Metered
    public EchoResponse nonBlockingEcho(NonBlockingEcho parameters) {
        var response = new EchoResponse();
        response.setValue("Blocking: " + parameters.getValue());
        return response;
    }

    public Future<EchoResponse> nonBlockingEchoAsync(
            final NonBlockingEcho parameters,
            final AsyncHandler<EchoResponse> asyncHandler) {

        final var asyncResponse = new ServerAsyncResponse<EchoResponse>();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                var response = new EchoResponse();
                response.setValue("Non-blocking: " + parameters.getValue());
                asyncResponse.set(response);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                asyncResponse.exception(e);
            }
            asyncHandler.handleResponse(asyncResponse);
        }).start();

        return asyncResponse;
    }
}
