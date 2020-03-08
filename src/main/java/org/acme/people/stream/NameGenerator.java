package org.acme.people.stream;

import io.reactivex.Flowable;
import javax.enterprise.context.ApplicationScoped;
import org.acme.people.utils.CuteNameGenerator;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import java.util.concurrent.TimeUnit;

// NAmeGenerator->kafka topic->NameConverter
 //->in memroy bus
 // ->NAmeResource (has channel and api resource to return stream)
@ApplicationScoped
public class NameGenerator {

    @Outgoing("generated-name")
    public Flowable<String> generate() {
        return Flowable.interval(5, TimeUnit.SECONDS)
                .map(tick -> CuteNameGenerator.generate());
    }

}