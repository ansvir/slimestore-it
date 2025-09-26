package com.example.slimestore.scheduler;

import com.example.slimestore.jpa.OutboxMessage;
import com.example.slimestore.repository.OutboxMessageRepository;
import com.example.slimestore.util.OrderUtil;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DataJpaTest
@Import(OutboxRelayerScheduler.class)
class OutboxRelayerSchedulerNegativeIntegrationTest {

    private static final String ORDERS_TOPIC = "orders";

    @MockitoBean
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private OutboxRelayerScheduler outboxRelayerScheduler;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void whenKafkaSendFails_thenMessageIsNotDeletedFromOutbox() {
        // GIVEN
        long id = 1L;
        String payload = OrderUtil.buildOrderStatusMessage(ORDER_CREATED, id);
        OutboxMessage message = createOutboxMessage(id, payload);
        when(outboxMessageRepository.findAll()).thenReturn(List.of(message));
        doThrow(new RuntimeException("Kafka Broker is down")).when(kafkaTemplate).send(any(), any());
        doNothing().when(outboxMessageRepository).delete(any());

        // WHEN
        outboxRelayerScheduler.processOutboxMessages();

        // THEN
        verify(kafkaTemplate, times(1)).send(message.getTopic(), message.getPayload());
        verify(outboxMessageRepository, never()).delete(any());
        when(outboxMessageRepository.findById(id)).thenReturn(Optional.of(message));
        assertThat(outboxMessageRepository.findById(id)).isPresent();
    }

    private OutboxMessage createOutboxMessage(Long id, String payload) {
        OutboxMessage message = new OutboxMessage();
        message.setId(id);
        message.setTopic(ORDERS_TOPIC);
        message.setPayload(payload);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}