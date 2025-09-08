package com.example.slimestore.service;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.OutboxMessage;
import com.example.slimestore.repository.OrderRepository;
import com.example.slimestore.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.slimestore.util.OrderUtil.buildOrderStatusMessage;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String ORDER_TOPIC = "orders";

    private final OrderRepository orderRepository;
    private final OutboxMessageRepository outboxMessageRepository;

    @Transactional
    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);

        OutboxMessage outboxMessage = new OutboxMessage();
        outboxMessage.setTopic(ORDER_TOPIC);
        outboxMessage.setPayload(buildOrderStatusMessage(Order.OrderStatus.ORDER_CREATED, savedOrder.getId()));
        outboxMessage.setCreatedAt(LocalDateTime.now());
        outboxMessageRepository.save(outboxMessage);

        return savedOrder;
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);

        OutboxMessage outboxMessage = new OutboxMessage();
        outboxMessage.setTopic(ORDER_TOPIC);
        outboxMessage.setPayload(buildOrderStatusMessage(Order.OrderStatus.ORDER_DELETED, id));
        outboxMessage.setCreatedAt(LocalDateTime.now());
        outboxMessageRepository.save(outboxMessage);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> findByProductName(String productName) {
        return orderRepository.findByProducts_Name(productName);
    }
}