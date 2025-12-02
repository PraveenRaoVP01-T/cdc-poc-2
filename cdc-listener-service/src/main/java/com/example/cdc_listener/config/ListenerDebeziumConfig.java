package com.example.cdc_listener.config;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListenerDebeziumConfig {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void startDebeziumEngine() {
        Properties props = new Properties();
        props.setProperty("name", "listener-embedded-engine");
        props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.MemoryOffsetBackingStore");
        props.setProperty("offset.flush.interval.ms", "60000");

        // Listener DB connection
        props.setProperty("database.hostname", "postgres-listener");
        props.setProperty("database.port", "5432");
        props.setProperty("database.user", "user");
        props.setProperty("database.password", "password");
        props.setProperty("database.dbname", "listenerdb");

        props.setProperty("slot.name", "debezium_listener");     // ← match the slot name
        props.setProperty("slot.drop_on_stop", "false");         // ← survive restarts
        props.setProperty("publication.name", "dbz_publication");
        props.setProperty("publication.autocreate.publication", "true");
        props.setProperty("plugin.name", "pgoutput");
        props.setProperty("publication.autocreate.mode", "filtered");

        // Debezium naming
        props.setProperty("database.server.name", "listener_server");
        props.setProperty("table.include.list", "public.order_replica");
        props.setProperty("topic.prefix", "listener");

        // Clean JSON output
        props.setProperty("transforms.unwrap.add.fields", "cdc_source=listener_replica");
        props.setProperty("transforms", "unwrap");
        props.setProperty("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
        props.setProperty("transforms.unwrap.drop.tombstones", "false");
        props.setProperty("transforms.unwrap.delete.handling.mode", "rewrite");
        props.setProperty("transforms.unwrap.add.fields", "cdc_source=listener_replica");

        props.setProperty("snapshot.mode", "initial");
        props.setProperty("tombstones.on.delete", "false");

        DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
                .using(props)
                .notifying((records, committer) -> {   // ← Correct signature
                    for (ChangeEvent<String, String> event : records) {
                        String topic = event.destination();   // e.g. listener_server.public.order_replica
                        String key = event.key();
                        String value = event.value();

                        // Async send to Kafka
                        kafkaTemplate.send(topic, key, value)
                                .whenComplete((result, ex) -> {
                                    if (ex != null) {
                                        log.error("Failed to send to Kafka topic '{}': {}", topic, ex.getMessage());
                                    } else {
                                        log.debug("Sent to {} offset {}", topic, result.getRecordMetadata().offset());
                                        try {
                                            committer.markProcessed(event);
                                        } catch (InterruptedException ie) {
                                            Thread.currentThread().interrupt();
                                        }
                                    }
                                });
                    }

                    committer.markBatchFinished();
                })
                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor(
                r -> {
                    Thread t = new Thread(r, "listener-debezium-engine");
                    t.setDaemon(true);
                    return t;
                });
        executor.submit(engine);

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                engine.close();
            } catch (IOException e) {
                log.error("Error closing Debezium engine", e);
            }
        }));

        log.info("Debezium 3.x engine started — streaming changes from order_replica");
    }
}
