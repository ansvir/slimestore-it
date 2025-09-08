package com.example.slimestore.full;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.Product;
import com.example.slimestore.repository.OrderRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;

import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_CREATED;
import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class OrderIntegrationTest {

    private static final String ORDERS_TOPIC = "orders";
    private static final String KAFKA_SERVERS = "spring.kafka.bootstrap-servers";

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add(KAFKA_SERVERS, kafka::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

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
    }

    @AfterEach
    void tearDownKafkaConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @Description("given order with five products when create new order then order saved to db and kafka message sent")
    void testCreateOrderWithFiveItems() {
        // GIVEN
        Order order = new Order();
        order.setCustomerName("Ivan Ivanov");
        List<Product> items = Arrays.asList(
                createPosition("Galaxy Slime", 1),
                createPosition("Glitter Slime", 2),
                createPosition("Cloud Slime", 1),
                createPosition("Fluffy Slime", 3),
                createPosition("Butter Slime", 1)
        );
        order.setProducts(items);

        // WHEN
        ResponseEntity<Order> response = restTemplate.postForEntity("/api/orders", order, Order.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertNotNull(response.getBody());
        Order createdOrder = response.getBody();
        assertNotNull(createdOrder.getId());

        Optional<Order> dbOrder = orderRepository.findById(createdOrder.getId());
        assertThat(dbOrder).isPresent();
        assertThat(dbOrder.get().getProducts()).hasSize(items.size());

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isEqualTo(1);
        String message = records.iterator().next().value();
        assertThat(message).contains(ORDER_CREATED.name());
        assertThat(message).contains(createdOrder.getId().toString());
    }

    @Test
    @Sql(value = "classpath:/sql/order/create_inactive_order.sql", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    @Description("given inactive order when delete order then no order in db and kafka message sent")
    void testDeleteInactiveOrder() {
        // GIVEN
        long orderId = 999L;

        // WHEN
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/orders/" + orderId, HttpMethod.DELETE, null, Void.class
        );

        // THEN (Verify HTTP, DB, and Kafka)
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Order> getResponse = restTemplate.getForEntity(
                "/api/orders/" + orderId, Order.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isEqualTo(1);
        String message = records.iterator().next().value();
        assertThat(message).contains(ORDER_DELETED.name());
        assertThat(message).contains(Long.toString(orderId));
    }

    @Test
    @Sql(value = "classpath:/sql/order/populate_orders_for_filter_test.sql", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    @Description("given order filter when apply filter then specific orders found")
    void testFindOrderByFilter() {
        // GIVEN && WHEN
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/orders/search?itemName=Cloud Slime", List.class
        );

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody().size()).isEqualTo(2);
    }

    private Product createPosition(String itemName, int quantity) {
        Product product = new Product();
        product.setName(itemName);
        product.setQuantity(quantity);
        return product;
    }

}