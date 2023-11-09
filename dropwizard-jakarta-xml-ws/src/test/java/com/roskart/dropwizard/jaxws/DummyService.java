package com.roskart.dropwizard.jaxws;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public class DummyService {

    @SuppressWarnings("EmptyMethod")
    @WebMethod
    public void foo() {
    }
}
