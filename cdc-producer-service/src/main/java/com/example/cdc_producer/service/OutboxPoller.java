package com.example.cdc_producer.service;

import com.example.cdc_producer.entity.OutboxEvent;
import com.example.cdc_producer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:2000}")
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxRepo.findTop100ByProcessedFalseOrderByIdAsc();

        for (OutboxEvent e : events) {
            try {
                kafka.send("order-events", e.getAggregateId().toString(), e.getPayload())
                        .thenRun(() -> {
                            e.setProcessed(true);
                            outboxRepo.save(e);
                            log.info("Published & marked processed: {}", e.getId());
                        });
            } catch (Exception ex) {
                log.error("Failed to publish event {}", e.getId(), ex);
                // retry logic or DLQ can be added
            }
        }
    }
}
