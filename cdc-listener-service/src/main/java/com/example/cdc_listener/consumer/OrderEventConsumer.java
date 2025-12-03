package com.example.cdc_listener.consumer;

import com.example.cdc_listener.entity.OrderReplica;
import com.example.cdc_listener.repository.OrderReplicaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.layout.LogstashLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class OrderEventConsumer {

    @Autowired
    private OrderReplicaRepository replicaRepo;

    @KafkaListener(topics = "order-events", concurrency = "3")
    @Transactional
    public void consume(@Payload String payload,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        Acknowledgment ack) throws JsonProcessingException {
        try {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(payload);

            UUID orderId = UUID.fromString(json.get("orderId").asText());

            if (replicaRepo.existsById(orderId)) {
                ack.acknowledge();
                return;
            }

            OrderReplica replica = new OrderReplica();
            replica.setId(orderId);
            replica.setName(json.get("name").asText());
            replica.setAmount(Double.parseDouble(json.get("amount").asText()));
            Instant createdAt = Instant.parse(json.get("createdAt").asText());
            replica.setCreatedAt(createdAt);
            Instant updatedAt = Instant.parse(json.get("updatedAt").asText());
            replica.setUpdatedAt(updatedAt);
            replica.setUploaded(false);

            replicaRepo.upsert(replica.getId(),replica.getName(),replica.getAmount(), replica.getCreatedAt(),replica.getUpdatedAt());
            ack.acknowledge();

//            log.info("order.replicated id={} name={} amount={}",
//                    orderId, replica.getName(), replica.getAmount());

        } catch (JsonProcessingException e) {
            log.error("Error Processing JSON Content: {} - {}", e, payload);
        } catch (Exception e) {
            log.error("Error occured somewhere else: {}", e.getMessage());
            log.info("Payload provided: {}", payload);
        }
    }
}
