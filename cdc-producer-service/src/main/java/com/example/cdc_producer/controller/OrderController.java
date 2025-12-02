package com.example.cdc_producer.controller;

import com.example.cdc_producer.entity.Order;
import com.example.cdc_producer.entity.OutboxEvent;
import com.example.cdc_producer.repository.OrderRepository;
import com.example.cdc_producer.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/orders")                 // plural is REST convention
@RequiredArgsConstructor                    // better than @Autowired
@Slf4j                                      // instead of System.out
public class OrderController {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // thread-safe

    @PostMapping
    @Transactional                              // this is the key – both inserts in same TX
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {

        Order order = Order.builder()
                .name(request.name())
                .amount(request.amount())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        order = orderRepository.save(order);
        log.info("Order saved with id {}", order.getId());

        // Build clean JSON payload (you can also create a separate Event class)
        String payload = order.getPayload();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(order.getId())
                .eventType("OrderCreated")
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(outboxEvent);
        log.info("Outbox event saved for order {}", order.getId());

        return ResponseEntity.ok(order);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException(e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // DTOs – always use them on the boundary
    // ──────────────────────────────────────────────────────────────
    public record CreateOrderRequest(String name, Double amount) {}

    public record OrderCreatedEvent(
            Long orderId,
            String customerName,
            Double amount,
            Instant createdAt
    ) {}
}