package org.kiwiproject.dropwizard.jakarta.xml.ws;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public class DummyService {

    @SuppressWarnings("EmptyMethod")
    @WebMethod
    public void foo() {
        // This is a fake service that does nothing.
    }
}
