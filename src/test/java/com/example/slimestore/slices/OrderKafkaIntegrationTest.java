package com.example.slimestore.slices;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.repository.OrderRepository;
import com.example.slimestore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { OrderService.class, KafkaTemplate.class })
public class OrderKafkaIntegrationTest {

    private static final String ORDERS_TOPIC = "orders";
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:6.2.1";
    private static final String ORDER_CREATED = "ORDER_CREATED";
    private static final String ORDER_DELETED = "ORDER_DELETED";

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @MockitoBean
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void whenCreateOrder_thenKafkaMessageIsSent() {
        // GIVEN
        Long orderId = 1L;
        Order order = new Order();
        order.setId(orderId);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // WHEN
        orderService.createOrder(new Order());

        // THEN
        verify(kafkaTemplate).send(ORDERS_TOPIC, buildOrderStatusMessage(ORDER_CREATED, orderId));
    }

    @Test
    void whenDeleteOrder_thenKafkaMessageIsSent() {
        // GIVEN
        Long orderId = 1L;
        doNothing().when(orderRepository).deleteById(orderId);

        // WHEN
        orderService.deleteOrder(orderId);

        // THEN
        verify(kafkaTemplate).send(ORDERS_TOPIC, buildOrderStatusMessage(ORDER_DELETED, orderId));
    }

    private String buildOrderStatusMessage(String status, Long orderId) {
        return String.format("%s:%s", status, orderId);
    }
}