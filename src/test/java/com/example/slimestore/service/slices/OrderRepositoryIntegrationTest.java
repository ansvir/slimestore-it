package com.example.slimestore.service.slices;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.Product;
import com.example.slimestore.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Description;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @Description("given order with five items when create new order then order saved to db")
    void testCreateOrderWithFiveItems() {
        // GIVEN
        Order order = new Order();
        order.setCustomerName("Ivan Ivanov");

        List<Product> items = Arrays.asList(
                createProduct("Galaxy Slime", 1),
                createProduct("Glitter Slime", 2),
                createProduct("Cloud Slime", 1),
                createProduct("Fluffy Slime", 3),
                createProduct("Butter Slime", 1)
        );
        order.setProducts(items);

        // WHEN
        Order savedOrder = orderRepository.save(order);

        // THEN
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getProducts().size()).isEqualTo(items.size());
        assertThat(savedOrder.getProducts().getFirst().getName()).isEqualTo("Galaxy Slime");
    }

    @Test
    @Description("given an order when delete it then order is not in db")
    void testDeleteOrder() {
        // GIVEN
        Order orderToDelete = new Order();
        orderToDelete.setCustomerName("Cancelled Customer");
        orderToDelete.setProducts(Collections.emptyList());
        Order savedOrder = orderRepository.save(orderToDelete);
        Long orderId = savedOrder.getId();

        // WHEN
        orderRepository.deleteById(orderId);

        // THEN
        Optional<Order> deletedOrder = orderRepository.findById(orderId);
        assertThat(deletedOrder).isNotPresent();
    }

    @Test
    @Description("given a specific product when finding orders then only specific orders are returned")
    void testFindOrderByFilter() {
        // GIVEN
        orderRepository.save(createTestOrder("Alice", "Galaxy Slime", 1));
        orderRepository.save(createTestOrder("Bob", "Cloud Slime", 2));
        orderRepository.save(createTestOrder("Charlie", "Fluffy Slime", 3));
        orderRepository.save(createTestOrder("Dave", "Cloud Slime", 1));

        // WHEN
        List<Order> foundOrders = orderRepository.findByProducts_Name("Cloud Slime");

        // THEN
        assertThat(foundOrders).isNotNull();
        assertThat(foundOrders.size()).isEqualTo(2);
    }

    private Product createProduct(String itemName, int quantity) {
        Product product = new Product();
        product.setName(itemName);
        product.setQuantity(quantity);
        return product;
    }

    private Order createTestOrder(String customerName, String itemName, int quantity) {
        Order order = new Order();
        order.setCustomerName(customerName);
        Product product = new Product();
        product.setName(itemName);
        product.setQuantity(quantity);
        order.setProducts(Collections.singletonList(product));
        return order;
    }
}
