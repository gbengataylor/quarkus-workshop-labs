package org.acme.people.rest;

import java.security.Principal;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.JsonString;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/secured")
@RequestScoped //	Adds a @RequestScoped as Quarkus uses a default scoping of ApplicationScoped and this will produce undesirable behavior since JWT claims are naturally request scoped.
public class JWTResource {

    @Inject
    JsonWebToken jwt; //JsonWebToken provides access to the claims associated with the incoming authenticated JWT token.

    @Inject
    @Claim(standard = Claims.iss)
    Optional<JsonString> issuer; // When using JWT Authentication, claims encoded in tokens can be @Inject ed into your class for convenient access.

    @GET
    @Path("/me")
    @RolesAllowed("user")
    @Produces(MediaType.TEXT_PLAIN)
    public String me(@Context SecurityContext ctx) { // The /me and /me/admin endpoints demonstrate how to access the security context for Quarkus apps secured with JWT. Here we are using a @RolesAllowed annotation to make sure that only users granted a specific role can access the endpoint.
        Principal caller = ctx.getUserPrincipal();
        String name = caller == null ? "anonymous" : caller.getName();
        boolean hasJWT = jwt != null;
        return String.format("hello %s, isSecure: %s, authScheme: %s, hasJWT: %s\n", name, ctx.isSecure(), ctx.getAuthenticationScheme(), hasJWT);
    }

    @GET
    @Path("/me/admin")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_PLAIN)
    public String meJwt(@Context SecurityContext ctx) {
        Principal caller = ctx.getUserPrincipal();
        String name = caller == null ? "anonymous" : caller.getName();
        boolean hasJWT = jwt != null;

        final StringBuilder helloReply = new StringBuilder(String.format("hello %s, isSecure: %s, authScheme: %s, hasJWT: %s\n", name, ctx.isSecure(), ctx.getAuthenticationScheme(), hasJWT));
        if (hasJWT && (jwt.getClaimNames() != null)) {
            //	Use of injected JWT Claim to print the all the claims
            helloReply.append("Injected issuer: [" + issuer.get() + "]\n");
            jwt.getClaimNames().forEach(n -> {
                helloReply.append("\nClaim Name: [" + n + "] Claim Value: [" + jwt.getClaim(n) + "]");
            });
        }
        return helloReply.toString();
    }
}