package com.example.slimestore.scheduler;

import com.example.slimestore.jpa.OutboxMessage;
import com.example.slimestore.repository.OutboxMessageRepository;
import com.example.slimestore.util.OrderUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_CREATED;
import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest
class OutboxRelayerSchedulerIntegrationTest {

    private static final String ORDERS_TOPIC = "orders";
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:6.2.1";
    private static final String KAFKA_SERVERS = "spring.kafka.bootstrap-servers";

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add(KAFKA_SERVERS, kafka::getBootstrapServers);
    }

    @MockitoBean
    private OutboxMessageRepository outboxMessageRepository;

    @MockitoSpyBean
    private OutboxRelayerScheduler outboxRelayerScheduler;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setupKafkaConsumer() {
        var groupName = "test-group";
        var resetPolicy = "earliest";
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupName);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, resetPolicy);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        consumer.subscribe(Collections.singleton(ORDERS_TOPIC));
        consumer.poll(Duration.ofMillis(100)); // healthcheck for consumer
    }

    @AfterEach
    void tearDownKafkaConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void whenMessagesInOutbox_thenRelayerSendsToKafkaAndDeletes() {
        // GIVEN
        var id1 = 1L;
        var id2 = 2L;
        var payload1 = OrderUtil.buildOrderStatusMessage(ORDER_CREATED, id1);
        var payload2 = OrderUtil.buildOrderStatusMessage(ORDER_DELETED, id2);
        OutboxMessage message1 = createOutboxMessage(id1, payload1);
        OutboxMessage message2 = createOutboxMessage(id2, payload2);
        List<OutboxMessage> messages = List.of(message1, message2);

        when(outboxMessageRepository.findAll()).thenReturn(messages);

        // WHEN
        outboxRelayerScheduler.processOutboxMessages();

        // THEN
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(5000));
        assertThat(records.count()).isEqualTo(messages.size());

        var receivedMessages = new ArrayList<String>();
        records.records(ORDERS_TOPIC).forEach(message -> receivedMessages.add(message.value()));

        assertThat(receivedMessages).containsExactlyInAnyOrder(payload1, payload2);

        verify(outboxMessageRepository, times(1)).findAll();
        verify(outboxMessageRepository, times(1)).delete(message1);
        verify(outboxMessageRepository, times(1)).delete(message2);

        verifyNoMoreInteractions(outboxMessageRepository);
    }

    @Test
    @Disabled("Test is under development")
    void givenSomething_WhenDoSomething_ThenResult() {
        // this test is for showcase of disabled
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