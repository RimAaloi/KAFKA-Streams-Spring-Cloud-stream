package org.sid.kafkaspringcloudstream.controllers;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.sid.kafkaspringcloudstream.events.PageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class PageEventController {
    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private InteractiveQueryService interactiveQueryService;

    @GetMapping("/publish")
    public PageEvent publish(String name, String topic){
        PageEvent event = new PageEvent(
                name,
                Math.random()>0.5?"U1":"U2",
                new Date(),
                10+new Random().nextInt(10000));
        streamBridge.send(topic, event);
        System.out.println("✅ Event publié: " + name + " vers " + topic);
        return event;
    }

    @GetMapping(path = "/analytics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> {
                    Map<String, Long> result = new HashMap<>();
                    try {
                        ReadOnlyWindowStore<String, Long> windowStore =
                                interactiveQueryService.getQueryableStore("count-store",
                                        QueryableStoreTypes.windowStore());

                        Instant now = Instant.now();
                        Instant from = now.minusSeconds(30); // Fenêtre de 30 secondes

                        KeyValueIterator<Windowed<String>, Long> fetchAll = windowStore.fetchAll(from, now);

                        while (fetchAll.hasNext()) {
                            KeyValue<Windowed<String>, Long> next = fetchAll.next();
                            String pageName = next.key.key();
                            Long count = next.value;
                            result.put(pageName, count);
                        }
                        fetchAll.close();

                        // Log pour débogage
                        if (!result.isEmpty()) {
                            System.out.println("📊 Données analytics: " + result);
                        }

                    } catch (Exception e) {
                        System.out.println("⏳ Store pas encore prêt, réessayez...");
                    }
                    return result;
                });
    }

    // Endpoint pour publier rapidement des données de test
    @GetMapping("/test")
    public String testData() {
        for (int i = 0; i < 10; i++) {
            PageEvent event = new PageEvent(
                    Math.random()>0.5?"P1":"P2",
                    "U" + (i+1),
                    new Date(),
                    100 + new Random().nextInt(1000)
            );
            streamBridge.send("T3", event);
        }
        return "10 événements de test publiés vers T3";
    }
}