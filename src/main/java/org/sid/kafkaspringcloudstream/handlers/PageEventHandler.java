package org.sid.kafkaspringcloudstream.handlers;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.sid.kafkaspringcloudstream.events.PageEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class PageEventHandler {

    @Bean
    public Consumer<PageEvent> pageEventConsumer(){
        return (input) -> {
            System.out.println("📥 Consommé: " + input.name() + " - " + input.user());
        };
    }

    @Bean
    public Supplier<PageEvent> pageEventSupplier(){
        return () -> {
            PageEvent event = new PageEvent(
                    Math.random()>0.5?"P1":"P2",
                    Math.random()>0.5?"U1":"U2",
                    new Date(),
                    10+new Random().nextInt(10000)
            );
            System.out.println("📤 Supplier: " + event.name());
            return event;
        };
    }

    @Bean
    public Function<KStream<String, PageEvent>, KStream<String, Long>> kStreamFunction(){
        return (input) -> input
                .peek((k, v) -> System.out.println("➡️ Traitement KStream: " + v.name()))
                .map((k, v) -> new KeyValue<>(v.name(), 1L)) // Compter chaque vue comme 1
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(30))) // Fenêtre de 30 secondes
                .count(Materialized.as("count-store"))
                .toStream()
                .map((k, v) -> {
                    System.out.println("📈 Comptage: " + k.key() + " = " + v);
                    return new KeyValue<>(k.key(), v);
                });
    }
}