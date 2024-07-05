package org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

import java.security.Principal;
import java.time.LocalDateTime;

@WebService(name = "JavaFirstService",
        serviceName = "JavaFirstService",
        portName = "JavaFirstService",
        targetNamespace = "http://org.kiwiproject.dropwizard.example/JavaFirstService",
        endpointInterface = "org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws.JavaFirstService")
public class JavaFirstServiceImpl implements JavaFirstService {

    @Resource
    WebServiceContext wsContext;

    @Override
    @Metered
    @ExceptionMetered
    public String echo(String in) throws JavaFirstServiceException {
        if (in == null || in.isBlank()) {
            throw new JavaFirstServiceException("Invalid parameter");
        }

        var userPrincipal = (Principal) wsContext.getMessageContext().get("dropwizard.jakarta.xml.ws.principal");
        return in + "; principal: " + userPrincipal.getName() + " at " + LocalDateTime.now();
    }
}
