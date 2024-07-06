package org.kiwiproject.dropwizard.jakarta.xml.ws;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

import java.security.Principal;

public class BasicAuthentication<P extends Principal> {

    private final Authenticator<BasicCredentials, P> authenticator;
    private final String realm;

    public BasicAuthentication(Authenticator<BasicCredentials, P> authenticator, String realm) {
        this.authenticator = authenticator;
        this.realm = realm;
    }

    public Authenticator<BasicCredentials, P> getAuthenticator() {
        return this.authenticator;
    }

    public String getRealm() {
        return realm;
    }

}
