package com.example.cdc_producer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class OutboxEvent {
    @Id
    @GeneratedValue
    public Long id;
    public UUID aggregateId;
    public String eventType;
    public String payload;       // JSON string
    public boolean processed = false;
    public LocalDateTime createdAt = LocalDateTime.now();

    public OutboxEvent(UUID id, String eventType, String payload) {
        this.aggregateId = id;
        this.eventType = eventType;
        this.payload = payload;
    }
}
