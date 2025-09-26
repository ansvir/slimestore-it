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

/**
 * Service for managing slime orders, implementing the Transactional Outbox Pattern.
 * This service ensures that an order and its corresponding messaging event are
 * saved atomically within a single database transaction.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String ORDER_TOPIC = "orders";

    private final OrderRepository orderRepository;
    private final OutboxMessageRepository outboxMessageRepository;

    /**
     * Creates a new order and saves a corresponding message to the outbox table.
     * This operation is atomic: if the order fails to save, the outbox message
     * will also be rolled back.
     *
     * @param order The order entity to be saved.
     * @return The saved order entity.
     */
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

    /**
     * Deletes an order by its ID and saves a corresponding message to the outbox table.
     * This operation is atomic: both the deletion and the outbox message are part
     * of the same database transaction.
     *
     * @param id The ID of the order to be deleted.
     */
    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);

        OutboxMessage outboxMessage = new OutboxMessage();
        outboxMessage.setTopic(ORDER_TOPIC);
        outboxMessage.setPayload(buildOrderStatusMessage(Order.OrderStatus.ORDER_DELETED, id));
        outboxMessage.setCreatedAt(LocalDateTime.now());
        outboxMessageRepository.save(outboxMessage);
    }

    /**
     * Retrieves an order by its unique ID.
     *
     * @param id The ID of the order to find.
     * @return An Optional containing the found order, or empty if not found.
     */
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    /**
     * Finds orders that contain a product with the given name.
     *
     * @param productName The name of the product to search for.
     * @return A list of orders matching the search criteria.
     */
    public List<Order> findByProductName(String productName) {
        return orderRepository.findByOrderProducts_Product_Name(productName);
    }
}