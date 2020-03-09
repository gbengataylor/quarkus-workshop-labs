package org.acme.people.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.security.identity.SecurityIdentity;

@Path("/secured")
//Note that we do not use any @RolesAllowed or any other instrumentation on the endpoint to specify access policy. It looks like an ordinary endpoint. Keycloak (the server) is the one enforcing access here, not Quarkus directly.
public class KeycloakResource {

    @Inject
    SecurityIdentity identity;


    @GET
    //	The SecurityIdentity is a generic object produced by the Keycloak extension that you can use to obtain information about the security principals and attributes embedded in the request.
    @Path("/confidential")
    @Produces(MediaType.TEXT_PLAIN)
    public String confidential() {
        return ("confidential access for: " + identity.getPrincipal().getName() +
          " with attributes:" + identity.getAttributes());
    }
}