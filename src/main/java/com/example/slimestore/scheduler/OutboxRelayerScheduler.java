package com.example.slimestore.scheduler;

import com.example.slimestore.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayerScheduler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxMessageRepository outboxMessageRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedRate = 5000)
    public void processOutboxMessages() {
        outboxMessageRepository.findAll()
                .stream()
                .<TransactionCallback<Boolean>>map(message -> status -> {
                    try {
                        kafkaTemplate.send(message.getTopic(), message.getPayload());
                        outboxMessageRepository.delete(message);
                        return true;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        var errorMessage = "Cannot process outbox messages due to: ";
                        log.error("{}{}", errorMessage, e.getMessage());
                        return false;
                    }
                }).forEach(transactionTemplate::execute);
    }
}