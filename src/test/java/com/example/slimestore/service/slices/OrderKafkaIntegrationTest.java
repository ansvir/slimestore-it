package com.example.slimestore.service.slices;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.OutboxMessage;
import com.example.slimestore.repository.OrderRepository;
import com.example.slimestore.repository.OutboxMessageRepository;
import com.example.slimestore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_CREATED;
import static com.example.slimestore.jpa.Order.OrderStatus.ORDER_DELETED;
import static com.example.slimestore.util.OrderUtil.buildOrderStatusMessage;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ OrderService.class })
@Transactional
class OrderKafkaIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Test
    @Description("given order when created then created status message saved to outbox table")
    void whenCreateOrder_thenMessageSavedToOutbox() {
        // GIVEN
        Order newOrder = new Order();
        newOrder.setCustomerName("Test Customer");

        // WHEN
        Order savedOrder = orderService.createOrder(newOrder);

        // THEN
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getId()).isNotNull();

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
}