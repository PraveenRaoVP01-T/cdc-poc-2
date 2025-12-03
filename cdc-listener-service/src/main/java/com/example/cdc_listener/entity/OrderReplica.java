package com.example.cdc_listener.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@DynamicInsert
public class OrderReplica {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;
    private String name;
    private Double amount;
    private Instant createdAt;
    private Instant updatedAt;

    private boolean isUploaded = false;

    public String getPayload() {
        return """
                { "orderId": "%s", "name": "%s", "amount": "%s", "createdAt": "%s", "updatedAt": "%s"
                """.formatted(id.toString(), name, amount.toString(), createdAt.toString(), updatedAt.toString());
    }
}
