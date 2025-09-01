package com.example.slimestore.service;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.example.slimestore.util.OrderUtil.buildOrderStatusMessage;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String ORDER_TOPIC = "orders";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        kafkaTemplate.send(ORDER_TOPIC, buildOrderStatusMessage(Order.OrderStatus.ORDER_CREATED, savedOrder.getId()));
        return savedOrder;
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
        kafkaTemplate.send(ORDER_TOPIC, buildOrderStatusMessage(Order.OrderStatus.ORDER_DELETED, id));
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> findByProductName(String productName) {
        return orderRepository.findByProducts_Name(productName);
    }

}