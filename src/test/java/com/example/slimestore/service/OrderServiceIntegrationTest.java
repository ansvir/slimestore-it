package com.example.slimestore.service;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.OrderProduct;
import com.example.slimestore.jpa.Product;
import com.example.slimestore.model.order.OrderDto;
import com.example.slimestore.model.orderproduct.OrderProductDto;
import com.example.slimestore.model.product.ProductDto;
import com.example.slimestore.repository.OrderRepository;
import com.example.slimestore.repository.OutboxMessageRepository;
import com.example.slimestore.scheduler.OutboxRelayerScheduler;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Description;
import org.springframework.core.ParameterizedTypeReference;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class OrderServiceIntegrationTest {

    private static final String ORDERS_TOPIC = "orders";
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:6.2.1";
    private static final String POSTGRES_IMAGE = "postgres:13";

    @Value("${app.outbox.delay}")
    private int outboxDelay;

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OutboxRelayerScheduler outboxRelayerScheduler;
    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    private Consumer<String, String> consumer;

    @BeforeEach
    void init() {
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

        outboxMessageRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @Description("given order with five products when create new order then order saved to db and kafka message sent")
    void testCreateOrderWithFiveItems() {
        // GIVEN
        OrderDto orderDto = new OrderDto();
        orderDto.setCustomerName("Ivan Ivanov");
        orderDto.setOrderProducts(createOrderProducts(
                "Galaxy Slime", 1, "Glitter Slime", 2, "Cloud Slime", 1, "Fluffy Slime", 3, "Butter Slime", 1));

        // WHEN
        ResponseEntity<OrderDto> response = restTemplate.postForEntity("/api/orders", orderDto, OrderDto.class);
        outboxRelayerScheduler.processOutboxMessages();

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertNotNull(response.getBody());
        OrderDto createdOrder = response.getBody();
        assertNotNull(createdOrder.getId());

        Optional<Order> dbOrder = orderRepository.findById(createdOrder.getId());
        assertThat(dbOrder).isPresent();
        assertThat(dbOrder.get().getOrderProducts()).hasSize(5);

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(outboxDelay));
        assertThat(records.count()).isEqualTo(1);
        String message = records.iterator().next().value();
        assertThat(message).contains(Order.OrderStatus.ORDER_CREATED.name());
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
        outboxRelayerScheduler.processOutboxMessages();

        // THEN
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Order> getResponse = restTemplate.getForEntity(
                "/api/orders/" + orderId, Order.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(outboxDelay));
        assertThat(records.count()).isEqualTo(1);
        String message = records.iterator().next().value();
        assertThat(message).contains(Order.OrderStatus.ORDER_DELETED.name());
        assertThat(message).contains(Long.toString(orderId));
    }

    @Test
    @Sql(value = "classpath:/sql/order/populate_orders_for_filter_test.sql", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    @Description("given order filter when apply filter then specific orders found")
    void testFindOrderByFilter() {
        // GIVEN && WHEN
        ResponseEntity<List<OrderDto>> response = restTemplate.exchange(
                "/api/orders/search?itemName=Cloud Slime",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody().size()).isEqualTo(2);
    }

    private List<OrderProductDto> createOrderProducts(Object... args) {
        List<OrderProductDto> items = new ArrayList<>();
        for (int i = 0; i < args.length; i += 2) {
            String productName = (String) args[i];
            int quantity = (Integer) args[i + 1];

            ProductDto product = new ProductDto();
            product.setName(productName);
            OrderProductDto orderProductDto = new OrderProductDto();
            orderProductDto.setProduct(product);
            orderProductDto.setQuantity(quantity);
            items.add(orderProductDto);
        }
        return items;
    }
}