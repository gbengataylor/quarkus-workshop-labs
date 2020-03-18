package org.acme.people.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.acme.people.model.DataTable;
import org.acme.people.model.EyeColor;
import org.acme.people.model.Person;
import org.acme.people.model.StarWarsPerson;
import org.acme.people.service.StarWarsService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import org.acme.people.utils.CuteNameGenerator;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;

import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.StartupEvent;
import io.vertx.axle.core.eventbus.EventBus;
import io.vertx.axle.core.eventbus.Message;
import io.quarkus.hibernate.orm.panache.PanacheQuery;

//openapi

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Path("/person")
@ApplicationScoped
public class PersonResource {

   @Inject
   EventBus bus;


     // inject restclient for external call
    @Inject
    @RestClient
    StarWarsService swService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Traced // expicit method tracing
    public List<Person> getAll() {
        return Person.listAll();
    }

    // TODO: add basic queries
    @GET
    @Path("/eyes/{color}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> findByColor(@PathParam(value = "color") EyeColor color) {
        return Person.findByColor(color);
    }

    @GET
    @Path("/birth/before/{year}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Finds people born before a specific year",
           description = "Search the people database and return a list of people born before the specified year")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "The list of people born before the specified year",
            content = @Content(
                schema = @Schema(implementation = Person.class)
            )),
        @APIResponse(responseCode = "500", description = "Something bad happened")
   })
    public List<Person> getBeforeYear(@Parameter(description = "Cutoff year for searching for people", required = true, name="year")
                                      @PathParam(value = "year") int year) {
        return Person.getBeforeYear(year);
    }
    // TODO: add datatable query
    @GET
    @Path("/datatable")
    @Produces(MediaType.APPLICATION_JSON)
    @Traced // expicit method tracing
    @Counted(name = "datatable", description = "How many times the datatable was called")
    @Timed(name = "datatableTime", description = "A measure how long it takes to draw the datatable", unit = MetricUnits.MILLISECONDS)
    public DataTable datatable(
        @QueryParam(value = "draw") int draw,
        @QueryParam(value = "start") int start,
        @QueryParam(value = "length") int length,
        @QueryParam(value = "search[value]") String searchVal

        ) {
            // TODO: Begin result
            DataTable result = new DataTable();
            result.setDraw(draw);
            // TODO: Filter based on search
            PanacheQuery<Person> filteredPeople;

            if (searchVal != null && !searchVal.isEmpty()) {
                filteredPeople = Person.<Person>find("name like :search",
                    Parameters.with("search", "%" + searchVal + "%"));
            } else {
                filteredPeople = Person.findAll();
            }
            // TODO: Page and return
            int page_number = start / length;
            filteredPeople.page(page_number, length);

            result.setRecordsFiltered(filteredPeople.count());
            result.setData(filteredPeople.list());
            result.setRecordsTotal(Person.count());

            return result;

    }
    // TODO: Add lifecycle hook
    // this should really be in the service
    // maybe set this only in dev mode
    @Transactional
    void onStart(@Observes StartupEvent ev) {
        for (int i = 0; i < 1000; i++) {
            String name = CuteNameGenerator.generate();
            LocalDate birth = LocalDate.now().plusWeeks(Math.round(Math.floor(Math.random() * 20 * 52 * -1)));
            EyeColor color = EyeColor.values()[(int)(Math.floor(Math.random() * EyeColor.values().length))];
            Person p = new Person();
            p.birth = birth;
            p.eyes = color;
            p.name = name;
            Person.persist(p);
        }
    }

    @POST
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<String> addPerson(@PathParam("name") String name) {
        return bus.<String>send("add-person", name)
          .thenApply(Message::body);
    }

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Person byName(@PathParam("name") String name) {
        return Person.find("name", name).firstResult();
    }

    // external call
    @GET
    @Path("/swpeople")
    @Produces(MediaType.APPLICATION_JSON)
    @Traced // expicit method tracing, without it just the service will be traced
    @Counted(name = "starWarsCharacters", description = "How many times we have retrieved Star Wars Characters")
    @Timed(name = "starWarsCharactersTime", description = "A measure how long it takes to retrieve Star Wars Characters", unit = MetricUnits.MILLISECONDS)
    public List<StarWarsPerson> getCharacters() {
        return IntStream.range(1, 6) // generate a stream of 5 integers that we will use as IDS to pass to service
            .mapToObj(swService::getPerson) // for each of all the ints call the StarWarsService::getPerson method
            .collect(Collectors.toList()); // collect results into a list and return it
    }
}