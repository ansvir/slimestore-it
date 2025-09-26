package com.example.slimestore.repository;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.OrderProduct;
import com.example.slimestore.jpa.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// test order repository with h2 embedded db
@DataJpaTest
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setupCatalogProducts() {
        productRepository.save(createCatalogProduct("Galaxy Slime"));
        productRepository.save(createCatalogProduct("Glitter Slime"));
        productRepository.save(createCatalogProduct("Cloud Slime"));
        productRepository.save(createCatalogProduct("Fluffy Slime"));
        productRepository.save(createCatalogProduct("Butter Slime"));
    }

    @Test
    void testCreateOrderWithFiveItems() {
        // GIVEN
        Order order = new Order();
        order.setCustomerName("Ivan Ivanov");
        order.setOrderProducts(Arrays.asList(
                createOrderProduct("Galaxy Slime", 1),
                createOrderProduct("Glitter Slime", 2),
                createOrderProduct("Cloud Slime", 1),
                createOrderProduct("Fluffy Slime", 3),
                createOrderProduct("Butter Slime", 1)
        ));

        // WHEN
        Order savedOrder = orderRepository.save(order);

        // THEN
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getOrderProducts().size()).isEqualTo(5);
        assertThat(savedOrder.getOrderProducts().getFirst().getProduct().getName()).isEqualTo("Galaxy Slime");
    }

    @Test
    void testFindOrderByFilter() {
        // GIVEN
        Order order1 = createOrderWithItems("Alice", "Galaxy Slime", 1);
        Order order2 = createOrderWithItems("Bob", "Cloud Slime", 2);
        Order order3 = createOrderWithItems("Charlie", "Fluffy Slime", 3);
        Order order4 = createOrderWithItems("Dave", "Cloud Slime", 1);
        orderRepository.saveAll(List.of(order1, order2, order3, order4));

        // WHEN
        List<Order> foundOrders = orderRepository.findByOrderProducts_Product_Name("Cloud Slime");

        // THEN
        assertThat(foundOrders).isNotNull();
        assertThat(foundOrders.size()).isEqualTo(2);
    }

    private Product createCatalogProduct(String name) {
        Product product = new Product();
        product.setName(name);
        return product;
    }

    private OrderProduct createOrderProduct(String productName, int quantity) {
        OrderProduct item = new OrderProduct();
        Product product = productRepository.findByName(productName)
                .orElseThrow();
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    private Order createOrderWithItems(String customerName, String itemName, int quantity) {
        Order order = new Order();
        order.setCustomerName(customerName);
        OrderProduct item = createOrderProduct(itemName, quantity);
        item.setOrder(order);
        order.setOrderProducts(Collections.singletonList(item));
        return order;
    }
}