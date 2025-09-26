package com.example.slimestore.service;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.OutboxMessage;
import com.example.slimestore.jpa.OrderProduct;
import com.example.slimestore.jpa.Product;
import com.example.slimestore.repository.OrderRepository;
import com.example.slimestore.repository.OutboxMessageRepository;
import com.example.slimestore.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_CREATED;
import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_DELETED;
import static com.example.slimestore.util.OrderUtil.buildOrderStatusMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Import(OrderService.class)
@Transactional
class OrderServiceKafkaIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OutboxMessageRepository outboxMessageRepository;
    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setupCatalogProducts() {
        productRepository.save(createCatalogProduct("Test Slime"));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    @Test
    @Description("given order when created then created status message saved to outbox table")
    void whenCreateOrder_thenMessageSavedToOutbox() {
        // GIVEN
        Order newOrder = new Order();
        newOrder.setCustomerName("Test Customer");
        newOrder.setOrderProducts(Collections.singletonList(createOrderProduct(newOrder, "Test Slime", 1)));

        // WHEN
        Order savedOrder = orderService.createOrder(newOrder);

        // THEN
        assertNotNull(savedOrder);
        assertNotNull(savedOrder.getId());

        assertThat(orderRepository.findById(savedOrder.getId())).isPresent();
        assertThat(outboxMessageRepository.findAll()).hasSize(1);
        Optional<OutboxMessage> outboxMessage = outboxMessageRepository.findAll().stream().findFirst();
        assertThat(outboxMessage).isPresent();
        assertThat(outboxMessage.get().getPayload())
                .contains(buildOrderStatusMessage(ORDER_CREATED, savedOrder.getId()));
    }

    @Test
    @Description("given order when deleted then deleted status message saved to outbox table")
    void whenDeleteOrder_thenMessageSavedToOutbox() {
        // GIVEN
        Order orderToDelete = new Order();
        orderToDelete.setCustomerName("Customer for deletion");
        orderToDelete.setOrderProducts(Collections.singletonList(createOrderProduct(orderToDelete, "Test Slime", 1)));
        Order savedOrder = orderRepository.save(orderToDelete);
        Long orderId = savedOrder.getId();

        // WHEN
        orderService.deleteOrder(orderId);

        // THEN
        assertThat(orderRepository.findById(orderId)).isNotPresent();
        assertThat(outboxMessageRepository.findAll()).hasSize(1);
        Optional<OutboxMessage> outboxMessage = outboxMessageRepository.findAll().stream()
                .findFirst();
        assertThat(outboxMessage).isPresent();
        assertThat(outboxMessage.get().getPayload()).contains(buildOrderStatusMessage(ORDER_DELETED, orderId));
    }

    private Product createCatalogProduct(String name) {
        Product product = new Product();
        product.setName(name);
        return product;
    }

    private OrderProduct createOrderProduct(Order order, String productName, int quantity) {
        OrderProduct item = new OrderProduct();
        Product product = productRepository.findByName(productName)
                .orElseThrow();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }
}