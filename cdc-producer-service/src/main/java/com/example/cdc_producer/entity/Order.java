package com.example.cdc_producer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private Double amount;
    private Instant createdAt;
    private Instant updatedAt;

    public String getPayload() {
        return """
                { "orderId": "%s", "name": "%s", "amount": "%s", "createdAt": "%s", "updatedAt": "%s" }""".formatted(id.toString(), name, amount.toString(), createdAt.toString(), updatedAt.toString());
    }
}
