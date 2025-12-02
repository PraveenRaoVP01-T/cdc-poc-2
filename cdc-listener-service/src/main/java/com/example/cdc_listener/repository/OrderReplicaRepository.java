package com.example.cdc_listener.repository;

import com.example.cdc_listener.entity.OrderReplica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface OrderReplicaRepository extends JpaRepository<OrderReplica, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO order_replica (id, name, amount, created_at, updated_at)
        VALUES (:id, :name, :amount, :createdAt, :updatedAt)
        ON CONFLICT (id) DO NOTHING
        """, nativeQuery = true)
    void upsert(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("amount") Double amount,
            @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt
    );
}
