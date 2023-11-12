package org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebFault;

@WebService(targetNamespace = "http://org.kiwiproject.dropwizard.example/JavaFirstService")
public interface JavaFirstService {

    @WebFault(name = "JavaFirstServiceException")
    class JavaFirstServiceException extends Exception {
        public JavaFirstServiceException(String s) {
            super(s);
        }
    }

    @WebMethod(operationName = "Echo")
    @WebResult(name = "EchoResponse")
    String echo(@WebParam(name = "EchoParameter") String in) throws JavaFirstServiceException;
}
