package com.example.cdc_listener.service;

import com.example.cdc_listener.entity.OrderReplica;
import com.example.cdc_listener.repository.OrderReplicaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReplicaBatchUploader {

    private final OrderReplicaRepository repo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();
    private static final String LOGSTASH_URL = "http://localhost:5044";

    @Scheduled(fixedDelay = 2_000) // every 1 minute
    public void uploadRecentChanges() {
        List<OrderReplica> changes = repo.findByUploadedFalseOrderByUpdatedAt();

        if (changes.isEmpty()) return;

        changes.forEach(order -> {
            Map<String, Object> event = Map.of(
                    "id", order.getId(),
                    "name", order.getName(),
                    "amount", order.getAmount(),
                    "updated_at", order.getUpdatedAt(),
                    "cdc_source", "batch_upload",
                    "@timestamp", Instant.now().toString()
            );

            try {
                String json = objectMapper.writeValueAsString(event) + "\n";
//                restTemplate.postForObject(LOGSTASH_URL, json, String.class);

                log.info("Sent to Logstash: {}", json.trim());
                order.setUploaded(true);
            } catch (Exception e) {
                log.error("Failed to send order {}: {}", order.getId(), e.getMessage());
                // will retry next run
            }
        });

        repo.saveAll(changes);
        log.info("Batch uploaded {} orders to Logstash/Elasticsearch", changes.size());
    }
}
