package org.acme.people.service;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.opentracing.Traced;


@ApplicationScoped
public class GreetingService {

    private String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
    // microprofile metrics
    @Counted(name = "greetings", description = "How many greetings we've given.")
    @Traced // expicit method tracing
    public String greeting(String name) {
        return "hello " + name + " from " + hostname;
    }
}